package com.robertotorino.datstation

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** Reads the quiz questions shipped as a plain JSON array in `assets/questions.json`. */
object JsonParserHelper {
    private const val TAG = "JsonParserHelper"
    const val ASSET_NAME = "questions.json"

    fun loadQuestionsFromAssets(context: Context): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        try {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            parseJsonArray(JSONArray(json), questions)
        } catch (e: Exception) {
            Log.e(TAG, "Could not load $ASSET_NAME", e)
        }
        return questions
    }

    private fun parseJsonArray(jsonArray: JSONArray, list: MutableList<QuestionEntity>) {
        for (i in 0 until jsonArray.length()) {
            when (val item = jsonArray.get(i)) {
                is JSONArray -> parseJsonArray(item, list)
                is JSONObject -> list.add(
                    QuestionEntity(
                        id = item.getString("id"),
                        text = item.getString("text"),
                        type = item.getString("type"),
                        category = item.getString("category"),
                        optionsJson = item.getJSONArray("options").toString(),
                        correctAnswer = item.getString("correct_answer"),
                        explanation = item.optString("explanation", ""),
                        points = item.optInt("points", 10),
                        hasBeenAsked = false
                    )
                )
            }
        }
    }
}
