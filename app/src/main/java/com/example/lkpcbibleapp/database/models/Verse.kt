package com.example.lkpcbibleapp.database.models  // ✅ Ensure this is correct

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verses")  // ✅ Ensure this annotation is present!
data class Verse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)
