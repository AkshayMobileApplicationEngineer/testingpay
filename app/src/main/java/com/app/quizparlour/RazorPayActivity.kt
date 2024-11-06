package com.app.quizparlour

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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
        private const val TAG = "RazorPayActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Checkout.preload(applicationContext)
        initialView()
        setup()
    }

    private fun initialView() {
        amountEditText = findViewById(R.id.amountEditText)
        buyNowButton = findViewById(R.id.buyNowButton)
    }

    private fun setup() {
        buyNowButton.setOnClickListener {
            val amountText = amountEditText.text.toString()

            if (amountText.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Please enter amount")
            } else {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Payment Initiated")
                    createOrderId(amountText)
                }
            }
        }
    }

    private fun createOrderId(amount: String) {
        val apiService = RazorPayInstance.apiService
        val amountInPaise = (amount.toDouble() * 100).toInt() // Razorpay expects amount in paise
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
                    val orderResponse = response.body()
                    val orderId = orderResponse?.id
                    if (orderId != null) {
                        Toast.makeText(this@RazorPayActivity, orderId.toString(), Toast.LENGTH_SHORT).show()
                        startPayment(userId = userId, amount = amount, description = "Payment for Quiz", orderId = orderId)
                        Log.d(TAG, "Order created successfully: $orderId")
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
        co.setKeyID(razor_pay_api_key)
        try {
            val options = JSONObject().apply {
                put("name", companyName)
                put("description", description)
                put("image", "https://quizparlour.in/assets/img/logo.png")
                put("theme.color", "#3399cc")
                put("currency", "INR")
                put("amount", (amount.toDouble() * 100).toInt()) // Amount in paise
                put("order_id", orderId)
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 4)
                })
                put("prefill", JSONObject().apply {
                    put("name", username)
                    put("email", emailAdress)
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
        val transactionAmount = amountEditText.text.toString().toDoubleOrNull()?.toInt() ?: 0
        val transactionType = "ADD"
        val transactionStatus = "COMPLETED"
        val transactionDescription = "Money Added To Wallet"

        sendTransactionStatus(userId, transactionAmount, transactionType, transactionStatus, transactionDescription)
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Transaction failed with code $errorCode: $errorMessage")
        val transactionAmount = amountEditText.text.toString().toDoubleOrNull()?.toInt() ?: 0
        val transactionType = "ADD"
        val transactionStatus = "FAILED"
        val transactionDescription = "Money Add Failed"

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
            userId = userId.toInt(),
            transactionAmount = amount.toDouble(),
            transactionType = type,
            transactionStatus = status,
            transactionDescription = description
        ).enqueue(object : Callback<TransactionResponse> {
            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val title = if (responseBody.status == "1") "Transaction Successful" else "Transaction Failed"
                        val imageResource = if (responseBody.status == "1") R.drawable.payment_success else R.drawable.payment_failed
                        showTransactionDialog(title,
                            responseBody.data?.transaction_id.toString(), description, imageResource)
                    }
                } else {
                    showTransactionDialog("Transaction Failed", "N/A", "Failed to log transaction", R.drawable.payment_failed)
                }
            }

            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                showTransactionDialog("Transaction Failed", "N/A", "Network error: ${t.message}", R.drawable.payment_failed)
            }
        })
    }

    private fun showTransactionDialog(title: String, transactionId: String?, description: String, imageResource: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.transaction_result_dialog, null)

        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        val dialogTransactionId: TextView = dialogView.findViewById(R.id.dialogTransactionId)
        val dialogDescription: TextView = dialogView.findViewById(R.id.dialogDescription)
        val paymentStatusImage: ImageView = dialogView.findViewById(R.id.paymentStatusImage)
        val okButton: Button = dialogView.findViewById(R.id.okButton)

        dialogTitle.text = title
        dialogTransactionId.text = "Transaction ID: $transactionId"
        dialogDescription.text = "Description: $description"
        paymentStatusImage.setImageResource(imageResource)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
