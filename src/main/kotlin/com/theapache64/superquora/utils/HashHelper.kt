package com.theapache64.superquora.utils

object HashHelper {
    fun getHashFrom(response: String): String {
        val hashRegEx = "name:\"QuestionAnswerPagedListQuery\".+?,id:\"(?<hash>.+?)\",".toRegex()
        val matchResult = hashRegEx.find(response)!!
        return matchResult.groupValues[1]
    }
}