package com.abdallamusa.salesapp.core.network.model

import com.google.gson.annotations.SerializedName

data class OdooRpcResponse<T>(
    @SerializedName("jsonrpc")
    val jsonRpc: String?,
    @SerializedName("result")
    val result: T?,
    @SerializedName("error")
    val error: OdooRpcError?
)

data class OdooRpcError(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: OdooRpcErrorData?
)

data class OdooRpcErrorData(
    @SerializedName("name")
    val name: String?,
    @SerializedName("message")
    val message: String?
)