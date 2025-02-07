package com.example.lkpcbibleapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lkpcbibleapp.viewmodel.BibleViewModel
import com.example.lkpcbibleapp.viewmodel.BibleViewModelFactory
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu as M3DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.input.pointer.positionChange
import com.example.lkpcbibleapp.parser.BtxParser
import com.example.lkpcbibleapp.database.models.BibleUiState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.lkpcbibleapp.ui.theme.LKPCBibleAppTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Install the splash screen before setting content
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            // ✅ Automatically applies light or dark mode based on system settings
            val darkTheme = isSystemInDarkTheme()

            LKPCBibleAppTheme(darkTheme = darkTheme) {
                BibleApp(viewModel = BibleViewModel(this))
            }
        }
    }
}

@Composable
fun BibleApp(viewModel: BibleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    // ✅ Observe uiState directly for reactivity
    var selectedBook by remember(uiState.currentBook) { mutableStateOf(uiState.currentBook) }
    var selectedChapter by remember(uiState.currentChapter) { mutableStateOf(uiState.currentChapter) }
    var selectedVerse by remember(uiState.currentVerse) { mutableStateOf(uiState.currentVerse) }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // ✅ Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("책명 장:절 (예: 창세기 1:1)") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(end = 8.dp),
                singleLine = true
            )
            Button(onClick = {
                viewModel.jumpToVerse(searchQuery)
                selectedBook = uiState.currentBook
                selectedChapter = uiState.currentChapter
                selectedVerse = uiState.currentVerse
            }) {
                Text("Go")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Book, Chapter, and Verse Dropdowns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropdownMenu(viewModel, uiState, selectedBook) { newBook ->
                selectedBook = newBook
                selectedChapter = 1
                selectedVerse = 1
                viewModel.loadChapter(newBook, 1)
            }

            ChapterDropdownSelector(
                selectedBook = selectedBook,
                selectedChapter = selectedChapter,
                onChapterSelected = { newChapter ->
                    selectedChapter = newChapter
                    selectedVerse = 1
                    viewModel.loadChapter(selectedBook, newChapter)
                },
                viewModel = viewModel
            )

            VerseDropdownSelector(
                selectedBook = selectedBook,
                selectedChapter = selectedChapter,
                selectedVerse = selectedVerse,
                onVerseSelected = { newVerse ->
                    selectedVerse = newVerse
                    viewModel.setSelectedVerse(newVerse)
                },
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Verse Display (Korean + English)
        VerseDisplay(uiState, viewModel)

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Navigation Buttons (Previous, Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                viewModel.previousVerse()
                // ✅ Update dropdowns when navigating
                selectedBook = uiState.currentBook
                selectedChapter = uiState.currentChapter
                selectedVerse = uiState.currentVerse
            }) {
                Text("Previous")
            }
            Button(onClick = {
                viewModel.nextVerse()
                // ✅ Update dropdowns when navigating
                selectedBook = uiState.currentBook
                selectedChapter = uiState.currentChapter
                selectedVerse = uiState.currentVerse
            }) {
                Text("Next")
            }
        }
    }
}


@Composable
fun ChapterDropdownSelector(
    selectedBook: String,
    selectedChapter: Int,
    onChapterSelected: (Int) -> Unit,
    viewModel: BibleViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val lastChapter = viewModel.findLastChapter(selectedBook)
    val options = (1..lastChapter).toList()

    Box {
        Button(onClick = { expanded = true }) {
            Text("${if (selectedBook == "Psalms") "편" else "장"}: $selectedChapter") // ✅ Always Korean
        }
        M3DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { chapter ->
                DropdownMenuItem(
                    text = { Text(chapter.toString()) },
                    onClick = {
                        onChapterSelected(chapter)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VerseDropdownSelector(
    selectedBook: String,
    selectedChapter: Int,
    selectedVerse: Int,
    onVerseSelected: (Int) -> Unit,
    viewModel: BibleViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val lastVerse = viewModel.findLastVerse(selectedBook, selectedChapter)
    val options = (1..lastVerse).toList()

    Box {
        Button(onClick = { expanded = true }) {
            Text("절: $selectedVerse") // ✅ Always Korean
        }
        M3DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { verse ->
                DropdownMenuItem(
                    text = { Text(verse.toString()) },
                    onClick = {
                        onVerseSelected(verse)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun DropdownMenu(
    viewModel: BibleViewModel,
    uiState: BibleUiState,
    selectedBook: String,
    onBookSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedBook) // ✅ Uses passed `selectedBook`
        }
        M3DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val bookList = BtxParser.bookMapKorean.values // ✅ Always show books in Korean
            bookList.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book) },
                    onClick = {
                        onBookSelected(book) // ✅ Use callback to update the selected book
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VerseDisplay(uiState: BibleUiState, viewModel: BibleViewModel) {
    val sensitivity = 50f // Lower = More sensitive
    val decayFactor = 0.8f // Reduces accidental double swipes
    val isDarkMode = isSystemInDarkTheme()

    val textColor = Color.LightGray //if (isDarkMode) Color.LightGray else Color.Black

    val gestureDetector = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
            if (dragAmount > sensitivity) {
                viewModel.previousVerse()
            } else if (dragAmount < -sensitivity) {
                viewModel.nextVerse()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .then(gestureDetector) // ✅ Attach swipe gestures
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // ✅ Book Name, Chapter, and Verse
            Text(
                text = "${uiState.currentBook} ${uiState.currentChapter}:${uiState.currentVerse}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ✅ Korean Verse
            Text(
                text = uiState.currentVerseText,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = textColor,
                modifier = Modifier.padding(16.dp)
            )

            // ✅ English Verse (Same Style)
            Text(
                text = uiState.currentVerseTextEnglish,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = textColor,
                modifier = Modifier.padding(16.dp)
            )
        }

        // ✅ Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.previousVerse() }) {
                Text("Previous")
            }
            Button(onClick = { viewModel.nextVerse() }) {
                Text("Next")
            }
        }
    }
}





