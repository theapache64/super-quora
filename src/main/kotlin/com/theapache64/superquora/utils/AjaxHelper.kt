package com.theapache64.superquora.utils

import org.w3c.xhr.XMLHttpRequest

object AjaxHelper {

    fun get(url: String, onSuccess: (response: String) -> Unit, onError: () -> Unit) {
        // Getting hash response
        val xhr = XMLHttpRequest()
        xhr.open("GET", url)
        xhr.onreadystatechange = {
            if (xhr.readyState.toInt() == 4) {
                if (xhr.status.toInt() == 200) {
                    onSuccess(xhr.responseText)
                }
            }
        };
        xhr.send()
    }

}