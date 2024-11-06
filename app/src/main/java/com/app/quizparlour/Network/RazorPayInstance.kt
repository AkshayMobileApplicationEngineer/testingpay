package com.app.quizparlour.Network


import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RazorPayInstance {

    private const val BASE_URL = "https://api.razorpay.com/v1/"

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", Credentials.basic("rzp_live_0e7KaXdVczrhK6", "APpK6rVzdSFLoNmKYa9LaGpE"))
            .method(original.method, original.body)
            .build()
        chain.proceed(request)
    }.build()

    val apiService: RazorpayApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RazorpayApiService::class.java)
    }
}
