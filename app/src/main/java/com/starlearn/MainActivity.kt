package com.starlearn

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class Flashcard(val id: Long, val word: String, val definition: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = FlashcardRepository(applicationContext)

        setContent {
            StarLearnTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(repo = repo, context = applicationContext)
                }
            }
        }
    }
}

enum class AppTab { DICTIONARY, FLASHCARDS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repo: FlashcardRepository, context: Context) {
    var currentTab by remember { mutableStateOf(AppTab.DICTIONARY) }
    val flashcards = remember { mutableStateListOf<Flashcard>().apply { addAll(repo.getAll()) } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (currentTab == AppTab.DICTIONARY) "Dictionary" else "Flashcards") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == AppTab.DICTIONARY,
                    onClick = { currentTab = AppTab.DICTIONARY },
                    label = { Text("Dictionary") },
                    icon = { Text("📘") }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.FLASHCARDS,
                    onClick = { currentTab = AppTab.FLASHCARDS },
                    label = { Text("Flashcards") },
                    icon = { Text("🧠") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            when (currentTab) {
                AppTab.DICTIONARY -> DictionaryScreen(
                    onSave = { word, definition ->
                        repo.add(word, definition)
                        flashcards.clear()
                        flashcards.addAll(repo.getAll())
                        Toast.makeText(context, "Added to flashcards", Toast.LENGTH_SHORT).show()
                    }
                )

                AppTab.FLASHCARDS -> FlashcardsScreen(
                    flashcards = flashcards,
                    onDelete = { card ->
                        repo.delete(card.id)
                        flashcards.remove(card)
                    },
                    onExport = {
                        val ok = exportFlashcards(context, flashcards)
                        Toast.makeText(
                            context,
                            if (ok) "Exported to Downloads" else "Export failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
fun DictionaryScreen(onSave: (String, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Enter word") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (query.isBlank()) return@Button
                loading = true
                error = null
                definition = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { CambridgeDictionary.lookup(query.trim()) }
                    loading = false
                    result.onSuccess { definition = it }
                    result.onFailure { error = it.message ?: "Lookup failed" }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Search")
        }

        if (loading) {
            Text("Searching Cambridge Dictionary...")
        }

        if (error != null) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
        }

        definition?.let {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = query.trim(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = it)
                    Button(onClick = { onSave(query.trim(), it) }) {
                        Text("Add as Flashcard")
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardsScreen(
    flashcards: List<Flashcard>,
    onDelete: (Flashcard) -> Unit,
    onExport: () -> Unit
) {
    var selected by remember { mutableStateOf<Flashcard?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (BuildConfig.EXPORT_ENABLED) {
            Button(onClick = onExport) {
                Text("Export for Anki")
            }
        }

        if (flashcards.isEmpty()) {
            Text("No flashcards yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(flashcards, key = { it.id }) { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = card },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = card.word,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    selected?.let { card ->
        Dialog(onDismissRequest = { selected = null }) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(card.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(card.definition)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { selected = null }) { Text("Back") }
                        Button(onClick = {
                            onDelete(card)
                            selected = null
                        }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

object CambridgeDictionary {
    fun lookup(word: String): Result<String> {
        return runCatching {
            val encoded = URLEncoder.encode(word, StandardCharsets.UTF_8.toString())
            val url = "https://dictionary.cambridge.org/dictionary/english/$encoded"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get()

            val candidate = doc.select("span.def.ddef_d.db").firstOrNull()?.text()?.trim()
            when {
                candidate.isNullOrBlank() -> throw IllegalStateException("No definition found for '$word'.")
                else -> candidate
            }
        }
    }
}

class FlashcardRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("flashcards", Context.MODE_PRIVATE)

    fun getAll(): List<Flashcard> {
        val raw = prefs.getString("data", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size < 3) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                Flashcard(
                    id = id,
                    word = unescape(parts[1]),
                    definition = unescape(parts.subList(2, parts.size).joinToString("\t"))
                )
            }
            .sortedByDescending { it.id }
    }

    fun add(word: String, definition: String) {
        val current = getAll().toMutableList()
        val cleanWord = word.trim()
        val cleanDef = definition.trim()
        val existingIndex = current.indexOfFirst { it.word.equals(cleanWord, ignoreCase = true) }
        val newItem = Flashcard(System.currentTimeMillis(), cleanWord, cleanDef)

        if (existingIndex >= 0) {
            current[existingIndex] = newItem
        } else {
            current.add(0, newItem)
        }
        saveAll(current)
    }

    fun delete(id: Long) {
        val next = getAll().filterNot { it.id == id }
        saveAll(next)
    }

    private fun saveAll(items: List<Flashcard>) {
        val payload = items.joinToString("\n") {
            "${it.id}\t${escape(it.word)}\t${escape(it.definition)}"
        }
        prefs.edit().putString("data", payload).apply()
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\t", "\\t")

    private fun unescape(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                val next = value[i + 1]
                when (next) {
                    'n' -> {
                        sb.append('\n')
                        i += 2
                        continue
                    }

                    't' -> {
                        sb.append('\t')
                        i += 2
                        continue
                    }

                    '\\' -> {
                        sb.append('\\')
                        i += 2
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}

fun exportFlashcards(context: Context, flashcards: List<Flashcard>): Boolean {
    if (flashcards.isEmpty()) return false
    val fileName = "starlearn_flashcards.tsv"
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "text/tab-separated-values")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false

    return runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                flashcards.forEach { card ->
                    val sanitizedWord = card.word.replace("\t", " ").replace("\n", " ").trim()
                    val sanitizedDefinition = card.definition.replace("\t", " ").replace("\n", " ").trim()
                    writer.append(sanitizedWord)
                        .append('\t')
                        .append(sanitizedDefinition)
                        .append('\n')
                }
            }
        } ?: throw IllegalStateException("Failed to open export stream")

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        true
    }.getOrElse {
        false
    }
}
