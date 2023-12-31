package com.requeique.agaz


import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


class GoogleTasksManager(activity: Activity, context: Context) {
//    private val service: Tasks
//
//    init {
//        val accountManager = AccountManager.get(activity)
//        val accounts = accountManager.getAccountsByType("com.google")
//        val token = AccountManager.get(context)
//            .getAuthToken(android.accounts.Account(""), "oauth2:" + TasksScopes.TASKS, Bundle(), activity, null, null)
//        .result
//            .getString(AccountManager.KEY_AUTHTOKEN);
//        val credential = GoogleCredential().setAccessToken(token);
//        var transport = NetHttpTransport()
//        val jsonFactory = JacksonFactory()
//        val scopes = listOf(TasksScopes.TASKS)
//
//
//        service = Tasks.Builder(transport, jsonFactory, credentials)
//            .setApplicationName("Google Tasks API Kotlin Quickstart")
//            .build()
//    }
//
//    @Throws(IOException::class)
//    fun createTask(taskTitle: String) {
//        val task = Task()
//            .setTitle(taskTitle)
//            .setNotes("Created with the Google Tasks API Kotlin Quickstart")
//
//        val createdTask = service.tasks().insert("default", task).execute()
//
//        println("Task created: ${createdTask.title}")
//    }
}

@Composable
fun rememberAuthLauncher(
    onAuthComplete: (AuthResult) -> Unit, cred: (GoogleCredential) -> Unit, context: Context,
    onAuthError: (ApiException) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val scope = rememberCoroutineScope()


    return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!

            Log.d("99", "========== ${account.account.toString()}")
            scope.launch(IO) {


                val token = AccountManager.get(context)
                    .getAuthToken(
                        account.account,
                        "oauth2:" + TasksScopes.TASKS,
                        Bundle(),
                        context as Activity,
                        {
                            Log.d("99", "===== (${it.result.toString()}")
                        },
                        null
                    )
                    .result.apply { Log.d("99", "======== ${this.toString()}") }
                    .getString(AccountManager.KEY_AUTHTOKEN)

                cred(GoogleCredential().setAccessToken(token))

                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)


                val authResult = Firebase.auth.signInWithCredential(credential).await()
                onAuthComplete(authResult)


            }
        } catch (e: ApiException) {
            onAuthError(e)
        }
    }
}

@Preview
@Composable
fun Test() {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    var credential by remember { mutableStateOf<GoogleCredential?>(null) }
    val gso =

        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("772791080245-m41kb684oigd2pa0rkqan8fq5idovr9l.apps.googleusercontent.com")
            .requestEmail()
            .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)
    val launcher = rememberAuthLauncher(onAuthComplete = {


    }, onAuthError = {

    }, cred = {
        credential = it
        Log.d("99", "$$$$$$$$ ${it.toString()}")
    }, context = context)

    LaunchedEffect(false) {
        launcher.launch(googleSignInClient.signInIntent)
    }
    Button(onClick = {

        coroutine.launch (IO){

            val transport = NetHttpTransport()
            val jsonFactory = JacksonFactory()
            Log.d("99", "1========== $credential")
            val service = Tasks.Builder(transport, jsonFactory, credential)
                .setApplicationName("Google Tasks API Kotlin Quickstart")
                .build()
            createTask(taskTitle = "test", service = service)
        }

//

    }) {

    }


}

suspend fun createTask( taskTitle: String, service: Tasks, due: DateTime? = null) = withContext(IO){
    val taskLists = service.tasklists().list()
          .execute()
    val taskList = taskLists.items[0]
    val task = Task()
        .setTitle(taskTitle)
        .setNotes("Generated From Agaz")
        .setDue(due)


    Log.d("999",service.tasklists().list().execute().map { it.key to it.value }.toString())
    val createdTask = service.tasks().insert(taskList.id, task).execute()


}


suspend fun listTaskList(service: Tasks): List<TaskList> = withContext(IO){
    val x = service.tasklists().list().execute().items

    return@withContext x
}


