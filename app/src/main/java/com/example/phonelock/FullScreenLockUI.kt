//package com.example.phonelock
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.imePadding
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.Button
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun FullscreenLockUI(onCorrectCode: () -> Unit) {
//    var input by remember { mutableStateOf("") }
//    val context = LocalContext.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .imePadding()
//            .padding(32.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            OutlinedTextField(
//                value = input,
//                onValueChange = { input = it },
//                label = { Text("Enter Code") },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                singleLine = true
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = {
//                if (input == "123456") {
//                    onCorrectCode()
//                } else {
//                    Toast.makeText(context, "Incorrect Code", Toast.LENGTH_SHORT).show()
//                }
//            }) {
//                Text("Submit", fontSize = 18.sp)
//            }
//        }
//    }
//}
