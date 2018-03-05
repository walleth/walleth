package org.walleth.functions

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.JSONObjectIterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun JSONObject.getStringOrNull(name: String) = if (has(name)) getString(name) else null
