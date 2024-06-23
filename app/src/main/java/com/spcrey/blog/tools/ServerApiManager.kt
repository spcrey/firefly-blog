package com.spcrey.blog.tools

import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object ServerApiManager {
    private const val TAG = "ServerApiManager"

    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:9000")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build();

    interface ApiService {
        @Headers("content-type: application/json")
        @GET("/")
        fun firefly(): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/register")
        fun register(@Body registerBody: RegisterBody): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/login")
        fun login(@Body loginBody: LoginBody): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @GET("/user/info")
        fun info(@Header("Authorization") token: String): Deferred<CommonData<UserInfo>>
    }

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    data class CommonData<T>(val code: Int, val message: String, val data: T)

    data class RegisterBody(val username: String, val password: String, val rePassword: String)
    data class LoginBody(val username: String, val password: String)
    data class UserInfo(
        val id: Int, val username: String, val nickname: String, val email: String, val userPic: String,
        val createTime: String, val updateTime: String
    )

    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello World!")
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbXMiOnsiaWQiOjEsInVzZXJuYW1lIjoic3BjcmV5In0sImV4cCI6MTcxOTA3MzQ5N30.F-XKud8M7DVARBYnj2LgGpsB2mR0rUtcKAQIuD3k8Wc"

        runBlocking {
            try {
                val requestBody = LoginBody("spcrey", "crey199854")
                val data = apiService.info("").await()
                println(data)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}