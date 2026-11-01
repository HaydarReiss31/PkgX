package com.haydareiss.pkgx;

import android.graphics.drawable.Drawable;

/** Listedeki tek bir uygulamanın özeti. */
final class AppEntry {
    final String label;
    final String packageName;
    final Drawable icon;
    final boolean system;
    final boolean enabled;

    AppEntry(String label, String packageName, Drawable icon, boolean system, boolean enabled) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.system = system;
        this.enabled = enabled;
    }
}
