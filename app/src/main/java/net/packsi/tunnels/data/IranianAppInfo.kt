package net.packsi.tunnels.data

import android.graphics.drawable.Drawable

data class IranianAppInfo(
    val packageName: String,
    val appName: String,   // Persian display label (catalog nameFa, else device label)
    val nameEn: String,    // English subtitle (catalog nameEn)
    val icon: Drawable,
    val isFree: Boolean,
)
