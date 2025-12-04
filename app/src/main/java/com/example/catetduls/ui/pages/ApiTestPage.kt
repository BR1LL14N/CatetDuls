package com.example.catetduls.ui.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.ui.viewmodel.ApiTestViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestPage() {

    // Retrofit instance
    val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2/api/") // base wajib ada, tapi ignored karena pakai @Url
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)

    val viewModel: ApiTestViewModel = viewModel(
        factory = ApiTestViewModel.Factory(apiService)
    )


    var endpoint by remember { mutableStateOf("") }
    val result by viewModel.apiResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "API Tester",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Masukkan endpoint (contoh: categories)") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (endpoint.isNotBlank()) {
                    viewModel.runDynamicApiTest(endpoint)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tes Endpoint")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hasil:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = result,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }
    }
}
