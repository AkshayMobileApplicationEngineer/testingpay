
package com.app.quizparlour.Network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class RazorpayOrderRequest(
    val amount: Int,
    val currency: String = "INR",
    val receipt: String,
    val notes: Map<String, String>
)

data class RazorpayOrderResponse(
    val id: String,
    val amount: Int,
    val amount_due: Int,
    val currency: String,
    val receipt: String,
    val status: String
)

interface RazorpayApiService {
    @Headers("Content-Type: application/json")
    @POST("orders")
    fun createOrder(
        @Body orderRequest: RazorpayOrderRequest
    ): Call<RazorpayOrderResponse>
}
