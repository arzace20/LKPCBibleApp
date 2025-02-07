package com.example.lkpcbibleapp.parser

import android.content.Context
import com.example.lkpcbibleapp.database.models.Verse
import android.util.Log

object BtxParser {
    var isKorean: Boolean = true // ✅ Toggle between English & Korean

    val bookMapEnglish = mapOf(
        "01" to "Genesis", "02" to "Exodus", "03" to "Leviticus", "04" to "Numbers", "05" to "Deuteronomy",
        "06" to "Joshua", "07" to "Judges", "08" to "Ruth", "09" to "1 Samuel", "10" to "2 Samuel",
        "11" to "1 Kings", "12" to "2 Kings", "13" to "1 Chronicles", "14" to "2 Chronicles",
        "15" to "Ezra", "16" to "Nehemiah", "17" to "Esther", "18" to "Job", "19" to "Psalms",
        "20" to "Proverbs", "21" to "Ecclesiastes", "22" to "Song of Songs", "23" to "Isaiah",
        "24" to "Jeremiah", "25" to "Lamentations", "26" to "Ezekiel", "27" to "Daniel",
        "28" to "Hosea", "29" to "Joel", "30" to "Amos", "31" to "Obadiah", "32" to "Jonah",
        "33" to "Micah", "34" to "Nahum", "35" to "Habakkuk", "36" to "Zephaniah",
        "37" to "Haggai", "38" to "Zechariah", "39" to "Malachi",
        "40" to "Matthew", "41" to "Mark", "42" to "Luke", "43" to "John",
        "44" to "Acts", "45" to "Romans", "46" to "1 Corinthians", "47" to "2 Corinthians",
        "48" to "Galatians", "49" to "Ephesians", "50" to "Philippians", "51" to "Colossians",
        "52" to "1 Thessalonians", "53" to "2 Thessalonians", "54" to "1 Timothy",
        "55" to "2 Timothy", "56" to "Titus", "57" to "Philemon", "58" to "Hebrews",
        "59" to "James", "60" to "1 Peter", "61" to "2 Peter", "62" to "1 John",
        "63" to "2 John", "64" to "3 John", "65" to "Jude", "66" to "Revelation"
    )

    val bookMapKorean = mapOf(
        "01" to "창세기", "02" to "출애굽기", "03" to "레위기", "04" to "민수기", "05" to "신명기",
        "06" to "여호수아", "07" to "사사기", "08" to "룻기", "09" to "사무엘상", "10" to "사무엘하",
        "11" to "열왕기상", "12" to "열왕기하", "13" to "역대상", "14" to "역대하",
        "15" to "에스라", "16" to "느헤미야", "17" to "에스더", "18" to "욥기", "19" to "시편",
        "20" to "잠언", "21" to "전도서", "22" to "아가", "23" to "이사야",
        "24" to "예레미야", "25" to "예레미야 애가", "26" to "에스겔", "27" to "다니엘",
        "28" to "호세아", "29" to "요엘", "30" to "아모스", "31" to "오바댜", "32" to "요나",
        "33" to "미가", "34" to "나훔", "35" to "하박국", "36" to "스바냐",
        "37" to "학개", "38" to "스가랴", "39" to "말라기",
        "40" to "마태복음", "41" to "마가복음", "42" to "누가복음", "43" to "요한복음",
        "44" to "사도행전", "45" to "로마서", "46" to "고린도전서", "47" to "고린도후서",
        "48" to "갈라디아서", "49" to "에베소서", "50" to "빌립보서", "51" to "골로새서",
        "52" to "데살로니가전서", "53" to "데살로니가후서", "54" to "디모데전서",
        "55" to "디모데후서", "56" to "디도서", "57" to "빌레몬서", "58" to "히브리서",
        "59" to "야고보서", "60" to "베드로전서", "61" to "베드로후서", "62" to "요한1서",
        "63" to "요한2서", "64" to "요한3서", "65" to "유다서", "66" to "요한계시록"
    )

    private val cachedVersesEnglish = mutableListOf<Verse>()
    private val cachedVersesKorean = mutableListOf<Verse>()

    fun loadBibleData(context: Context) {
        if (cachedVersesEnglish.isEmpty()) loadBTXFile(context, "niv.btx", cachedVersesEnglish, bookMapEnglish)
        Log.d("BtxParser", "English Bible loaded. Total verses: ${cachedVersesEnglish.size}")
        if (cachedVersesKorean.isEmpty()) loadBTXFile(context, "knrv.btx", cachedVersesKorean, bookMapKorean)
    }

    private fun loadBTXFile(context: Context, filename: String, cache: MutableList<Verse>, bookMap: Map<String, String>) {
        try {
            val inputStream = context.assets.open(filename)
            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("\\s+".toRegex(), limit = 3)
                    if (parts.size < 3) return@forEach

                    val bookKeyRaw = parts[0]
                    val chapterVerse = parts[1]
                    val verseText = parts[2]

                    val bookNumber = bookKeyRaw.take(2)
                    val chapterVerseParts = chapterVerse.split(":", limit = 2)
                    if (chapterVerseParts.size < 2) return@forEach

                    val chapter = chapterVerseParts[0].toIntOrNull() ?: return@forEach
                    val verseNumber = chapterVerseParts[1].toIntOrNull() ?: return@forEach

                    val bookName = bookMap[bookNumber]

                    if (bookName != null) {
                        cache.add(Verse(id = 0, book = bookName, chapter = chapter, verse = verseNumber, text = verseText))

                        // ✅ Debugging Logs
                        // Log.d("BtxParser", "Loaded Verse - File: $filename, Book: $bookName, Chapter: $chapter, Verse: $verseNumber")
                    } else {
                        Log.e("BtxParser", "Unknown book number in file: $filename, Raw Key: $bookNumber")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BtxParser", "Error loading BTX file: $filename", e)
        }

        Log.d("BtxParser", "Loaded ${cache.size} verses into cache for $filename")
    }

    fun getVerses(book: String, chapter: Int, isKorean: Boolean): List<Verse> {
        val lookupBook = if (isKorean) book else bookMapKorean.entries.firstOrNull { it.value == book }?.key?.let { bookMapEnglish[it] } ?: book

        val filteredVerses = if (isKorean) {
            cachedVersesKorean.filter { it.book == lookupBook && it.chapter == chapter }
        } else {
            cachedVersesEnglish.filter { it.book == lookupBook && it.chapter == chapter }
        }

        Log.d("BtxParser", "Book: $book, Chapter: $chapter, isKorean: $isKorean, Found: ${filteredVerses.size}")
        return filteredVerses
    }



    fun searchVerses(query: String): List<Verse> {
        return if (isKorean) {
            cachedVersesKorean.filter { it.text.contains(query, ignoreCase = true) }
        } else {
            cachedVersesEnglish.filter { it.text.contains(query, ignoreCase = true) }
        }
    }

    fun getCachedVerses(): List<Verse> {
        return if (isKorean) cachedVersesKorean else cachedVersesEnglish
    }

    fun getEnglishVerse(book: String, chapter: Int, verse: Int): String {
        val englishBook = bookMapEnglish.entries.firstOrNull { it.value == book }?.value ?: return ""
        return getVerses(englishBook, chapter, isKorean = false).firstOrNull { it.verse == verse }?.text ?: "English translation not available"
    }

}
