package com.requeique.agaz


import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.requeique.agaz.ui.theme.AgazTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.ParseException
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            AgazTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var dialogState by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val coroutine = rememberCoroutineScope()
                    var credential by remember { mutableStateOf<GoogleCredential?>(null) }
                    val gso =

                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("")//Server Client ID
                            .requestEmail()
                            .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    val launcher = rememberAuthLauncher(onAuthComplete = {


                    }, onAuthError = {

                    }, cred = {
                        credential = it

                    }, context = context)
                    val transport = NetHttpTransport()
                    val jsonFactory = JacksonFactory()


                    LaunchedEffect(false) {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                    val service = Tasks.Builder(transport, jsonFactory, credential)
                        .setApplicationName("Google Tasks API Kotlin Quickstart")
                        .build()

                    MainContent(onFabClicked = {


                        coroutine.launch(Dispatchers.IO) {


                            it.flatMap { it.lines }.drop(1).forEach {

                                createTask(taskTitle = it.text, service = service)
                            }

                        }
                    })


                }
            }
        }
    }
}

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MainContent(onFabClicked: (List<TextBlock>) -> Unit = {}) {
    var result: List<TextBlock> by remember() { mutableStateOf(listOf()) }
    var isButtonActivated by remember { mutableStateOf(false) }
    Toast.makeText(
        LocalContext.current,
        result.map { it.lines.map { it.text } }.toString(),
        Toast.LENGTH_SHORT
    ).show()

    Scaffold(floatingActionButton = {
        CompositionLocalProvider(
            LocalRippleTheme provides
                    if (isButtonActivated) LocalRippleTheme.current else NoRippleTheme
        ) {

            ExtendedFloatingActionButton(
                text = { Text("Submit") },
                icon = { Icon(Icons.Outlined.Check, null) },
                onClick = {

                    if (isButtonActivated) {

                        onFabClicked(result)
                        isButtonActivated = false
                    }
                },
                containerColor = if (isButtonActivated) MaterialTheme.colorScheme.secondary else Gray
            )
        }
    }, topBar = {
        TopAppBar(title = { Text("Agaz") })
    }) {
        Column(Modifier.padding(top = 18.dp)) {
            Card(Modifier.weight(0.5f)) {
                PreviewViewComposable(modifier = Modifier.weight(0.5f), result = {

                    result = it
                    isButtonActivated = true
                })
            }
            Card(
                Modifier
                    .weight(0.5f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(18.dp),
                    content = {
                        itemsIndexed(result.flatMap { it.lines }) { i, it ->
                            when (i) {
                                0 -> {
                                    val date = try {
                                        val dateFormat =
                                            SimpleDateFormat("MM-dd-yy", Locale.US)
                                        val _date =
                                            dateFormat.parse(it.text.map { if (it == ' ') "" else it }
                                                .joinToString(""))
                                        val myFormatObj =
                                            SimpleDateFormat("E, MMM dd yyyy", Locale.US)


                                        myFormatObj.format(_date)
//                                                _date.toString()

                                    } catch (e: ParseException) {
                                        null
                                    }
                                    if (date != null)
                                        Text(
                                            date,
                                            style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                }

                                1 -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .padding(top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Row {
                                                Text(
                                                    text = "\u2022",
                                                    modifier = Modifier.width(20.dp)
                                                )
                                                Text(
                                                    it.text,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                }

                                result.flatMap { it.lines }.size - 1 -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 18.dp,
                                            bottomEnd = 18.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .padding(top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Row {
                                                Text(
                                                    text = "\u2022",
                                                    modifier = Modifier.width(20.dp)
                                                )
                                                Text(
                                                    it.text,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .padding(top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Row {
                                                Text(
                                                    text = "\u2022",
                                                    modifier = Modifier.width(20.dp)
                                                )
                                                Text(
                                                    it.text,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        it
    }
}

@Composable
private fun PreviewViewComposable(
    result: (List<TextBlock>) -> Unit,
    modifier: Modifier = Modifier
) {

    AndroidView(modifier = modifier.fillMaxWidth(), factory = { context: Context ->

        val cameraExecutor = Executors.newSingleThreadExecutor()
        val previewView = PreviewView(context).also {
            it.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)


        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageCapture = ImageCapture.Builder().build()

            @ExperimentalGetImage

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, OCR { str ->

                        result(str)

                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            cameraProvider.unbindAll()

            // Bind use cases to camera

            cameraProvider.bindToLifecycle(
                context as ComponentActivity, cameraSelector, preview, imageCapture, imageAnalyzer
            )


        }, ContextCompat.getMainExecutor(context))
        previewView

    })
}

@ExperimentalGetImage
private class OCR(
    val callback: (List<TextBlock>) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {


        val scanner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val mediaImage = imageProxy.image!!
        mediaImage.let {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { text ->
                    if (text.text.isNotEmpty()) {
                        callback(text.textBlocks)
                    }
                }
                .addOnFailureListener {

                }
        }
        imageProxy.close()
    }
}
