package com.spcrey.blog.tools

import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
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
        .baseUrl("http://120.26.13.9:9000")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build();

    interface ApiService {
        @Headers("content-type: application/json")
        @GET("/")
        fun firefly(): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/sendSms")
        fun userSendSms(@Body form: UserSendSmsForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/loginByCode")
        fun userLoginByCode(@Body form: UserLoginByCodeForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @GET("/user/info")
        fun userInfo(@Header("Authorization") token: String): Deferred<CommonData<UserInfo>>

        @Headers("content-type: application/json")
        @POST("/user/loginByPassword")
        fun userLoginByPassword(@Body form: UserLoginByPasswordForm): Deferred<CommonData<String>>
    }

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    data class CommonData<T>(val code: Int, val message: String, val data: T)

    data class UserSendSmsForm(val phoneNumber: String)

    data class UserLoginByCodeForm(val phoneNumber: String, val code: String)

    data class UserLoginByPasswordForm(val phoneNumber: String, val password: String)

    data class UserInfo(
        val id: Int, val phoneNumber: String?,
        val nickname: String?, val email: String?, val personalSignature: String?,
        val avatarUrl: String?,
        val isFollowed: Boolean?, val isFollower: Boolean?,
        val createTime: String, val updateTime: String
    )
}