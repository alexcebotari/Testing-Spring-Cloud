package com.example.demo

data class ServiceResponse<T>(val status: String, val data: T)

data class DummyServiceResponse(var status: String = "", var data: GetPaymentStatusData = GetPaymentStatusData())

data class GetPaymentStatusData(
    var paymentId: String = "",
    var status: String = "",
    var paymentReference: String = "",
    var paymentDate: String = "",
    var amount: Double = 0.0
)