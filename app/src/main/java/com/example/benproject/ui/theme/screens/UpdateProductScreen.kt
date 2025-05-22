package com.example.benproject.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benproject.data.MainViewModel
import com.example.benproject.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProductScreen(viewModel: MainViewModel, product: Product) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var price by remember { mutableStateOf(product.price.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        TextField(value = price, onValueChange = { price = it }, label = { Text("Price") })

        Button(onClick = {
            viewModel.updateProduct(product.id, name, description, price.toDouble(), product.imageUrl)
        }) {
            Text("Update Product")
        }
    }
}
