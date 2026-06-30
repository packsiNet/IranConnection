package net.packsi.tunnels

import android.app.Application
import com.google.android.gms.ads.MobileAds
import net.packsi.tunnels.data.auth.ApiClient

class IranConnectionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        MobileAds.initialize(this)
    }
}
