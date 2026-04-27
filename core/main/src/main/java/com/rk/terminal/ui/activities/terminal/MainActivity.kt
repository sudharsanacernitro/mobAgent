package com.rk.terminal.ui.activities.terminal

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.rk.terminal.ui.screens.terminal.MkSession;

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay
import android.util.Log
import com.rk.TerminalLogger
import com.rk.terminal.setupAlpine
import kotlinx.coroutines.CoroutineScope


class MainActivity : ComponentActivity() {
    var isBound = false




    override fun onStart() {

//        super.onStart()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(Intent(this, SessionService::class.java))
//        }else{
//            startService(Intent(this, SessionService::class.java))
//        }
//        Intent(this, SessionService::class.java).also { intent ->
//            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
//        }
//
        super.onStart()

        TerminalLogger.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            setupAlpine(
                onProgress = { progress ->
                    println("Download progress: ${(progress * 100).toInt()}%")
                },
                onComplete = {
//                    // Files are ready, now create the headless terminal session
//                    val headlessSession = MkSession.createSession(this@MainActivity,"1",0);
//
//                    headlessSession.initializeEmulator();
//
//                    lifecycleScope.launch {
//                        delay(3000)
//                        val command = "java --version > output.txt\n".toByteArray()
//                        headlessSession.write(command, 0, command.size)
//                        Log.d("TAG", "Write executed after delay")
//                    }

                },
                onError = { e ->
                    println("Setup failed: ${e.message}")
                }
            )
        }



    }

    override fun onStop() {
        super.onStop()

    }


    private var denied = 1
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && denied <= 2) {
                denied++
                requestPermission()
            }
        }

    fun requestPermission(){
        // Only request on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var isKeyboardVisible = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermission()

        if (intent.hasExtra("awake_intent")){
            moveTaskToBack(true)
        }

    }



}