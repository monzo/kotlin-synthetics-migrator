package com.monzo.commonui

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

// This function intentionally returns a platform type. This is not ideal, but we're replacing kotlinx synthetic
// view imports which already use platform types, and there are too many call-sites in our app that rely on these
// fields _sometimes_ being nullable (e.g. where we conditionally inflate layouts in some Fragments/Activities).
@Suppress("HasPlatformType")
fun <T : View> View.findById(@IdRes id: Int) = ViewCacheUtil.findCachedView<T>(this, id)

// This function intentionally returns a platform type. This is not ideal, but we're replacing kotlinx synthetic
// view imports which already use platform types, and there are too many call-sites in our app that rely on these
// fields _sometimes_ being nullable (e.g. where we conditionally inflate layouts in some Fragments/Activities).
@Suppress("HasPlatformType")
fun <T : View> Fragment.findById(@IdRes id: Int) = ViewCacheUtil.findCachedView<T>(view, id)

// This function intentionally returns a platform type. This is not ideal, but we're replacing kotlinx synthetic
// view imports which already use platform types, and there are too many call-sites in our app that rely on these
// fields _sometimes_ being nullable (e.g. where we conditionally inflate layouts in some Fragments/Activities).
@Suppress("HasPlatformType")
fun <T : View> Activity.findById(@IdRes id: Int): T {
    // We can't use the actual decorView as the cache container, since doesn't get recreated in some configuration
    // changes (like split screen). This would cause the cache to survive these changes, which is something we want to
    // avoid. Instead, the 'content' view is safe to use, since it does indeed get recreated in these cases.
    return ViewCacheUtil.findCachedView(findViewById(android.R.id.content), id)
}

// This function intentionally returns a platform type. This is not ideal, but we're replacing kotlinx synthetic
// view imports which already use platform types, and there are too many call-sites in our app that rely on these
// fields _sometimes_ being nullable (e.g. where we conditionally inflate layouts in some Fragments/Activities).
@Suppress("HasPlatformType")
fun <T : View> RecyclerView.ViewHolder.findById(@IdRes id: Int) = ViewCacheUtil.findCachedView<T>(itemView, id)
