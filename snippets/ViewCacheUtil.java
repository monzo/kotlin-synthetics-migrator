package com.monzo.commonui;

import android.util.SparseArray;
import android.view.View;

import androidx.annotation.IdRes;

// This class is intentionally written in Java to ensure the return type is a platform type. This is not
// ideal, but we're replacing kotlin synthetic view imports which already use platform types, and there are
// too many call-sites in our app that rely on these fields _sometimes_ being nullable (e.g. where we
// conditionally inflate layouts in some Fragments/Activities).
class ViewCacheUtil {

    @SuppressWarnings("unchecked")
    public static <T> T findCachedView(View container, @IdRes int id) {
        if (container == null) {
            return null;
        }
        SparseArray<View> viewCache = (SparseArray<View>) container.getTag(R.id.tag_view_cache);
        if (viewCache == null) {
            viewCache = new SparseArray<>();
            container.setTag(R.id.tag_view_cache, viewCache);
        }
        View view = viewCache.get(id);
        if (view == null) {
            view = container.findViewById(id);
            viewCache.put(id, view);
        }
        return (T) view;
    }
}
