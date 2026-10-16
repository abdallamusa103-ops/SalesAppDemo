package com.abdallamusa.salesapp.core.datastore

data class UserSession(

    val userId: Int? = null,
    val isInternalUser: Boolean = false,
    val sessionId: String? = null,
    val serverUrl: String? = null,
    val dbName: String? = null,
) {
    val isLoggedIn: Boolean
        get()
        = !sessionId.isNullOrEmpty() && userId != null && userId > 0

}
