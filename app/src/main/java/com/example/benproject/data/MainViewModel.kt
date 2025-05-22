package com.example.benproject.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.benproject.network.ImgurApi
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val imgurService: ImgurApi = createImgurService()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<Product>>(emptyList())
    val cart = _cart.asStateFlow()


    fun fetchProducts() {
        firestore.collection("products").get()
            .addOnSuccessListener { snapshot ->
                val fetchedProducts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                _products.value = fetchedProducts
            }
            .addOnFailureListener { println("Error fetching products: ${it.message}") }
    }


    fun removeFromCart(product: Product) {
        _cart.value = _cart.value.filterNot { it.id == product.id }
    }

    fun initiatePayment(
        phoneNumber: String,
        amount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val paymentRequest = mapOf(
                    "phoneNumber" to phoneNumber,
                    "amount" to amount
                )

                val response = fakePaymentApiCall(paymentRequest)

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) { onError("Payment failed: ${response.message}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error: ${e.localizedMessage}") }
            }
        }
    }


    private fun fakePaymentApiCall(request: Map<String, Any>): ApiResponse {
        return ApiResponse(true, "Payment initiated successfully!")
    }

    data class ApiResponse(val isSuccessful: Boolean, val message: String)



    fun getTotalPrice(): Double {
        return _cart.value.sumOf { it.price }
    }


    fun updateProduct(productId: String, name: String, description: String, price: Double, imageUrl: String) {
        val updatedProduct = mapOf(
            "name" to name,
            "description" to description,
            "price" to price,
            "imageUrl" to imageUrl
        )

        firestore.collection("products").document(productId).update(updatedProduct)
            .addOnSuccessListener { fetchProducts() }
            .addOnFailureListener { it.printStackTrace() }
    }


    fun deleteProduct(productId: String) {
        firestore.collection("products").document(productId).delete()
            .addOnSuccessListener { fetchProducts() }
            .addOnFailureListener { it.printStackTrace() }
    }


    fun signOut(navController: NavHostController) {
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }


    fun uploadProductImageAndSave(
        uri: Uri,
        context: Context,
        name: String,
        description: String,
        price: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = getFileFromUri(context, uri)
            if (file == null) {
                withContext(Dispatchers.Main) { onError("Failed to process image") }
                return@launch
            }

            try {
                val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val response = imgurService.uploadImage("Client-ID d49f4b97e67b998", imagePart)

                if (response.isSuccessful && response.body()?.data?.link != null) {
                    val imageUrl = response.body()?.data?.link!!

                    val product = mapOf(
                        "name" to name,
                        "description" to description,
                        "price" to price,
                        "imageUrl" to imageUrl
                    )

                    firestore.collection("products").add(product)
                        .addOnSuccessListener { viewModelScope.launch(Dispatchers.Main) { onSuccess() } }
                        .addOnFailureListener { e -> viewModelScope.launch(Dispatchers.Main) { onError(e.message ?: "Firestore error") } }
                } else {
                    withContext(Dispatchers.Main) { onError("Imgur upload failed: ${response.message()}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Exception: ${e.localizedMessage}") }
            }
        }
    }


    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_image.jpg")
            file.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()
            file
        } catch (e: Exception) {
            println("Error processing image: ${e.localizedMessage}")
            null
        }
    }


    private fun createImgurService(): ImgurApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ImgurApi::class.java)
    }
}
