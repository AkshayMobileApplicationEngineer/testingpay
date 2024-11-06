package com.app.quizparlour

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.app.quizparlour.ModelData.TransactionResponse
import com.app.quizparlour.Network.RazorPayInstance
import com.app.quizparlour.Network.RazorpayOrderRequest
import com.app.quizparlour.Network.RazorpayOrderResponse
import com.app.quizparlour.Network.RetrofitInstance
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.Locale

class RazorPayActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var amountEditText: EditText
    private lateinit var buyNowButton: Button
    private val userId = "1902"
    private val username = "Akshay Kumar Prajapati"
    private val contactNumber = "8987918309"
    private val emailAddress = "meliveakshay@gmail.com"
    private val companyName = "Quiz Parlour"
    private val razorPayApiKey = "rzp_live_0e7KaXdVczrhK6"

    companion object {
        private const val TAG = "RazorPayActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Checkout.preload(applicationContext)
        initializeView()
        setupListeners()
    }

    private fun initializeView() {
        amountEditText = findViewById(R.id.amountEditText)
        buyNowButton = findViewById(R.id.buyNowButton)
    }

    private fun setupListeners() {
        buyNowButton.setOnClickListener {
            val amountText = amountEditText.text.toString()
            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Invalid amount: $amountText")
                return@setOnClickListener
            }

            Log.d(TAG, "Payment Initiated with amount: $amount")
            createOrderId(amount)
        }
    }

    private fun createOrderId(amount: Double) {
        val apiService = RazorPayInstance.apiService
        val amountInPaise = (amount * 100).toInt()
        val orderRequest = RazorpayOrderRequest(
            amount = amountInPaise,
            currency = "INR",
            receipt = "Quiz Play for live",
            notes = mapOf(
                "notes_key_1" to "Quiz Parlour",
                "notes_key_2" to "Mahatma Gandhi Quiz"
            )
        )

        apiService.createOrder(orderRequest).enqueue(object : Callback<RazorpayOrderResponse> {
            override fun onResponse(call: Call<RazorpayOrderResponse>, response: Response<RazorpayOrderResponse>) {
                if (response.isSuccessful) {
                    val orderId = response.body()?.id
                    if (orderId != null) {

                        startPayment(userId, amount.toString(), "Payment for Quiz", orderId)
                        Log.d(TAG, "Order created successfully: $orderId")
                    } else {
                        handleOrderFailure("Order ID not found in response")
                    }
                } else {
                    handleOrderFailure("Failed to create order: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RazorpayOrderResponse>, t: Throwable) {
                handleOrderFailure("Error creating order: ${t.message}")
            }
        })
    }





    private fun startPayment(userId: String, amount: String, description: String, orderId: String) {
        val co = Checkout()
        co.setKeyID(razorPayApiKey)
        try {
            val options = JSONObject().apply {
                put("name", companyName)
                put("description", description)
                put("image", "https://quizparlour.in/assets/img/logo.png")
                put("theme.color", "#3399cc")
                put("currency", "INR")
                put("amount", (amount.toDouble() * 100).toInt())
                put("order_id", orderId)
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 4)
                })
                put("prefill", JSONObject().apply {
                    put("name", username)
                    put("email", emailAddress)
                    put("contact", contactNumber)
                })
                put("method", JSONObject().apply {
                    put("netbanking", true)
                    put("card", true)
                    put("wallet", true)
                    put("upi", true)
                })
            }
            co.open(this, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error in startPayment: ${e.message}")
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(transactionId: String?) {
        Log.d(TAG, "Transaction successful: ID $transactionId")
        handleTransactionResult("COMPLETED", "Money Added To Wallet")
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Transaction failed with code $errorCode: $errorMessage")
        handleTransactionResult("FAILED", "Money Add Failed")
    }

    private fun handleTransactionResult(status: String, description: String) {
        val transactionAmount = amountEditText.text.toString().toDoubleOrNull()?.toInt() ?: 0
        sendTransactionStatus(userId, transactionAmount, "ADD", status, description)
    }

    private fun sendTransactionStatus(
        userId: String,
        amount: Int,
        type: String,
        status: String,
        description: String
    ) {
        val apiService = RetrofitInstance.apiService
        apiService.sendTransactionStatus(
            userId = userId.toInt(),
            transactionAmount = amount.toDouble(),
            transactionType = type,
            transactionStatus = status,
            transactionDescription = description
        ).enqueue(object : Callback<TransactionResponse> {
            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                if (response.isSuccessful) {
                    val message = if (response.body()?.status == "1") "Transaction Successful" else "Transaction Failed"
                    showDialog(message, "Transaction ID: ${response.body()?.data?.transaction_id ?: "N/A"}")
                } else {
                    showDialog("Transaction Failed", "Failed to log transaction")
                }
            }

            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                showDialog("Transaction Failed", "Network error: ${t.message}")
            }
        })
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun handleOrderFailure(errorMessage: String) {
        Log.e(TAG, errorMessage)
        Toast.makeText(this, "Failed to create order", Toast.LENGTH_SHORT).show()
    }
}
