package com.example.catetduls.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.catetduls.di.NetworkModule
import com.example.catetduls.ui.viewmodel.ApiTestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestPage() {
    val apiService = NetworkModule.apiService
    val viewModel: ApiTestViewModel = viewModel(
        factory = ApiTestViewModel.Factory(apiService)
    )

    var selectedMethod by remember { mutableStateOf("GET") }
    var endpoint by remember { mutableStateOf("") }
    var jsonBody by remember { mutableStateOf("") }
    var customToken by remember { mutableStateOf("") }
    var useCustomAuth by remember { mutableStateOf(false) }

    val result by viewModel.apiResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Tester") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ============ QUICK TESTS ============
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Quick Tests",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                endpoint = "ping"
                                selectedMethod = "GET"
                                jsonBody = ""
                                viewModel.testApi("GET", "ping")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ping")
                        }

                        OutlinedButton(
                            onClick = {
                                endpoint = "categories"
                                selectedMethod = "GET"
                                jsonBody = ""
                                viewModel.testApi(
                                    "GET",
                                    "categories",
                                    customToken = if (useCustomAuth) customToken else null
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Categories")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                endpoint = "auth/me"
                                selectedMethod = "GET"
                                jsonBody = ""
                                viewModel.testApi(
                                    "GET",
                                    "auth/me",
                                    customToken = if (useCustomAuth) customToken else null
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Auth Me")
                        }

                        OutlinedButton(
                            onClick = {
                                endpoint = "books"
                                selectedMethod = "GET"
                                jsonBody = ""
                                viewModel.testApi(
                                    "GET",
                                    "books",
                                    customToken = if (useCustomAuth) customToken else null
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Books")
                        }
                    }
                }
            }

            HorizontalDivider()

            // ============ CUSTOM REQUEST ============
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔧 Custom Request",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // HTTP Method Selector
                    Text("HTTP Method:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GET", "POST", "PUT", "DELETE").forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Endpoint Input
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Endpoint") },
                        placeholder = { Text("e.g., categories, auth/login, books/1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
//                        leadingIcon = {
//                            Icon(Icons.Default.Language, contentDescription = null)
//                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // JSON Body (untuk POST/PUT)
                    if (selectedMethod == "POST" || selectedMethod == "PUT") {
                        OutlinedTextField(
                            value = jsonBody,
                            onValueChange = { jsonBody = it },
                            label = { Text("Request Body (JSON)") },
                            placeholder = { Text("{\n  \"key\": \"value\"\n}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick JSON Templates
                        Text("Templates:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    jsonBody = """
                                        {
                                          "email": "test@example.com",
                                          "password": "password123"
                                        }
                                    """.trimIndent()
                                }
                            ) {
                                Text("Login", style = MaterialTheme.typography.labelSmall)
                            }

                            TextButton(
                                onClick = {
                                    jsonBody = """
                                        {
                                          "name": "Test User",
                                          "email": "test@example.com",
                                          "password": "password123",
                                          "password_confirmation": "password123"
                                        }
                                    """.trimIndent()
                                }
                            ) {
                                Text("Register", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Custom Auth Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useCustomAuth,
                            onCheckedChange = { useCustomAuth = it }
                        )
                        Text("Use Custom Token")
                    }

                    // Custom Token Input
                    if (useCustomAuth) {
                        OutlinedTextField(
                            value = customToken,
                            onValueChange = { customToken = it },
                            label = { Text("Bearer Token") },
                            placeholder = { Text("Paste your token here") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Send Button
                    Button(
                        onClick = {
                            if (endpoint.isNotBlank()) {
                                viewModel.testApi(
                                    method = selectedMethod,
                                    endpoint = endpoint,
                                    jsonBody = if (jsonBody.isNotBlank()) jsonBody else null,
                                    customToken = if (useCustomAuth && customToken.isNotBlank()) customToken else null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && endpoint.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Request")
                        }
                    }
                }
            }

            HorizontalDivider()

            // ============ RESULT ============
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Result",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (!isLoading) {
                            IconButton(
                                onClick = { /* TODO: Copy to clipboard */ }
                            ) {
//                                Icon(Icons.Default.Description, contentDescription = "Copy")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}