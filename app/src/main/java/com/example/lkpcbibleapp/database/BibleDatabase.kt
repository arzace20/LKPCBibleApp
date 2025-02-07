package com.example.lkpcbibleapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lkpcbibleapp.database.models.Verse  // ✅ Ensure correct import
import com.example.lkpcbibleapp.database.VerseDao

@Database(entities = [Verse::class], version = 1, exportSchema = false)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java, "bible.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
