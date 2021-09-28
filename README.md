## kotlin-synthetics-migrator
An Intellij Plugin for migrating an Android app from Kotlin Synthetics to a custom `findById` function. 

#### Prerequsites
1. Add the necessary `findById` functions to your app. Here's [ours](https://github.com/monzo/kotlin-synthetics-migrator/blob/main/snippets/ViewFinders.kt).
2. Update the [hardcoded package](https://github.com/monzo/kotlin-synthetics-migrator/blob/92afc6473d59ba04f2ac5277ef6338892b9a982d/src/main/kotlin/com/monzo/syntheticsmigrator/KotlinSyntheticsMigrator.kt#L25) in the tool to point to your new `findById` functions.
3. Add a `local.properties` file with the following two properties:

```
# Note: /Contents at the end is MacOS specific. 
studio.path=/Users/bradley/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/213.5744.223.2113.8103819/Android Studio Preview.app/Contents
studio.version=213.5744.223
```

You can find your studio version in `About Android Studio`: 

<img width="504" alt="Screenshot 2022-02-22 at 17 34 47" src="https://user-images.githubusercontent.com/2288921/155194878-49e14604-44f0-4d70-856b-133342106cde.png">


4. (Optional) If you have a large project, you might also want to increase the available RAM. Add the following to that `tasks` dsl in `build.gradle.kts`:

```
runIde { maxHeapSize = "4g" }
```


#### Usage
`./gradlew runIde`

This will run Studio with the plugin installed. Open your project, then click `Run Kotlin Synthetics Migrator...` in the `Refactor` menu. 
