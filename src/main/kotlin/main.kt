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
        val qId = qIdRegEx.find(pageData)!!.groupValues[1]
        println("Question ID : '$qId'")

        // Getting hashUrl
        val x1 = pageData.split("ansFrontendRelayWebpackManifest = ")
        val x2 = x1[1].split("\"};")[0] + "\"}"
        val j1 = JSON.parse<Json>(x2)
        val hashUrl = j1["page-QuestionPageLoadable"].toString()
        println("hashUrl: $hashUrl")

        onHashUrl(hashUrl)
    }

}

fun onHashUrl(hashUrl: String) {
    // Getting response
    val xhr = XMLHttpRequest()
    xhr.open("GET", hashUrl)
    xhr.onreadystatechange = {
        if (xhr.readyState.toInt() == 4 && xhr.status.toInt() == 200) {
            val hashRegEx = "name:\"QuestionAnswerPagedListQuery\".+?,id:\"(?<hash>.+?)\",".toRegex()
            val data = xhr.responseText
            val matchResult = hashRegEx.find(data)!!
            val hash = matchResult.groupValues[1]
            onHash(hash)
        }
    };
    xhr.send()
}

fun onHash(hash: String) {
    println("Hash is $hash")
}
