package com.theapache64.superquora.data.repo

import com.theapache64.superquora.data.remote.Answer
import com.theapache64.superquora.data.remote.AnswersResponse
import com.theapache64.superquora.models.Params
import com.theapache64.superquora.utils.AjaxHelper
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object AnswerRepo {

    private const val ANSWER_PER_REQUEST = 10
    private const val KEY_OPENED_ANSWERS = "opened_answers"

    fun getAnswers(
        pageNo: Int,
        params: Params,
        successCallback: (AnswersResponse) -> Unit,
        errorCallback: (String?) -> Unit
    ) {

        val after = (pageNo - 1) * ANSWER_PER_REQUEST

        // Let's do the final REST call
        val requestBody = """
            {
                "queryName": "QuestionAnswerPagedListQuery",
                "extensions": {
                    "hash": "${params.hash}"
                },
                "variables": {
                    "qid": ${params.questionId},
                    "first": $ANSWER_PER_REQUEST,
                    "after": "$after"
                }
            }
        """.trimIndent()

        AjaxHelper.post(
            "https://www.quora.com/graphql/gql_para_POST?q=QuestionAnswerPagedListQuery",
            requestBody,
            listOf(
                Pair("content-type", "application/json"),
                Pair("quora-formkey", params.formKey)
            ),
            onSuccess = { response ->
                val json = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<AnswersResponse>(response)

                successCallback(json)
            },
            onError = {
                errorCallback(it)
            }
        )
    }

    fun getOpenedAnswers(): Set<String> {
        return (window.localStorage.getItem(KEY_OPENED_ANSWERS) ?: "[]").let {
            Json.decodeFromString(it)
        }
    }

    fun appendToOpenedAnswers(randomAnswer: Answer) {
        // Add answer to read list
        val openedAnswers = getOpenedAnswers().toMutableSet()
        openedAnswers.add(randomAnswer.id)
        window.localStorage.setItem(KEY_OPENED_ANSWERS, JSON.stringify(openedAnswers))
    }

    fun removeFromOpenedAnswers(answerId: String) {
        val openedAnswers = getOpenedAnswers().toMutableSet()
        openedAnswers.remove(answerId)
        window.localStorage.setItem(KEY_OPENED_ANSWERS, JSON.stringify(openedAnswers))
    }
}