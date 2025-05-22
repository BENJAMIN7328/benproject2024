package com.example.benproject.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.benproject.data.MainViewModel
import com.example.benproject.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(navController: NavHostController, viewModel: MainViewModel = viewModel()) {
    val products = viewModel.products.collectAsState().value // ✅ Ensure StateFlow is properly observed

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Product List", style = MaterialTheme.typography.headlineLarge)
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (products.isEmpty()) {
                    item {
                        Text(
                            text = "No products available.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(products) { product ->
                        ProductCard(product, viewModel, navController) // ✅ Correct reference
                    }
                }
            }
        }
    }
}


@Composable
fun ProductCard(product: Product, viewModel: MainViewModel, navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            Text(text = product.name, style = MaterialTheme.typography.titleLarge)
            Text(text = product.description, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$${product.price}", style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { viewModel.deleteProduct(product.id) }) {
                    Text("Delete")
                }
                Button(onClick = { navController.navigate("update_product/${product.id}") }) {
                    Text("Update")
                }
            }
        }
    }
}
