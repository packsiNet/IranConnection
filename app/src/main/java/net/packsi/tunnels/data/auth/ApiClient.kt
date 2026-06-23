package net.packsi.tunnels.data.auth

import android.content.Context
import com.google.gson.Gson
import net.packsi.tunnels.BuildConfig
import net.packsi.tunnels.data.payment.PaymentApi
import net.packsi.tunnels.data.subscription.SubscriptionApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Central HTTP layer. Call [init] once (from MainActivity) before use. */
object ApiClient {
    lateinit var authApi: AuthApi
        private set
    lateinit var userApi: UserApi
        private set
    lateinit var subscriptionApi: SubscriptionApi
        private set
    lateinit var paymentApi: PaymentApi
        private set

    val gson: Gson = Gson()

    fun init(context: Context) {
        TokenStore.init(context)

        val base = BuildConfig.BASE_URL.trimEnd('/') + "/"

        // Bare client/retrofit for refresh — no authenticator, so a failed
        // refresh can't recurse back into TokenAuthenticator.
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val refreshApi = Retrofit.Builder()
            .baseUrl(base)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApi::class.java)

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator(refreshApi))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        authApi = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        subscriptionApi = retrofit.create(SubscriptionApi::class.java)
        paymentApi = retrofit.create(PaymentApi::class.java)
    }
}
