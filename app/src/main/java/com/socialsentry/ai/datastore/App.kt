package com.socialsentry.ai.datastore

import kotlinx.serialization.Serializable

@Serializable
data class App(
    val name: String,
    val blocked: Boolean
)

