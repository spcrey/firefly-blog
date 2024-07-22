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
import retrofit2.http.Query
import java.time.LocalDateTime
import java.time.LocalTime
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

        @Headers("content-type: application/json")
        @POST("/user/register")
        fun userRegister(@Body form: UserRegisterForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/logout")
        fun userLogout(@Header("Authorization") token: String): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/update")
        fun userUpdate(@Header("Authorization") token: String, @Body form: UserUpdateForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/article/list")
        fun articleList(@Header("Authorization") token: String="", @Body form: ArticleListForm): Deferred<CommonData<ArticleList>>

        @Headers("content-type: application/json")
        @POST("/article/like")
        fun articleLike(@Header("Authorization") token: String, @Body form: ArticleLikeForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/article/unlike")
        fun articleUnlike(@Header("Authorization") token: String, @Body form: ArticleLikeForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/updateAvatar")
        fun userUpdateAvatar(@Header("Authorization") token: String, @Body form: UserUpdateAvatarForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/article/add")
        fun articleAdd(@Header("Authorization") token: String, @Body form: ArticleAddForm): Deferred<CommonData<String>>
    }

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    data class ArticleAddForm(val content: String, val imageUrls: MutableList<String>)

    data class CommonData<T>(val code: Int, val message: String, val data: T)

    data class UserSendSmsForm(val phoneNumber: String)

    data class UserLoginByCodeForm(val phoneNumber: String, val code: String)

    data class UserUpdateAvatarForm(val avatarUrl: String)

    data class UserLoginByPasswordForm(val phoneNumber: String, val password: String)

    data class UserRegisterForm(val phoneNumber: String, val password: String, val rePassword: String)

    data class UserUpdateForm(var nickname: String?=null, var email: String?=null, var personalSignature: String?=null)

    data class ArticleListForm(val pageSize: Int, val pageNum: Int)

    data class ArticleLikeForm(val id: Int)

    data class Article(
        val id: Int, val content: String, val userID: Int, val createTime: String,
        val imageUrls: List<String>,
        val userNickname: String, val userAvatarUrl: String,
        var likeCount: Int, var likeStatus: Boolean?, val commentCount: Int
    )

    data class ArticleList(val total: Int, val items: List<Article>)

    data class UserInfo(
        val id: Int, val phoneNumber: String?,
        var nickname: String?, var email: String?, var personalSignature: String?,
        val avatarUrl: String?,
        val isFollowed: Boolean?, val isFollower: Boolean?,
        val createTime: String, val updateTime: String
    )
}