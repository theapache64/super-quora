package com.theapache64.superquora.utils

import org.w3c.xhr.XMLHttpRequest

object AjaxHelper {

    fun post(
        url: String,
        requestBody : String,
        headers: List<Pair<String, String>>,
        onSuccess: (response: String) -> Unit,
        onError: (msg: String?) -> Unit
    ) {
        val xhr = XMLHttpRequest()
        xhr.open("POST", url)
        for (header in headers) {
            val (key, value) = header
            xhr.setRequestHeader(key, value)
        }
        xhr.onreadystatechange = {
            if (xhr.readyState.toInt() == 4) {
                if (xhr.status.toInt() == 200) {
                    onSuccess(xhr.responseText)
                } else {
                    onError("${xhr.status} -> '${xhr.responseText}'")
                }
            }
        };
        xhr.send(requestBody)

    }

    fun get(url: String, onSuccess: (response: String) -> Unit, onError: () -> Unit) {
        // Getting hash response
        val xhr = XMLHttpRequest()
        xhr.open("GET", url)
        xhr.onreadystatechange = {
            if (xhr.readyState.toInt() == 4) {
                if (xhr.status.toInt() == 200) {
                    onSuccess(xhr.responseText)
                } else {
                    onError()
                }
            }
        };
        xhr.send()
    }

}