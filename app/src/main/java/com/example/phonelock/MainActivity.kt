package com.example.phonelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen {
                    startActivity(Intent(this, LockScreenActivity::class.java))
                }
            }
        }
    }
}

@Composable
fun MainScreen(onLockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This is a test screen.\nOpen any app and test abusive word detection.")
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onLockClick) {
                Text("Lock Screen")
            }
        }
    }
}
