package com.spcrey.blog.tools

import com.spcrey.blog.tools.ServerApiManager.UserMessageList

object CachedData {
    var token: String? = null
    var user: ServerApiManager.User? = null
    val articles: MutableList<ServerApiManager.Article> = mutableListOf()
    var currentPageNum = 1
    var multiUserMessageList = ServerApiManager.MultiUserMessageList(mutableListOf(), 0)

    @JvmStatic
    fun main(args: Array<String>) {
    }
}