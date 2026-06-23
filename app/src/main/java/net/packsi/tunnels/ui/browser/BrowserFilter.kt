package net.packsi.tunnels.ui.browser

import android.net.Uri
import android.webkit.WebResourceResponse

enum class FilterDecision { ALLOWED, BLOCKED, NEEDS_IP_CHECK }

object BrowserFilter {

    // High-traffic or policy-blocked Iranian sites — always blocked regardless of IP
    val BLOCKLIST: Set<String> = setOf(
        "filimo.com",
        "telewebion.com",
        "namava.ir",
    )

    // Iranian services that use non-.ir domains (often via CDN — IP check would fail)
    private val IRANIAN_WHITELIST: Set<String> = setOf(
        "digikala.com",
        "sheypoor.com",
        "torob.com",
        "aparat.com",
        "arzdigital.com",
        "virgool.io",
        "namnak.com",
        "tasnimnews.com",
        "digiato.com",
        "snapp.ir",
        "tapsi.ir",
    )

    /**
     * Fast, synchronous decision — no DNS needed.
     * Call this before [IranianIpChecker.isIranianHost] to short-circuit obvious cases.
     * [host] must already be lowercase and stripped of "www." prefix.
     */
    fun quickCheck(host: String): FilterDecision {
        if (BLOCKLIST.any { host == it || host.endsWith(".$it") }) return FilterDecision.BLOCKED
        if (host.endsWith(".ir")) return FilterDecision.ALLOWED
        if (IRANIAN_WHITELIST.any { host == it || host.endsWith(".$it") }) return FilterDecision.ALLOWED
        return FilterDecision.NEEDS_IP_CHECK
    }

    fun blockedResponse(url: String): WebResourceResponse =
        WebResourceResponse("text/html", "UTF-8", blockedHtml(url).byteInputStream(Charsets.UTF_8))

    fun blockedHtml(url: String): String {
        val host = try { Uri.parse(url).host ?: url } catch (_: Exception) { url }
        return """
            <!DOCTYPE html><html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body { font-family: sans-serif; display: flex; justify-content: center;
                           align-items: center; min-height: 100vh; background: #f8f9fa;
                           padding: 24px; }
                    .card { background: white; border-radius: 16px; padding: 32px 24px;
                            max-width: 320px; width: 100%;
                            box-shadow: 0 2px 12px rgba(0,0,0,0.08); text-align: center; }
                    .icon { font-size: 40px; margin-bottom: 16px; }
                    h2 { color: #1a1a2e; font-size: 17px; margin-bottom: 8px; }
                    .host { color: #1a73e8; font-size: 13px; font-weight: bold;
                            margin-bottom: 12px; word-break: break-all; }
                    p { color: #666; font-size: 12px; line-height: 1.7; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon">🚫</div>
                    <h2>Site Not Available</h2>
                    <div class="host">$host</div>
                    <p>This browser is restricted to Iranian websites (.ir domains and approved Iranian services).</p>
                </div>
            </body></html>
        """.trimIndent()
    }
}
