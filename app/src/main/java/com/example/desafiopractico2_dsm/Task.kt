package com.example.desafiopractico2_dsm

class Task (
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var isCompleted: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)