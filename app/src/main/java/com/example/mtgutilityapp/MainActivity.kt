package com.example.mtgutilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mtgutilityapp.data.local.AppDatabase
import com.example.mtgutilityapp.data.remote.ScryfallApi
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.ui.navigation.NavGraph
import com.example.mtgutilityapp.ui.theme.MTGUtilityAppTheme
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var repository: CardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database
        val database = AppDatabase.getDatabase(applicationContext)
        val cardDao = database.cardDao()

        // Initialize Retrofit
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ScryfallApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val scryfallApi = retrofit.create(ScryfallApi::class.java)

        // Initialize repository
        repository = CardRepository(cardDao, scryfallApi)

        setContent {
            MTGUtilityAppTheme {
                NavGraph(repository = repository)
            }
        }
    }
}