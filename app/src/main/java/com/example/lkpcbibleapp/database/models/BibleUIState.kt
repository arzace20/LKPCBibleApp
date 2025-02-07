package com.example.lkpcbibleapp.database.models

data class BibleUiState(
    val books: List<String> = emptyList(),
    val currentBook: String = "창세기",
    val currentChapter: Int = 1,
    val currentVerse: Int = 1,
    val currentVerseText: String = "태초에 하나님이 천지를 창조하시니라",
    val currentVerseTextEnglish: String = "In the beginning God created the heavens and the earth."
)
