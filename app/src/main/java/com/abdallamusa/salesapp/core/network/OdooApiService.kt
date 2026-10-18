package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.core.network.model.OdooRpcResponse
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface OdooApiService {

    @POST("web/database/list")
    suspend fun listDatabases(
        @Body request: OdooRpcRequest<Map<String, Any?>> = OdooRpcRequest(params = emptyMap())
    ): Response<OdooRpcResponse<JsonElement>>

    @POST("web/session/authenticate")
    suspend fun authenticate(
        @Body request: OdooRpcRequest<Map<String, Any?>>
    ): Response<OdooRpcResponse<JsonElement>>

    @POST("web/dataset/call_kw/{model}/{method}")
    suspend fun callKw(
        @Path("model") model: String,
        @Path("method") method: String,
        @Body request: OdooRpcRequest<Map<String, Any?>>
    ): Response<OdooRpcResponse<JsonElement>>
}