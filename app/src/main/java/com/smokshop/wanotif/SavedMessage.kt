package com.smokshop.wanotif

data class SavedMessage(
    val timestamp: String,
    val sender: String,
    val message: String,
    val type: String,       // "DM" veya "Grup"
    val packageName: String // com.whatsapp veya com.whatsapp.w4b
)
