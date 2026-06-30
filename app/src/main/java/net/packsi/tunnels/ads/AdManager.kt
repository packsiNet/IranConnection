package net.packsi.tunnels.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages preloading and showing of interstitial ads for the free-tier ad session flow.
 *
 * Replace INTERSTITIAL_UNIT_ID with your real Ad Unit ID from the AdMob console before release.
 * The current value is Google's official test ID and will only show test ads.
 */
object AdManager {

    // TODO: Replace with real Ad Unit ID before publishing
    private const val INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Preloads an interstitial ad in the background. Safe to call multiple times —
     * will not start a new load if one is already in flight or if an ad is ready.
     */
    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            },
        )
    }

    /**
     * Shows the preloaded interstitial ad and calls [onComplete] when the user dismisses it.
     * If no ad is ready (network issue, not loaded yet) [onComplete] is called immediately
     * so the VPN session still starts — the caller must handle this gracefully.
     * Automatically preloads the next ad after the current one is consumed.
     */
    fun showAd(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            // No ad available — proceed without showing one
            preload(activity)
            onComplete()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity)
                onComplete()
            }
        }
        ad.show(activity)
    }

    fun isAdReady(): Boolean = interstitialAd != null
}
