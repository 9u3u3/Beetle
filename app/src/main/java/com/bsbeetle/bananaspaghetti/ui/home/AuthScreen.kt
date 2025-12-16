package com.bsbeetle.bananaspaghetti.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalAuthViewModel
import com.bsbeetle.bananaspaghetti.ui.components.GridBackground
import com.bsbeetle.bananaspaghetti.ui.components.LiquidGlassBox
import com.bsbeetle.bananaspaghetti.ui.components.VignetteEffect
import com.bsbeetle.bananaspaghetti.ui.components.beetleAscii
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.R

@Composable
fun AuthScreen(navController: NavController) {
    val authViewModel = LocalAuthViewModel.current
    val isAuthorized by authViewModel.isAuthorized.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val authError by authViewModel.authError.collectAsStateWithLifecycle()
    
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        // Background Elements
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (gridLayout, beetleAsciiLeft, beetleAsciiRight, vignette) = createRefs()

            GridBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(gridLayout) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )

            beetleAscii(
                modifier = Modifier
                    .size(400.dp)
                    .constrainAs(beetleAsciiLeft) {
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                    }
                    .alpha(0.6f)
            )

            beetleAscii(
                modifier = Modifier
                    .size(400.dp)
                    .constrainAs(beetleAsciiRight) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    }
                    .rotate(180f)
                    .alpha(0.6f)
            )

            VignetteEffect(
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(vignette) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                intensity = 1f
            )
        }

        // Auth Card - Placed on top via Box layering
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .wrapContentHeight()
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0x80FFFFFF),
                            Color(0x40FFFFFF),
                            Color(0x80FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with logo and title (like MainMenu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.beetle_logo),
                        contentDescription = "Beetle logo",
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Beetle",
                        fontFamily = KarmaticFont,
                        color = Color.White,
                        fontSize = 36.sp,
                        style = TextStyle(shadow = Shadow(color = Color(0xFFFFFFFF), offset = Offset(0f, 0f), blurRadius = 15f))
                    )
                }

                Text(
                    text = "Enter your email to access",
                    fontFamily = born2bsporty,
                    color = Color(0xFFAAAAAA),
                    fontSize = 18.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (email.isNotBlank()) {
                                authViewModel.checkWaitlist(email)
                            }
                        }
                    )
                )

                Button(
                    onClick = { authViewModel.checkWaitlist(email) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "Check Access", 
                            fontFamily = born2bsporty,
                            fontSize = 16.sp
                        )
                    }
                }

                if (authError != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = authError!!,
                            color = Color(0xFFFF6B6B),
                            fontFamily = born2bsporty,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        if (authError == "Access Denied") {
                            Text(
                                text = "Get Access at beetleide.vercel.app",
                                color = Color.Cyan,
                                fontFamily = born2bsporty,
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://beetleide.vercel.app"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
