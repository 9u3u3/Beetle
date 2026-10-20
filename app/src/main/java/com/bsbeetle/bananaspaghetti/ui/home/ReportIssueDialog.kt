package com.bsbeetle.bananaspaghetti.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Report Issue Dialog with email, description, and image picker.
 *
 * The email is pre-filled from the last one entered and stays editable — there is no sign-in
 * flow that could supply it automatically.
 */
@Composable
fun ReportIssueDialog(
    userEmail: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf(userEmail) }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Issue",
                        fontFamily = KarmaticFont,
                        fontSize = 24.sp,
                        color = Color(0xFFFF4444)
                    )
                    
                    IconButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        enabled = !isSubmitting
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Success message
                if (submitSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Report submitted successfully!\nThank you for your feedback.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4EC9B0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", color = Color.Black)
                    }
                    return@Column
                }
                
                // Email field
                Text(
                    text = "Email",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    enabled = !isSubmitting,
                    placeholder = { Text("you@example.com", color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFDDDDDD),
                        unfocusedTextColor = Color(0xFFDDDDDD),
                        focusedBorderColor = Color(0xFF5C5C5C),
                        unfocusedBorderColor = Color(0xFF3C3C3C),
                        focusedContainerColor = Color(0xFF2A2A2A),
                        unfocusedContainerColor = Color(0xFF2A2A2A),
                        disabledTextColor = Color(0xFF888888),
                        disabledBorderColor = Color(0xFF3C3C3C),
                        disabledContainerColor = Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description field
                Text(
                    text = "Description",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF4444),
                        unfocusedBorderColor = Color(0xFF3C3C3C),
                        focusedContainerColor = Color(0xFF2A2A2A),
                        unfocusedContainerColor = Color(0xFF2A2A2A),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("Describe the issue...", color = Color(0xFF666666)) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image picker
                Text(
                    text = "Screenshot",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                if (selectedImageUri != null) {
                    // Image preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        val bitmap = remember(selectedImageUri) {
                            selectedImageUri?.let { uri ->
                                try {
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        BitmapFactory.decodeStream(input)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3C3C3C), RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        // Remove button
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color(0xFFFF4444), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Image picker button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color(0xFF3C3C3C), RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = "Add image",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to select screenshot",
                                color = Color(0xFF666666),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Error message
                if (submitError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = submitError!!,
                        color = Color(0xFFFF4444),
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Submit button
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            submitError = "Please enter your email"
                            return@Button
                        }
                        if (selectedImageUri == null) {
                            submitError = "Please select a screenshot"
                            return@Button
                        }
                        onEmailChange(email)
                        
                        scope.launch {
                            isSubmitting = true
                            submitError = null
                            
                            try {
                                val imageBytes = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                                        input.readBytes()
                                    }
                                }
                                
                                if (imageBytes == null) {
                                    submitError = "Failed to read image"
                                    isSubmitting = false
                                    return@launch
                                }
                                
                                val client = HttpClient(OkHttp)
                                
                                val response = withContext(Dispatchers.IO) {
                                    client.submitFormWithBinaryData(
                                        url = "https://beetle-backend.vercel.app/api/reports/submit",
                                        formData = formData {
                                            append("email", email)
                                            append("description", description)
                                            append("image", imageBytes, Headers.build {
                                                append(HttpHeaders.ContentType, "image/png")
                                                append(HttpHeaders.ContentDisposition, "filename=\"screenshot.png\"")
                                            })
                                        }
                                    )
                                }
                                
                                client.close()
                                
                                if (response.status.value in 200..299) {
                                    submitSuccess = true
                                } else {
                                    val errorBody = response.bodyAsText()
                                    submitError = "Failed to submit: $errorBody"
                                }
                                
                            } catch (e: Exception) {
                                submitError = "Error: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444),
                        disabledContainerColor = Color(0xFF666666)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSubmitting) "Submitting..." else "Submit Report",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
