package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

private val midnight = Color(0xFF07111F)
private val cyan = Color(0xFF52D8F5)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { JarvisScreen(::speak) }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.UK
    }
    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply") }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
}

data class ChatMessage(val text: String, val fromUser: Boolean)

@Composable
fun JarvisScreen(speak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var messages by remember {
        mutableStateOf(listOf(ChatMessage("Good evening. I am JARVIS, your personal assistant. How may I assist you?", false)))
    }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening(context, speechRecognizer) { result -> input = result; listening = false }
    }

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty() || busy) return
        messages = messages + ChatMessage(question, true)
        input = ""
        busy = true
        scope.launch {
            val answer = withContext(Dispatchers.IO) { JarvisApi.ask(question) }
            messages = messages + ChatMessage(answer, false)
            busy = false
            speak(answer)
        }
    }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        onDispose { speechRecognizer?.destroy() }
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10263C), midnight)))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("J.A.R.V.I.S", color = cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("PERSONAL AI SYSTEM", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Surface(color = Color(0xFF12384A), shape = CircleShape) {
                Text("ONLINE", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = cyan, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(135.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(118.dp).background(Color(0xFF103B50), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(88.dp).background(Color(0xFF17627A), CircleShape), contentAlignment = Alignment.Center) {
                    Text("J", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Light)
                }
            }
        }
        Text(if (listening) "LISTENING..." else if (busy) "PROCESSING..." else "READY FOR YOUR COMMAND",
            Modifier.fillMaxWidth(), color = cyan, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (message.fromUser) Color(0xFF16465A) else Color(0xFF172638),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Text(message.text, Modifier.padding(13.dp), color = Color.White, fontSize = 15.sp)
                    }
                }
            }
            if (busy) item { Text("JARVIS is thinking…", color = cyan, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Message JARVIS…", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = cyan, unfocusedBorderColor = Color(0xFF456174),
                    cursorColor = cyan
                ), shape = RoundedCornerShape(18.dp), maxLines = 3
            )
            Spacer(Modifier.width(7.dp))
            IconButton(onClick = {
                when {
                    listening -> { speechRecognizer?.stopListening(); listening = false }
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                        startListening(context, speechRecognizer) { input = it; listening = false }.also { listening = true }
                    else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Icon(Icons.Default.Mic, contentDescription = "Speak", tint = if (listening) Color.Red else cyan)
            }
            IconButton(onClick = { sendMessage() }, enabled = !busy && input.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = cyan)
            }
        }
        Text("Voice recognition uses your device's speech service.", color = Color.Gray, fontSize = 10.sp)
    }
}

private fun startListening(
    context: android.content.Context,
    recognizer: SpeechRecognizer?,
    onResult: (String) -> Unit
) {
    if (recognizer == null) return
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {}
        override fun onResults(results: Bundle?) {
            onResult(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
    }
    recognizer.startListening(intent)
}

object JarvisApi {
    // Replace with your deployed backend URL. Never put an OpenAI API key in this Android app.
    private const val BASE_URL = "https://YOUR_BACKEND_HOST"
    private val client = OkHttpClient()
    fun ask(message: String): String {
        if (BASE_URL.contains("YOUR_BACKEND_HOST")) {
            return "My interface is ready, but my AI connection is not configured yet. Add your backend URL in JarvisApi.kt to activate my intelligence."
        }
        return try {
            val json = JSONObject().put("message", message)
            val request = Request.Builder().url("$BASE_URL/chat")
                .post(json.toString().toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "I could not reach my AI service. Please try again."
                JSONObject(response.body?.string().orEmpty()).optString("reply", "I received an empty response.")
            }
        } catch (_: Exception) {
            "I am unable to connect at the moment. Please check your internet connection and backend."
        }
    }
}
