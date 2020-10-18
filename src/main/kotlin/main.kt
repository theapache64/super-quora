import org.w3c.xhr.XMLHttpRequest
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.js.JSON.parse

private const val KEY_READ_ANSWERS = "read_answers"
private lateinit var params: Params
fun main() {
    document.documentElement?.outerHTML?.let { pageData ->
        // Getting form key
        val formKeyRegEx = "\"formkey\": \"(?<formKey>.+?)\"".toRegex()
        val formKey = formKeyRegEx.find(pageData)!!.groupValues[1]
        println("Form key : '$formKey'")

        // Getting question id
        val qIdRegEx = "\"pageOid\": (?<qId>\\d+)".toRegex()
        val questionId = qIdRegEx.find(pageData)!!.groupValues[1]
        println("Question ID : '$questionId'")

        // Getting hashUrl
        val x1 = pageData.split("ansFrontendRelayWebpackManifest = ")
        val x2 = x1[1].split("\"};")[0] + "\"}"
        val j1 = parse<kotlin.js.Json>(x2)
        val hashUrl = j1["page-QuestionPageLoadable"].toString()
        println("hashUrl: $hashUrl")

        // Getting hash response
        val xhr = XMLHttpRequest()
        xhr.open("GET", hashUrl)
        xhr.onreadystatechange = {
            if (xhr.readyState.toInt() == 4 && xhr.status.toInt() == 200) {
                val hashRegEx = "name:\"QuestionAnswerPagedListQuery\".+?,id:\"(?<hash>.+?)\",".toRegex()
                val data = xhr.responseText
                val matchResult = hashRegEx.find(data)!!
                val hash = matchResult.groupValues[1]
                params = Params(
                    formKey,
                    questionId,
                    hash
                )
                onParamsReady()
            }
        };
        xhr.send()
    }

    // Setting click listener on page
    document.body?.onmousedown = {
        if (it.button == 1.toShort()) {


            println("Total Answers In Memory:  ${fullAnswers.size}")
            println("Read Answers:  ${readAnswers.size}")

            getRandomAnswer(
                onAnswer = { randomAnswer ->
                    console.log("Random : $randomAnswer")
                    /*window.open(
                        "https://quora.com${randomAnswer.permaUrl}",
                        "_blank"
                    )*/
                },
                onLoading = {
                    console.log("You've read all the answers in cache. Now requesting for more answers...")
                },
                onAllAnswersRead = {
                    console.log("WTF! You read all answers available to this question")
                },
                onError = { message ->
                    console.log(message)
                }
            )
        }
    }

}

fun getRandomAnswer(
    onAnswer: (Answer) -> Unit,
    onLoading: () -> Unit,
    onAllAnswersRead: () -> Unit,
    onError: (message: String) -> Unit
) {
    if (fullAnswers.isNotEmpty()) {
        val randomAnswer = fullAnswers
            .filter { readAnswers.contains(it.id).not() } // answers that are not in read list
            .randomOrNull()

        if (randomAnswer != null) {
            // Add answer to read list
            val readAnswersHot: MutableSet<String> = (window.localStorage.getItem(KEY_READ_ANSWERS) ?: "[]").let {
                Json.decodeFromString(it)
            }
            readAnswersHot.add(randomAnswer.id)
            window.localStorage.setItem(KEY_READ_ANSWERS, JSON.stringify(readAnswersHot))
            readAnswers = readAnswersHot

            onAnswer(randomAnswer)
        } else {
            // Load more answers
            onLoading()
            loadNextPage(
                onDataLoaded = {
                    getRandomAnswer(
                        onAnswer, onLoading, onAllAnswersRead, onError
                    )
                },
                onError = {
                    onAllAnswersRead()
                }
            )
        }
    } else {
        onError("Answers are not ready!")
    }
}

var readAnswers = mutableSetOf<String>()
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
                // Data finished
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
                errorCallback("Failed to get answers : ${xhr.status} -> '${xhr.responseText}'")
            }
        }
    };
    xhr.send(requestBody)
}






