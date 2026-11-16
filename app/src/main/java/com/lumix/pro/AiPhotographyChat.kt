package com.lumix.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object AiPhotographyClient {
    private val http = OkHttpClient()
    suspend fun ask(apiKey: String, question: String): Result<String> = runCatching {
        require(apiKey.isNotBlank()) { "أدخل مفتاح OpenAI في خانة المساعد" }
        require(question.isNotBlank()) { "اكتب سؤالك أولًا" }
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", .35)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "system")
                put("content", "أنت مساعد تصوير احترافي متخصص في الكاميرات والإضاءة والتكوين والتحرير والفيديو. أجب بالعربية باختصار عملي، واسأل عن الهاتف والمشهد عند الحاجة. لا تدّعي أن إعدادًا يعمل إذا كان الهاتف لا يدعمه.")
            }).put(JSONObject().apply { put("role", "user"); put("content", question) }))
        }.toString()
        val request = Request.Builder().url("https://api.openai.com/v1/chat/completions").addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(body.toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("فشل الاتصال: ${response.code}")
            JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }
}

@Composable
fun AiPhotographyChat(apiKey: String, onApiKey: (String) -> Unit, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("اسألني عن الإضاءة أو العدسات أو التكوين أو تعديل الصور.") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color(0xF21A222B), RoundedCornerShape(26.dp)).border(1.dp, Color(0x667CF4D6), RoundedCornerShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("LUMIX AI · مساعد التصوير", color = Color(0xFF7CF4D6)); LiquidAction("إغلاق", onClick = onClose) }
        OutlinedTextField(value = apiKey, onValueChange = onApiKey, label = { Text("مفتاح OpenAI الخاص بك") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("المفتاح لا يأتي داخل التطبيق. أدخله أنت حتى لا يتم تسريب سر داخل APK.", color = Color.LightGray)
        OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("اسأل عن أي شيء في التصوير") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Text(answer, color = Color.White)
        if (busy) CircularProgressIndicator(color = Color(0xFF7CF4D6)) else Button(onClick = { busy = true; scope.launch { answer = AiPhotographyClient.ask(apiKey, question).getOrElse { it.message ?: "تعذر الحصول على إجابة" }; busy = false } }) { Text("اسأل المساعد") }
        Spacer(Modifier.height(2.dp))
    }
}
