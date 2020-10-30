package com.theapache64.superquora.utils

import kotlin.js.Json

class HtmlAnalyzer(
    private val inputHtml: String
) {
    fun getFormKey(): String {
        val formKeyRegEx = "\"formkey\": \"(?<formKey>.+?)\"".toRegex()
        return formKeyRegEx.find(inputHtml)!!.groupValues[1]
    }

    fun getQuestionId(isQuestion: Boolean): String {
        val qIdRegEx = if (isQuestion) {
            "\"pageOid\": (?<qId>\\d+)".toRegex()
        } else {
            "\\\\\"qid\\\\\":(?<qId>\\d+)".toRegex()
        }
        return qIdRegEx.find(inputHtml)!!.groupValues[1]
    }

    fun getHashUrl(): String {
        val x1 = inputHtml.split("ansFrontendRelayWebpackManifest = ")
        val x2 = x1[1].split("\"};")[0] + "\"}"
        val j1 = JSON.parse<Json>(x2)
        return j1["page-QuestionPageLoadable"].toString()
    }
}