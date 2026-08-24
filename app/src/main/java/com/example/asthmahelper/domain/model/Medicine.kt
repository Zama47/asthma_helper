package com.example.asthmahelper.domain.model

data class Medicine(
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val isPopular: Boolean = false
)