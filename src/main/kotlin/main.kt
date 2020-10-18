import org.w3c.xhr.XMLHttpRequest
import kotlinx.browser.document
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.js.JSON.parse


private lateinit var params: Params
private var page = 1
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
}

fun onParamsReady() {
    getAnswers(
        pageNo = 1,
        successCallback = {
            println("Got ${it.size} answers")
        },
        errorCallback = {
            println("Error: ${it}")
        }
    )
}

const val ANSWER_PER_REQUEST = 10

fun getAnswers(pageNo: Int, successCallback: (List<Answer>) -> Unit, errorCallback: (String) -> Unit) {

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
                println("Edged : ${json.data.question.pagedListDataConnection.edges.size}")
            } else {
                errorCallback("Failed to get answers : ${xhr.status} -> '${xhr.responseText}'")
            }
        }
    };
    xhr.send(requestBody)
}





