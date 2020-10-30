package com.theapache64.superquora

import com.theapache64.superquora.data.remote.Answer
import com.theapache64.superquora.data.repo.AnswerRepo
import com.theapache64.superquora.models.Params
import com.theapache64.superquora.utils.AjaxHelper
import com.theapache64.superquora.utils.HashHelper
import com.theapache64.superquora.utils.HtmlAnalyzer
import com.theapache64.superquora.utils.OPEN_RANDOM_QUESTION_BUTTON
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.url.URL

private lateinit var params: Params
private var fatalError: String? = null
private var shouldShowAnswerReady: Boolean = false

val fullAnswers = mutableListOf<Answer>()
var currentPage: Int = 0

val ANSWER_REGEX = "https:\\/\\/www\\.quora\\.com\\/(.+?)\\/answers?\\/".toRegex()

fun main() {

    val pageUrl = window.location.toString()
    val slashCount = pageUrl.count { char -> char == '/' }

    if (slashCount == 3) {
        // question
        watchOpenRandomAnswer(isQuestion = true)
    } else {
        val isAnswer = ANSWER_REGEX.find(pageUrl) != null
        if (isAnswer) {
            watchOpenRandomAnswer(isQuestion = false)
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

private const val QUESTION_DIV_SELECTOR =
    "#root > div > div > div:nth-child(3) > div > div > div:nth-child(1) > div.q-box.qu-borderBottom > div > div.q-box.qu-zIndex--action_bar > div > div:nth-child(1)"
private const val ANSWER_DIV_SELECTOR =
    "#root > div > div > div:nth-child(3) > div > div > div > div.q-box.qu-borderAll.qu-borderRadius--small.qu-borderColor--raised.qu-boxShadow--small.qu-bg--raised > div.q-box.qu-pt--medium.qu-px--medium.qu-pb--tiny > div.q-box.qu-bg--raised.qu-zIndex--action_bar.qu-borderTop.qu-boxShadow--n_small > div > div:nth-child(1)"

private fun watchOpenRandomAnswer(isQuestion: Boolean) {

    // Add 'Random Answer' Button

    val selector = if (isQuestion) {
        QUESTION_DIV_SELECTOR
    } else {
        ANSWER_DIV_SELECTOR
    }

    val buttonContainer =
        document.querySelector(selector) as? HTMLDivElement
    if (buttonContainer != null) {
        buttonContainer.insertAdjacentHTML("beforeend", OPEN_RANDOM_QUESTION_BUTTON)

        // Adding listener for added button
        val openRandomAnswerButton = document.getElementById("button_open_random_answer") as HTMLButtonElement
        openRandomAnswerButton.addEventListener("click", {
            openRandomAnswer()
        })

        prepareData(isQuestion)
    } else {
        console.log("Failed to find button container")
    }
}

private fun prepareData(isQuestion: Boolean) {
    document.documentElement?.outerHTML?.let { pageData ->

        val htmlAnalyzer = HtmlAnalyzer(pageData)
        val formKey = htmlAnalyzer.getFormKey()
        val questionId = htmlAnalyzer.getQuestionId(isQuestion)
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
}

private fun openRandomAnswer() {
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
        shouldShowAnswerReady = true
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
                if (shouldShowAnswerReady) {
                    Notiflix.Notify.Success("Now try! :)")
                    shouldShowAnswerReady = false
                }
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









