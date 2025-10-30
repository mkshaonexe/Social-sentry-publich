package com.example.socialsentry.data.datastore

import androidx.datastore.core.Serializer
import com.example.socialsentry.data.model.SocialSentrySettings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object SocialSentrySettingsSerializer : Serializer<SocialSentrySettings> {
    
    override val defaultValue: SocialSentrySettings
        get() = SocialSentrySettings()
    
    override suspend fun readFrom(input: InputStream): SocialSentrySettings {
        return try {
            Json.decodeFromString(
                deserializer = SocialSentrySettings.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }
    
    override suspend fun writeTo(t: SocialSentrySettings, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = SocialSentrySettings.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}

