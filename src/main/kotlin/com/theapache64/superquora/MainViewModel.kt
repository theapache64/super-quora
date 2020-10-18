package com.theapache64.superquora

import com.theapache64.superquora.data.remote.Answer
import com.theapache64.superquora.data.remote.AnswersResponse
import com.theapache64.superquora.models.Params
import com.theapache64.superquora.utils.AjaxHelper
import com.theapache64.superquora.utils.HashHelper
import com.theapache64.superquora.utils.HtmlAnalyzer
import org.w3c.xhr.XMLHttpRequest
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.*
import kotlinx.serialization.json.*

private const val KEY_READ_ANSWERS = "read_answers"
private lateinit var params: Params
private var fatalError: String? = null

fun main() {
    document.documentElement?.outerHTML?.let { pageData ->

        val htmlAnalyzer = HtmlAnalyzer(pageData)
        val formKey = htmlAnalyzer.getFormKey()
        val questionId = htmlAnalyzer.getQuestionId()
        val hashUrl = htmlAnalyzer.getHashUrl()


        // Getting hash
        AjaxHelper.get(
            hashUrl,
            onSuccess = { response ->
                params = Params(
                    formKey,
                    questionId,
                    HashHelper.getHashFrom(response)
                )
                onParamsReady()
            },
            onError = {
                fatalError = "Failed to get hash"
                console.log(fatalError)
            }
        )

    }

    // Setting click listener on page
    document.body?.onmousedown = {
        if (it.button == 1.toShort()) {

            println("Total Answers In Memory:  ${fullAnswers.size}")
            println("Read Answers:  ${openedAnswers.size}")

            getRandomAnswer(
                onAnswer = { randomAnswer ->
                    console.log("Random : $randomAnswer")
                    window.open(
                        "https://quora.com${randomAnswer.permaUrl}",
                        "_blank"
                    )
                },
                onLoadingMore = {
                    console.log("You've read all the answers in cache. Now requesting for more answers. Hold tight")
                },
                onNoMoreAnswer = {
                    window.alert("WTF! You read all answers available to this question")
                },
                onError = { message ->
                    window.alert(message)
                }
            )
        }
    }

}

fun getRandomAnswer(
    onAnswer: (Answer) -> Unit,
    onLoadingMore: () -> Unit,
    onNoMoreAnswer: () -> Unit,
    onError: (message: String) -> Unit
) {
    if (fullAnswers.isNotEmpty()) {

        val randomAnswer = fullAnswers
            .filter { openedAnswers.contains(it.id).not() } // answers that are not in read list
            .randomOrNull()

        if (randomAnswer != null) {
            // Add answer to read list
            val openedAnswersHot: MutableSet<String> = (window.localStorage.getItem(KEY_READ_ANSWERS) ?: "[]").let {
                Json.decodeFromString(it)
            }
            openedAnswersHot.add(randomAnswer.id)
            window.localStorage.setItem(KEY_READ_ANSWERS, JSON.stringify(openedAnswersHot))

            // Refresh read list
            openedAnswers.clear()
            openedAnswers.addAll(openedAnswersHot)

            onAnswer(randomAnswer)
        } else {
            // Load more answers
            onLoadingMore()
            loadNextPage(
                onDataLoaded = {
                    getRandomAnswer(
                        onAnswer = {
                            window.alert("Now try!")
                            onAnswer(it)
                        }, onLoadingMore, onNoMoreAnswer, onError
                    )
                },
                onError = {
                    onNoMoreAnswer()
                }
            )
        }
    } else {
        onError("Answers are not ready!")
    }
}

val openedAnswers: MutableSet<String> by lazy {
    (window.localStorage.getItem(KEY_READ_ANSWERS) ?: "[]").let {
        Json.decodeFromString(it)
    }
}
val fullAnswers = mutableListOf<Answer>()
var currentPage: Int = 0

fun onParamsReady() {
    loadNextPage()
}

private fun loadNextPage(
    onDataLoaded: () -> Unit = {},
    onError: (message: String) -> Unit = {}
) {
    currentPage++
    getAnswers(
        pageNo = currentPage,
        successCallback = { answersResponse ->
            val currentPageAnswers = answersResponse.data
                .question
                .pagedListDataConnection.edges
                .filter { it.node.answer != null }
                .map { it.node.answer!! }

            if (currentPageAnswers.isNotEmpty()) {
                fullAnswers.addAll(currentPageAnswers)
                onDataLoaded()
            } else {
                // com.theapache64.sq.data.remote.Data finished
                onError("No more answers")
            }
        },
        errorCallback = {
            onError(it)
        }
    )
}

const val ANSWER_PER_REQUEST = 10

fun getAnswers(pageNo: Int, successCallback: (AnswersResponse) -> Unit, errorCallback: (String) -> Unit) {

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

    val xhr = XMLHttpRequest()
    xhr.open("POST", "https://www.quora.com/graphql/gql_para_POST?q=QuestionAnswerPagedListQuery")
    xhr.setRequestHeader("content-type", "application/json")
    xhr.setRequestHeader("quora-formkey", params.formKey)
    xhr.onreadystatechange = {
        if (xhr.readyState.toInt() == 4) {
            if (xhr.status.toInt() == 200) {
                val json = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<AnswersResponse>(xhr.responseText)

                successCallback(json)
            } else {
                val message = "Failed to get answers : ${xhr.status} -> '${xhr.responseText}'"
                console.error(message)
                errorCallback(message)
            }
        }
    };
    xhr.send(requestBody)
}






