package com.robertotorino.datstation

import android.content.Context
import org.json.JSONArray
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object JsonParserHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY = "12345678901234567890123456789012" // 32 chars for AES-256
    private const val IV = "1234567890123456" // 16 chars for IV

    fun loadQuestionsFromAssets(context: Context): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        try {
            // Read encrypted file from assets
            val encryptedBytes = context.assets.open("questions.dat").use { it.readBytes() }
            
            // Decrypt the bytes
            val decryptedJsonString = decrypt(encryptedBytes)
            
            val root = JSONArray(decryptedJsonString)
            parseJsonArray(root, questions)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return questions
    }

    private fun decrypt(encryptedBytes: ByteArray): String {
        val keySpec = SecretKeySpec(KEY.toByteArray(), "AES")
        val ivSpec = IvParameterSpec(IV.toByteArray())
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun parseJsonArray(jsonArray: JSONArray, list: MutableList<QuestionEntity>) {
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.get(i)
            if (item is JSONArray) {
                parseJsonArray(item, list)
            } else if (item is org.json.JSONObject) {
                list.add(
                    QuestionEntity(
                        id = item.getString("id"),
                        text = item.getString("text"),
                        type = item.getString("type"),
                        category = item.getString("category"),
                        optionsJson = item.getJSONArray("options").toString(),
                        correctAnswer = item.getString("correct_answer"),
                        points = item.optInt("points", 10),
                        hasBeenAsked = false
                    )
                )
            }
        }
    }
}
