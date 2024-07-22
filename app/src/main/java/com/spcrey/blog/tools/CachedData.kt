package com.spcrey.blog.tools

object CachedData {
    var token: String? = null
    var userInfo: ServerApiManager.UserInfo? = null
    val articles: MutableList<ServerApiManager.Article> = mutableListOf()
    var currentPageNum = 1
}