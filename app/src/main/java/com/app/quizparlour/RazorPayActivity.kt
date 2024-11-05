package com.app.quizparlour

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

class RazorPayActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var amountEditText: EditText
    private lateinit var buyNowButton: Button
    private val userId = "1902" // Set user ID here
    private val username = "Akshay Kumar Prajapati"
    private val contactNumber = "8987918309"
    private val emailAdress = "meliveakshay@gmail.com"
    private val companyName = "Quiz Parlour"
    private val razor_pay_api_key = "rzp_test_XTRFi55yS7MOsO"

    companion object {
        private const val TAG = "RazorPayActivity" // Tag for logging
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set layout
        Checkout.preload(applicationContext) // Preload Razorpay checkout
        initialView() // Initialize views
        setup() // Setup button click listeners
    }

    private fun initialView() {
        amountEditText = findViewById(R.id.amountEditText) // EditText for amount input
        buyNowButton = findViewById(R.id.buyNowButton) // Button to initiate payment
    }

    private fun setup() {
        buyNowButton.setOnClickListener {
            val amountText = amountEditText.text.toString() // Get the amount entered by user

            // Check if amount is empty
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Please enter amount") // Log error
            } else {
                val amount = amountText.toDoubleOrNull() // Parse amount to Double
                // Validate amount
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Invalid amount entered: $amountText") // Log error
                } else {
                    Log.d(TAG, "Payment Initiated for amount: $amount") // Log payment initiation
                    createOrderId(amountText) // Create order ID
                }
            }
        }
    }

    private fun createOrderId(amount: String) {
        val apiService = RazorPayInstance.apiService
        val amountInPaise = (amount.toDouble() * 100).toInt() // Convert amount to paise
        val orderRequest = RazorpayOrderRequest(
            amount = amountInPaise,
            currency = "INR",
            receipt = "Quiz Play for live",
            notes = mapOf(
                "notes_key_1" to "Quiz Parlour",
                "notes_key_2" to "Mahatma Gandhi Quiz"
            )
        )

        // Make API call to create order
        apiService.createOrder(orderRequest).enqueue(object : Callback<RazorpayOrderResponse> {
            override fun onResponse(call: Call<RazorpayOrderResponse>, response: Response<RazorpayOrderResponse>) {
                if (response.isSuccessful) {
                    val orderResponse = response.body()
                    val orderId = orderResponse?.id
                    if (orderId != null) {
                        // Order created successfully
                        Toast.makeText(this@RazorPayActivity, "Order ID: $orderId", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Order created successfully: $orderId") // Log order ID
                        startPayment(userId = userId, amount = amount, description = "Payment for Quiz", orderId = orderId) // Start payment
                    } else {
                        Log.e(TAG, "Order ID not found in response")
                        Toast.makeText(this@RazorPayActivity, "Failed to create order", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Failed to create order: ${response.errorBody()?.string()}")
                    Toast.makeText(this@RazorPayActivity, "Failed to create order", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RazorpayOrderResponse>, t: Throwable) {
                Log.e(TAG, "Error creating order: ${t.message}")
                Toast.makeText(this@RazorPayActivity, "Error creating order", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startPayment(userId: String, amount: String, description: String, orderId: String) {
        val co = Checkout()
        co.setKeyID(razor_pay_api_key) // Set Razorpay API key
        try {
            val options = JSONObject().apply {
                put("name", companyName) // Company name
                put("description", description) // Payment description
                put("image", "https://quizparlour.in/assets/img/logo.png") // Company logo
                put("theme.color", "#3a1486") // Theme color
                put("currency", "INR") // Currency
                put("amount", (amount.toDouble() * 100).toInt()) // Amount in paise
                put("order_id", orderId) // Order ID
                put("retry", JSONObject().apply {
                    put("enabled", true) // Retry enabled
                    put("max_count", 4) // Max retry count
                })
                put("prefill", JSONObject().apply {
                    put("name", username) // User name
                    put("email", emailAdress) // User email
                    put("contact", contactNumber) // User contact
                })
                put("method", JSONObject().apply {
                    put("netbanking", true) // Enable net banking
                    put("card", true) // Enable card payments
                    put("wallet", true) // Enable wallets
                    put("upi", true) // Enable UPI
                })
            }
            co.open(this, options) // Open Razorpay checkout
        } catch (e: Exception) {
            Log.e(TAG, "Error in startPayment: ${e.message}")
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(transactionId: String?) {
        Log.d(TAG, "Transaction successful: ID $transactionId")
        val transactionAmount = amountEditText.text.toString().toDoubleOrNull()?.toInt() ?: 0 // Get amount entered by the user
        val transactionType = "ADD"
        val transactionStatus = "COMPLETED"
        val transactionDescription = "Money Added To Wallet"

        // Send transaction status to server
        sendTransactionStatus(userId, transactionAmount, transactionType, transactionStatus, transactionDescription)
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Transaction failed with code $errorCode: $errorMessage")
        val transactionAmount = amountEditText.text.toString().toDoubleOrNull()?.toInt() ?: 0 // Get amount entered by the user
        val transactionType = "ADD"
        val transactionStatus = "FAILED"
        val transactionDescription = "Money Add Failed"

        // Send transaction status to server
        sendTransactionStatus(userId, transactionAmount, transactionType, transactionStatus, transactionDescription)
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
            userId = userId.toInt(), // Convert userId to Int if necessary
            transactionAmount = amount.toDouble(), // Ensure this matches the expected type
            transactionType = type,
            transactionStatus = status,
            transactionDescription = description
        ).enqueue(object : Callback<TransactionResponse> {
            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        // Check transaction status and show dialog accordingly
                        when (responseBody.status) {
                            1.toString() -> showDialog("Transaction Successful", "Transaction successful. Transaction ID: ${responseBody.data?.transaction_id ?: "N/A"}. Message: $description")
                            0.toString() -> showDialog("Transaction Failed", "Transaction failed. Transaction ID: ${responseBody.data?.transaction_id ?: "N/A"}. Message: $description")
                            else -> showDialog("Transaction Failed", "Unknown status code: ${responseBody.status}")
                        }
                    } ?: showDialog("Transaction Failed", "Response body is null.")
                } else {
                    Log.e(TAG, "Failed to send transaction data: ${response.errorBody()?.string()}")
                    showDialog("Transaction Failed", "Failed to log transaction")
                }
            }

            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                Log.e(TAG, "Error sending transaction data: ${t.message}")
                showDialog("Transaction Failed", "Error logging transaction")
            }
        })
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
