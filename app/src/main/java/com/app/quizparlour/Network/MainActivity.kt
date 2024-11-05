package com.app.quizparlour.Network//package com.app.quizparlour
//
//import android.app.AlertDialog
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.razorpay.Checkout
//import com.razorpay.PaymentResultListener
//import org.json.JSONObject
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.text.SimpleDateFormat
//import java.util.*
//
//class MainActivity : AppCompatActivity(), PaymentResultListener {
//
//    private lateinit var amountEditText: EditText
//    private lateinit var buyNowButton: Button
//    private val userId = "1902" // Example user ID, should be dynamic based on the logged-in user
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        Checkout.preload(applicationContext) // Preload checkout for faster payment initiation
//
//        //amountEditText = findViewById(R.id.amountEditText)
//        //buyNowButton = findViewById(R.id.buyNowButton)
//
//        buyNowButton.setOnClickListener {
//            val transactionAmount = amountEditText.text.toString()
//            if (transactionAmount.isNotEmpty()) {
//                Log.d("TAG", "Initiating order creation for amount: $transactionAmount")
//                createOrder(transactionAmount)
//            } else {
//                Log.e("TAG", "Transaction amount is empty.")
//                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun createOrder(amount: String) {
//        val apiService = RetrofitInstance.apiService
//        val createDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
//
//        val orderId = "${String.format("%06d", (Math.random() * 1000000).toInt())}${Calendar.getInstance().timeInMillis.toString().take(2).uppercase()}"
//        apiService.createOrder(orderId, amount.toDouble(), createDate).enqueue(object : Callback<OrderId> {
//            override fun onResponse(call: Call<OrderId>, response: Response<OrderId>) {
//                if (response.isSuccessful && response.body()?.status == "1") {
//                    val orderId = response.body()!!.data
//                    if (orderId.isNotEmpty()) {
//                        val description="Going for the test "
//                        Log.d("TAG", "Order created successfully with ID: $orderId")
//                        startPayment(userId, amount, description, orderId)
//                    } else {
//                        Log.e("TAG", "Order ID is empty.")
//                        Toast.makeText(this@MainActivity, "Order ID is empty", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    val error = response.errorBody()?.string() ?: "Unknown error"
//                    Log.e("TAG", "Failed to create order: $error")
//                    Toast.makeText(this@MainActivity, "Failed to create order", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<OrderId>, t: Throwable) {
//                Log.e("TAG", "Order creation failed: ${t.message}")
//                Toast.makeText(this@MainActivity, "Order creation error: ${t.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    private fun startPayment(userId: String, amount: String, description: String, orderId: String) {
//        val co = Checkout()
//        co.setKeyID("rzp_test_XTRFi55yS7MOsO")
//        try {
//            val options = JSONObject().apply {
//                put("name", "Quiz Parlour")
//                put("description", description)
//                put("image", R.drawable.ic_launcher_background)
//                put("theme.color", "#3399cc")
//                put("currency", "INR")
//                put("amount", (amount.toDouble() * 100).toInt()) // Amount in paise
//                put("order_id", orderId)
//                put("retry", JSONObject().apply {
//                    put("enabled", true)
//                    put("max_count", 4)
//                })
//                put("prefill", JSONObject().apply {
//                    put("name", "Akshay Kumar Prajapati")
//                    put("email", "akshaykumarprajapati@example.com")
//                    put("contact", "8987918309")
//                })
//                put("method", JSONObject().apply {
//                    put("netbanking", true)
//                    put("card", true)
//                    put("wallet", true)
//                    put("upi", true)
//                })
//            }
//            co.open(this, options)
//        } catch (e: Exception) {
//            Log.e("TAG", "Error in startPayment: ${e.message}")
//            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//
//
//    private fun handleTransactionStatus(status: String, response: String?, amount: Double) {
//        try {
//            sendTransactionToApi(userId, amount, "ADD", status, response ?: "Transaction status unknown.")
//        } catch (e: Exception) {
//            Log.e("TAG", "Error in handleTransactionStatus: ${e.message}")
//        }
//    }
//
//    override fun onPaymentSuccess(response: String?) {
//        Log.d("TAG", "Payment Successful: $response")
//        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
//        handleTransactionStatus("COMPLETED", response ?: "Success", amountEditText.text.toString().toDoubleOrNull() ?: 0.0)
//    }
//
//    override fun onPaymentError(code: Int, response: String?) {
//        Log.e("TAG", "Payment Error: $response")
//        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()
//        handleTransactionStatus("DECLINED", response ?: "Payment failed", amountEditText.text.toString().toDoubleOrNull() ?: 0.0)
//    }
//
//    private fun sendTransactionToApi(userId: String, transactionAmount: Double, transactionType: String, transactionStatus: String, transactionDescription: String) {
//        val apiService = RetrofitInstance.apiService
//        apiService.logTransaction(
//            userId = userId.toInt(),
//            transactionAmount = transactionAmount,
//            transactionType = transactionType,
//            transactionStatus = transactionStatus,
//            transactionDescription = transactionDescription
//        ).enqueue(object : Callback<TransactionResponse> {
//            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
//                if (response.isSuccessful) {
//                    response.body()?.let { responseBody ->
//                        val dialogTitle = if (responseBody.status == "1") "Transaction Successful" else "Transaction Failed"
//                        val dialogMessage = "Transaction ID: ${responseBody.data?.transaction_id ?: "N/A"}\n$transactionDescription"
//                        showDialog(dialogTitle, dialogMessage)
//                    } ?: showDialog("Transaction Failed", "Response body is null.")
//                } else {
//                    Log.e("TAG", "Failed to log transaction: ${response.errorBody()?.string()}")
//                    showDialog("Transaction Failed", "Failed to log transaction")
//                }
//            }
//
//            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
//                Log.e("TAG", "Transaction logging failed: ${t.message}")
//                showDialog("Error", "Error: ${t.message}")
//            }
//        })
//    }
//
//
//    private fun showDialog(title: String, message: String) {
//        AlertDialog.Builder(this)
//            .setTitle(title)
//            .setMessage(message)
//            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
//            .create()
//            .show()
//    }
//}
