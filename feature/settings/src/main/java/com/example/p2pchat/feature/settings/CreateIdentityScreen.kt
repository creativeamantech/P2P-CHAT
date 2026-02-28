package com.example.p2pchat.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateIdentityRoute(
    viewModel: CreateIdentityViewModel = hiltViewModel(),
    onIdentityCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CreateIdentityUiState.Success) {
            onIdentityCreated()
        }
    }

    CreateIdentityScreen(
        uiState = uiState,
        onCreateIdentity = viewModel::onCreateIdentity
    )
}

@Composable
fun CreateIdentityScreen(
    uiState: CreateIdentityUiState,
    onCreateIdentity: (String, String) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var identityType by remember { mutableStateOf("PERMANENT") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to P2P Chat",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Create your identity to start chatting securely.")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            singleLine = true,
            enabled = uiState !is CreateIdentityUiState.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Identity Type:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = identityType == "PERMANENT",
                onClick = { identityType = "PERMANENT" }
            )
            Text("Permanent (Trusted)")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = identityType == "BURNER",
                onClick = { identityType = "BURNER" }
            )
            Text("Burner (Expires in 24h)")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = identityType == "CONTEXTUAL",
                onClick = { identityType = "CONTEXTUAL" }
            )
            Text("Contextual (e.g. Work)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState is CreateIdentityUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState is CreateIdentityUiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { onCreateIdentity(displayName, identityType) },
                enabled = displayName.isNotBlank()
            ) {
                Text("Create Identity")
            }
        }
    }
}
