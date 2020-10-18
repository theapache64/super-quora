package com.theapache64.superquora

import com.theapache64.superquora.data.remote.Answer
import com.theapache64.superquora.data.repo.AnswerRepo
import com.theapache64.superquora.models.Params
import com.theapache64.superquora.utils.AjaxHelper
import com.theapache64.superquora.utils.HashHelper
import com.theapache64.superquora.utils.HtmlAnalyzer
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.url.URL

private lateinit var params: Params
private var fatalError: String? = null

val fullAnswers = mutableListOf<Answer>()
var currentPage: Int = 0

fun main() {

    when (window.location.toString().count { char -> char == '/' }) {
        3 -> {
            // question
            watchOpenRandomAnswer()
        }

        5 -> {
            // answer
            watchOpenRandomAnswer()
            watchForRemoveAnswerFromOpenedList()
        }

    }

}

fun watchForRemoveAnswerFromOpenedList() {

    document.body?.onkeyup = {
        if (it.keyCode == 46) {
            val answerId = URL(window.location.toString()).searchParams.get("id")
            if (answerId != null) {
                AnswerRepo.removeFromOpenedAnswers(answerId)
                Notiflix.Notify.Success("Removed answer from opened answer list")
            }
        }
    }
}

private fun watchOpenRandomAnswer() {
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
                console.log("Params ready -> $params")
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
            if (fatalError != null) {
                window.alert(fatalError!!)
            } else {

                getRandomAnswer(
                    onAnswer = { randomAnswer ->
                        console.log("Random : $randomAnswer")
                        window.open(
                            "https://quora.com${randomAnswer.permaUrl}?id=${randomAnswer.id}",
                            "_blank"
                        )
                    },
                    onLoadingMore = {
                        Notiflix.Notify.Info("You've already read all answers in page $currentPage, checking next page")
                    },
                    onNoMoreAnswer = {
                        fatalError = "No more random answers. You've read all!"
                        Notiflix.Notify.Failure(fatalError!!)
                    },
                    onError = { message ->
                        Notiflix.Notify.Failure("Error: $message")
                    }
                )
            }
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

        val openedAnswers = AnswerRepo.getOpenedAnswers()

        val randomAnswer = fullAnswers
            .filter { openedAnswers.contains(it.id).not() } // answers that are not in read list
            .randomOrNull()

        if (randomAnswer != null) {
            AnswerRepo.appendToOpenedAnswers(randomAnswer)
            onAnswer(randomAnswer)
        } else {
            // Load more answers
            onLoadingMore()
            loadNextPage(
                onDataLoaded = {
                    getRandomAnswer(
                        onAnswer = {
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
        Notiflix.Notify.Info("Wo wo wo! Wait a sec. Quora is not fast as you ...")
    }
}

fun onParamsReady() {
    loadNextPage()
}

private fun loadNextPage(
    onDataLoaded: () -> Unit = {},
    onError: (message: String) -> Unit = {}
) {
    currentPage++
    AnswerRepo.getAnswers(
        pageNo = currentPage,
        params = params,
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
                onError("No more answers")
            }
        },
        errorCallback = {
            onError(it ?: "Something went wrong")
        }
    )
}









