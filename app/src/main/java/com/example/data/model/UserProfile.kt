package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = "user", // "admin" or "user"
    val creationDate: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)
