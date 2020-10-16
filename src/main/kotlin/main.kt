import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.js.Json

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
        val j1 = JSON.parse<Json>(x2)
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
                onAllRequiredParamsAreReady(
                    formKey,
                    questionId,
                    hash
                )
            }
        };
        xhr.send()
    }
}


fun onAllRequiredParamsAreReady(formKey: String, questionId: String, hash: String) {
    // Let's do the final REST call
    val requestBody = """
        {
            "queryName": "QuestionAnswerPagedListQuery",
            "extensions": {
                "hash": "$hash"
            },
            "variables": {
                "qid": $questionId,
                "first": 50,
                "after": "0"
            }
        }
    """.trimIndent()

    val xhr = XMLHttpRequest()
    xhr.open("POST", "https://www.quora.com/graphql/gql_para_POST?q=QuestionAnswerPagedListQuery")
    xhr.setRequestHeader("content-type", "application/json")
    xhr.setRequestHeader("quora-formkey", formKey)
    xhr.onreadystatechange = {
        if (xhr.readyState.toInt() == 4 && xhr.status.toInt() == 200) {
            console.log("Hurray : ${xhr.responseText}")
        }
    };
    xhr.send(requestBody)
}


