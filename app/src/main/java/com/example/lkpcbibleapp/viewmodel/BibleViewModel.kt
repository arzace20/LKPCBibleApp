package com.example.lkpcbibleapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lkpcbibleapp.database.models.BibleUiState
import com.example.lkpcbibleapp.database.models.Verse
import com.example.lkpcbibleapp.parser.BtxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BibleViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    private var versesKorean: List<Verse> = emptyList()
    private var versesEnglish: List<Verse> = emptyList()
    private var currentIndex = 0

    init {
        loadBibleData() // ✅ Loads both English & Korean data into cache
    }

    private fun loadBibleData() {
        viewModelScope.launch {
            BtxParser.loadBibleData(context)

            _uiState.value = _uiState.value.copy(
                books = BtxParser.bookMapKorean.values.toList() // ✅ Always show books in Korean
            )

            // ✅ Load Genesis 1:1 on app launch
            val firstBook = "창세기" // Korean name for Genesis
            val firstChapter = 1
            val firstVerse = 1

            versesKorean = BtxParser.getVerses(firstBook, firstChapter, isKorean = true)
            versesEnglish = BtxParser.getVerses(firstBook, firstChapter, isKorean = false)

            if (versesKorean.isNotEmpty() && versesEnglish.isNotEmpty()) {
                currentIndex = 0 // ✅ Start from Genesis 1:1
                _uiState.value = _uiState.value.copy(
                    currentBook = firstBook,
                    currentChapter = firstChapter,
                    currentVerse = firstVerse,
                    currentVerseText = versesKorean.first().text,
                    currentVerseTextEnglish = versesEnglish.first().text
                )
            } else {
                Log.e("BibleViewModel", "Failed to load Genesis 1:1")
            }
        }
    }


    fun loadChapter(book: String, chapter: Int) {
        viewModelScope.launch {
            versesKorean = BtxParser.getVerses(book, chapter, isKorean = true)
            versesEnglish = BtxParser.getVerses(book, chapter, isKorean = false)

            if (versesKorean.isNotEmpty()) {
                currentIndex = 0 // ✅ Always start from the first verse
                updateCurrentVerse()
            } else {
                Log.e("BibleViewModel", "No verses found for $book $chapter")
            }
        }
    }



    fun setSelectedVerse(verse: Int) {
        currentIndex = versesKorean.indexOfFirst { it.verse == verse }
        if (currentIndex != -1) {
            updateCurrentVerse()
        } else {
            Log.e("BibleViewModel", "Verse not found: ${_uiState.value.currentBook} ${_uiState.value.currentChapter}:$verse")
        }
    }

    fun nextVerse() {
        viewModelScope.launch {
            if (currentIndex < versesKorean.size - 1) {
                currentIndex++
            } else {
                moveToNextChapterOrBook()
            }
            updateCurrentVerse()
        }
    }


    fun previousVerse() {
        viewModelScope.launch {
            if (currentIndex > 0) {
                currentIndex--
            } else {
                moveToPreviousChapterOrBook()
            }
            updateCurrentVerse()
        }
    }


    private fun moveToNextChapterOrBook() {
        val currentBook = _uiState.value.currentBook
        val currentChapter = _uiState.value.currentChapter
        val books = _uiState.value.books

        val nextChapterVersesKorean = BtxParser.getVerses(currentBook, currentChapter + 1, isKorean = true)
        val nextChapterVersesEnglish = BtxParser.getVerses(currentBook, currentChapter + 1, isKorean = false)

        if (nextChapterVersesKorean.isNotEmpty()) {
            versesKorean = nextChapterVersesKorean
            versesEnglish = nextChapterVersesEnglish
            currentIndex = 0
            updateCurrentVerse()
            return
        }

        val currentBookIndex = books.indexOf(currentBook)
        if (currentBookIndex < books.size - 1) {
            val nextBook = books[currentBookIndex + 1]
            val firstChapterVersesKorean = BtxParser.getVerses(nextBook, 1, isKorean = true)
            val firstChapterVersesEnglish = BtxParser.getVerses(nextBook, 1, isKorean = false)

            if (firstChapterVersesKorean.isNotEmpty()) {
                versesKorean = firstChapterVersesKorean
                versesEnglish = firstChapterVersesEnglish
                currentIndex = 0
                _uiState.value = _uiState.value.copy(currentBook = nextBook, currentChapter = 1)
                updateCurrentVerse()
                return
            }
        }

        Log.e("BibleViewModel", "Reached the last verse of the Bible")
    }


    private fun moveToPreviousChapterOrBook() {
        val currentBook = _uiState.value.currentBook
        val currentChapter = _uiState.value.currentChapter
        val books = _uiState.value.books

        if (currentChapter > 1) {
            val previousChapterVersesKorean = BtxParser.getVerses(currentBook, currentChapter - 1, isKorean = true)
            val previousChapterVersesEnglish = BtxParser.getVerses(currentBook, currentChapter - 1, isKorean = false)

            if (previousChapterVersesKorean.isNotEmpty()) {
                versesKorean = previousChapterVersesKorean
                versesEnglish = previousChapterVersesEnglish
                currentIndex = previousChapterVersesKorean.size - 1
                updateCurrentVerse()
                return
            }
        }

        val currentBookIndex = books.indexOf(currentBook)
        if (currentBookIndex > 0) {
            val previousBook = books[currentBookIndex - 1]
            val lastChapter = findLastChapter(previousBook)
            val lastChapterVersesKorean = BtxParser.getVerses(previousBook, lastChapter, isKorean = true)
            val lastChapterVersesEnglish = BtxParser.getVerses(previousBook, lastChapter, isKorean = false)

            if (lastChapterVersesKorean.isNotEmpty()) {
                versesKorean = lastChapterVersesKorean
                versesEnglish = lastChapterVersesEnglish
                currentIndex = lastChapterVersesKorean.size - 1
                _uiState.value = _uiState.value.copy(currentBook = previousBook, currentChapter = lastChapter)
                updateCurrentVerse()
                return
            }
        }

        Log.e("BibleViewModel", "Reached the first verse of the Bible")
    }


    fun findLastChapter(book: String): Int {
        var chapter = 1
        while (BtxParser.getVerses(book, chapter, isKorean = true).isNotEmpty()) {
            chapter++
        }
        return chapter - 1
    }


    fun findLastVerse(book: String, chapter: Int): Int {
        val verses = BtxParser.getVerses(book, chapter, isKorean = true)
        return verses.maxOfOrNull { it.verse } ?: 1
    }


    fun jumpToVerse(input: String) {
        viewModelScope.launch {
            val pattern = """^([\p{L}\d\s]+)\s+(\d+):(\d+)$""".toRegex()
            val match = pattern.matchEntire(input.trim())

            if (match != null) {
                val bookNameInput = match.groupValues[1].trim()
                val chapter = match.groupValues[2].toInt()
                val verse = match.groupValues[3].toInt()

                // ✅ Determine if input is in Korean or English
                val matchedBookKorean = BtxParser.bookMapKorean.entries.firstOrNull { it.value == bookNameInput }?.value
                val matchedBookEnglish = BtxParser.bookMapEnglish.entries.firstOrNull { it.value.equals(bookNameInput, ignoreCase = true) }?.value
                val matchedBook = matchedBookKorean ?: matchedBookEnglish

                if (matchedBook != null) {
                    // ✅ Load verses for the selected book and chapter
                    versesKorean = BtxParser.getVerses(matchedBook, chapter, isKorean = true)
                    versesEnglish = BtxParser.getVerses(matchedBook, chapter, isKorean = false)

                    val verseIndex = versesKorean.indexOfFirst { it.verse == verse }

                    if (verseIndex != -1) {
                        // ✅ Update the UI state correctly so Next/Previous work properly
                        currentIndex = verseIndex
                        _uiState.value = _uiState.value.copy(
                            currentBook = matchedBook,
                            currentChapter = chapter,
                            currentVerse = verse,
                            currentVerseText = versesKorean[verseIndex].text,
                            currentVerseTextEnglish = versesEnglish.getOrNull(verseIndex)?.text ?: "Not Found"
                        )
                    } else {
                        Log.e("BibleViewModel", "Verse not found: $matchedBook $chapter:$verse")
                    }
                } else {
                    Log.e("BibleViewModel", "Book not found: $bookNameInput")
                }
            } else {
                Log.e("BibleViewModel", "Invalid input format: $input")
            }
        }
    }



    fun reloadBible() {
        viewModelScope.launch {
            BtxParser.loadBibleData(context) // ✅ Reload data
            _uiState.value = _uiState.value.copy(books = BtxParser.bookMapKorean.values.toList()) // ✅ Always display books in Korean

            val cachedVerses = BtxParser.getCachedVerses()
            if (cachedVerses.isNotEmpty()) {
                currentIndex = 0
                updateCurrentVerse()
            }
        }
    }

    private fun updateCurrentVerse() {
        if (currentIndex in versesKorean.indices) {
            val verseKorean = versesKorean[currentIndex]
            val verseEnglish = versesEnglish.getOrNull(currentIndex) // Might be missing, so handle safely

            _uiState.value = _uiState.value.copy(
                currentBook = verseKorean.book,
                currentChapter = verseKorean.chapter,
                currentVerse = verseKorean.verse,
                currentVerseText = verseKorean.text,
                currentVerseTextEnglish = verseEnglish?.text ?: "Not Found"
            )
        } else {
            Log.e("BibleViewModel", "Verse not found at index: $currentIndex")
        }
    }



}
