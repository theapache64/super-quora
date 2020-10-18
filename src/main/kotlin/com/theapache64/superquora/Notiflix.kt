package com.theapache64.superquora

external class Notiflix {
    companion object {
        val Notify: Notify
        val Loading: Loading
    }
}

external interface Loading {
    fun Init(data: Any?)
    fun Standard(message : String)
}

external interface Notify {
    fun Success(msg: String)
    fun Failure(msg: String)
    fun Info(msg: String)
}