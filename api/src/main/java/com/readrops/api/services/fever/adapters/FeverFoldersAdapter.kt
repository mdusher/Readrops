package com.readrops.api.services.fever.adapters

import android.annotation.SuppressLint
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.extensions.nextNonEmptyString
import com.readrops.api.utils.extensions.skipField
import com.readrops.api.utils.extensions.skipToEnd
import com.readrops.db.entities.Folder
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson

class FeverFoldersAdapter {

    @ToJson
    fun toJson(folders: List<Folder>) = ""

    @SuppressLint("CheckResult")
    @FromJson
    fun fromJson(reader: JsonReader): List<Folder> = with(reader) {
        return try {
            val folders = arrayListOf<Folder>()

            beginObject()

            repeat(3) {
                skipField()
            }

            nextName() // beginning of folders array
            beginArray()

            while (hasNext()) {
                beginObject()

                val folder = Folder()
                while (hasNext()) {
                    with(folder) {
                        when (selectName(NAMES)) {
                            0 -> remoteId = nextInt().toString()
                            1 -> name = nextNonEmptyString()
                        }
                    }
                }

                folders += folder
                endObject()
            }

            endArray()
            skipToEnd()
            endObject()

            folders
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
    }

    companion object {
        val NAMES: JsonReader.Options = JsonReader.Options.of("id", "title")
    }
}