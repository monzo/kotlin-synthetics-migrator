package com.monzo.syntheticsmigrator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.intentions.receiverType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

private const val FindByIdFqn = "com.monzo.commonui.findById"

class KotlinSyntheticsMigrator : AnAction("Run Kotlin Synthetics Migrator...") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PlatformDataKeys.PROJECT)!!

        val psiManager = PsiManager.getInstance(project)

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex

        val filesWithSyntheticReferences = mutableListOf<FileWithSyntheticReferences>()

        // Collect all KtFiles in the app that have synthetic view imports.
        ReadAction.compute<Unit, Nothing> {
            fileIndex.iterateContent { virtualFile ->
                // Ignore library files.
                if (!fileIndex.isInSource(virtualFile)) return@iterateContent true

                // Ignore idea templates.
                if (virtualFile.path.contains(".idea")) return@iterateContent true

                // Only care about Kotlin files with synthetic imports.
                if (virtualFile.name.endsWith(".kt")) {
                    val psiFile = psiManager.findFile(virtualFile) as KtFile
                    psiFile.toFileWithSyntheticReferencesOrNull()?.let {
                        filesWithSyntheticReferences += it
                    }
                }

                return@iterateContent true
            }
        }

        val filesToModify = filesWithSyntheticReferences.map { it.file }
        val filesToModifyArray = PsiUtilCore.toPsiFileArray(filesToModify)

        // Run refactoring in a single write command so that we can undo it if anything goes wrong.
        WriteCommandAction.runWriteCommandAction(project, null, null, {
            for (file in filesWithSyntheticReferences) {
                file.process(project)
            }

            println("Shortening fully qualified names...")
            ShortenReferences.DEFAULT.process(filesToModify)
        }, *filesToModifyArray)

        println("Refactor complete!")
    }
}

private fun KtFile.toFileWithSyntheticReferencesOrNull(): FileWithSyntheticReferences? {
    val syntheticImports = findSyntheticImports()
    if (syntheticImports.isEmpty()) return null

    println("Looking for synthetic view imports in $name")

    val referenceExpressions = PsiTreeUtil
        .findChildrenOfType(this, KtReferenceExpression::class.java)

    val syntheticReferences = mutableSetOf<SyntheticReference>()

    for (referenceExpression in referenceExpressions) {
        referenceExpression.toSyntheticReferenceOrNull()?.let {
            syntheticReferences += it
        }
    }

    return FileWithSyntheticReferences(
        file = this,
        syntheticImports = syntheticImports,
        syntheticReferences = syntheticReferences
    )
}

private fun KtReferenceExpression.toSyntheticReferenceOrNull(): SyntheticReference? {
    val property = resolveMainReferenceToDescriptors()
        .filterIsInstance<AndroidSyntheticProperty>()
        .firstOrNull() ?: return null

    val viewType = resolveViewTypeFqn() ?: return null

    var receiverFqn = (property as PropertyDescriptor).receiverType()!!.fqName!!.asString()
    if (receiverFqn == "kotlinx.android.extensions.LayoutContainer") {
        receiverFqn = "androidx.recyclerview.widget.RecyclerView.ViewHolder"
    }

    return SyntheticReference(
        viewTypeFqn = viewType,
        resourceId = property.resource.id.name,
        receiverFqn = receiverFqn
    )
}

private fun FileWithSyntheticReferences.process(project: Project) {
    println("Refactoring ${file.name}")

    val psiFactory = KtPsiFactory(project, markGenerated = false)

    val anchorElement = file.lastChild

    // Add all of the new properties to the file.
    for (reference in syntheticReferences.reversed()) {
        val resourceId = reference.resourceId
        val receiver = reference.receiverFqn
        val type = reference.viewTypeFqn
        val newElement = psiFactory.createProperty(
            "private val $receiver.$resourceId inline get() = findById<$type>(R.id.$resourceId)"
        )

        file.addAfter(newElement, anchorElement)
    }

    // Add a gap between the anchor and all of the new properties.
    file.addAfter(psiFactory.createNewLine(2), anchorElement)

    // Delete synthetic imports which should fix any conflict errors.
    for (syntheticImport in syntheticImports) {
        syntheticImport.delete()
    }

    // We know this is not null because we checked earlier to see if there was at least one
    // synthetic view import.
    val importList = file.importList!!

    // The typical approach for refactoring is to use fully qualified names and then use
    // ShortenReferences.DEFAULT.process to automatically add imports. However fully qualified
    // names do not work for extension functions, so we have to insert this import ourselves
    if (importList.imports.none { it.importPath?.pathStr == FindByIdFqn }) {
        importList.add(
            psiFactory.createImportDirective(
                ImportPath(
                    FqName(FindByIdFqn),
                    false,
                    null
                )
            )
        )
    }
}

private data class FileWithSyntheticReferences(
    val file: KtFile,
    val syntheticImports: List<KtImportDirective>,
    val syntheticReferences: Set<SyntheticReference>
)

private data class SyntheticReference(
    val viewTypeFqn: String,
    val resourceId: String,
    val receiverFqn: String
)

private fun KtReferenceExpression.resolveViewTypeFqn(): String? {
    return analyze(BodyResolveMode.PARTIAL).getType(this)?.fqName?.asString()
}

private fun KtFile.findSyntheticImports(): List<KtImportDirective> {
    val importList = importList ?: return emptyList()
    val importElements = PsiTreeUtil.findChildrenOfType(importList, KtImportDirective::class.java)
    return importElements.filter {
        it.importPath?.pathStr?.startsWith("kotlinx.android.synthetic") ?: false
    }
}
