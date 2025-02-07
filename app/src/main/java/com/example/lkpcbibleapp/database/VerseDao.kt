package com.example.lkpcbibleapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lkpcbibleapp.database.models.Verse
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter")
    fun getVerses(book: String, chapter: Int): Flow<List<Verse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(verses: List<Verse>): List<Long> // ✅ FIXED: Return List<Long>

    @Query("SELECT * FROM verses WHERE text LIKE '%' || :query || '%' LIMIT 20")
    fun searchVerses(query: String): Flow<List<Verse>>
}
