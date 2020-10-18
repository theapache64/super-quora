package com.theapache64.superquora.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AnswersResponse(
    @SerialName("data")
    val `data`: Data
)


@Serializable
data class Data(
    @SerialName("question")
    val question: Question
)

@Serializable
data class Question(
    @SerialName("id")
    val id: String, // UXVlc3Rpb25AMDoyMDYzOTM0
    @SerialName("pagedListDataConnection")
    val pagedListDataConnection: PagedListDataConnection,
    @SerialName("qid")
    val qid: Int, // 2063934
    @SerialName("__typename")
    val typename: String // com.theapache64.sq.data.remote.Question
)

@Serializable
data class PagedListDataConnection(
    @SerialName("edges")
    val edges: List<Edge>,
    @SerialName("pageInfo")
    val pageInfo: PageInfo,
    @SerialName("__typename")
    val typename: String // QuestionPagedListConnection
)

@Serializable
data class Edge(
    @SerialName("cursor")
    val cursor: String, // 4
    @SerialName("id")
    val id: String, // UXVlc3Rpb25QYWdlZExpc3RFZGdlOjQ=
    @SerialName("node")
    val node: Node,
    @SerialName("__typename")
    val typename: String // QuestionPagedListEdge
)

@Serializable
data class PageInfo(
    @SerialName("endCursor")
    val endCursor: String, // 12
    @SerialName("hasNextPage")
    val hasNextPage: Boolean // true
)

@Serializable
data class Node(
    @SerialName("answer")
    val answer: Answer? = null
)


@Serializable
data class Answer(
    @SerialName("aid")
    val aid: Int, // 239799923
    @SerialName("id")
    val id: String, // fjkhgkhhdfgjkdkfj
    @SerialName("permaUrl")
    val permaUrl: String, // /How-can-I-get-over-a-break-up/answer/Ãnšhul-Thakur
)

