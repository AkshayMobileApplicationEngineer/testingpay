package com.app.quizparlour.Network

import com.app.quizparlour.ModelData.TransactionResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("razorpay/payapi.php")
    fun sendTransactionStatus(
        @Field("user_id") userId: Int,
        @Field("transaction_amount") transactionAmount: Double,
        @Field("transaction_type") transactionType: String,
        @Field("transaction_status") transactionStatus: String,
        @Field("transaction_description") transactionDescription: String
    ): Call<TransactionResponse>


}
