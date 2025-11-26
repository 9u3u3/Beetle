package com.bsbeetle.bananaspaghetti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList

//NO_PORTRAIT

@Composable
fun ZoomOut(navController: NavController) {

    val activeSession = SessionList.sessions[SessionList.activeSessionIndex!!]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D2D2D))
            .padding(16.dp)
    ) {

        // Sessions List
        SessionList.sessions.forEach { session ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color.Black, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable {
                        SessionList.activeSessionIndex = session.id
                        navController.navigate(Screen.Terminal.route) // We have to properly navigate through the stack
                    }
            ) {
                Text(
                    text = "Session: ${session}\nId: ${session.id}\nType: ${session.type}",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Session
        Text(
            text = "Active Session:\n" +
                    "Session: $activeSession\n" +
                    "Id: ${activeSession.id}\n" +
                    "Type: ${activeSession.type}",
            color = Color.White
        )
    }
}