package com.example.desafiopractico2_dsm

data class Task( // ← esto debe ser "data class", no solo "class"
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var isCompleted: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)
