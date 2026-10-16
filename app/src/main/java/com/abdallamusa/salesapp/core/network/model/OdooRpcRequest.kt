package com.abdallamusa.salesapp.core.network.model

import com.google.gson.annotations.SerializedName

data class OdooRpcRequest<T>(
    @SerializedName("jsonrpc")
    val jsonRpc: String = "2.0",
    @SerializedName("params")
    val params: T
)