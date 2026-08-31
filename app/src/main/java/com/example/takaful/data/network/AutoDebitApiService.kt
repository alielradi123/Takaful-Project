package com.example.takaful.data.network

import kotlinx.coroutines.delay

interface AutoDebitApiService {
    suspend fun processCardPayment(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        cardHolder: String,
        amount: Double,
        isRecurring: Boolean
    ): PaymentResult
}

sealed class PaymentResult {
    data class Success(val transactionId: String, val message: String) : PaymentResult()
    data class Error(val code: Int, val message: String) : PaymentResult()
}

class MockAutoDebitApiService : AutoDebitApiService {
    override suspend fun processCardPayment(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        cardHolder: String,
        amount: Double,
        isRecurring: Boolean
    ): PaymentResult {
        // Simulate network delay
        delay(2000)
        
        // Simple mock logic
        return if (cardNumber.length == 16 && cvv.length == 3) {
            PaymentResult.Success(
                transactionId = "CARD_${System.currentTimeMillis()}",
                message = "Payment Processed Successfully"
            )
        } else {
            PaymentResult.Error(
                code = 400,
                message = "Invalid Card Details"
            )
        }
    }
}
