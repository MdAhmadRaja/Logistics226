package com.example.logistics

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.*

class VoiceAddLoadFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var startVoiceButton: Button
    private lateinit var tts: TextToSpeech
    private lateinit var hindiToEnglishMap: Map<String, Pair<String, String>>
    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    private var step = 0

    private var logisticType = ""
    private var tons = 0.0
    private var pricePerTon = 0.0
    private var totalPrice = 0.0
    private var vehicleType = "Truck"
    private var contact = ""
    private var sourceState = ""
    private var sourceCity = ""
    private var destinationState = ""
    private var destinationCity = ""
    private var date = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_add_load, container, false)
        startVoiceButton = view.findViewById(R.id.startVoiceButton1)
        tts = TextToSpeech(requireContext(), this)
        hindiToEnglishMap = loadHindiEnglishMap()

        startVoiceButton.setOnClickListener { startConversation() }

        return view
    }

    private fun loadHindiEnglishMap(): Map<String, Pair<String, String>> {
        val map = mutableMapOf<String, Pair<String, String>>()
        try {
            val inputStream: InputStream = requireContext().assets.open("hindi_to_english.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj: JSONObject = jsonArray.getJSONObject(i)
                val cityHindi = obj.getString("city_hindi")
                val cityEng = obj.getString("name")
                val stateHindi = obj.getString("state_hindi")
                val stateEng = obj.getString("state")
                map[cityHindi] = Pair(cityEng, stateEng)
                map[stateHindi] = Pair(cityEng, stateEng)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun startConversation() {
        step = 0
        speak("नमस्ते! चलिए आपका लोड जोड़ते हैं। कृपया लोड का नाम बताइए।")
        listenHindi()
    }

    private fun listenHindi() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            startActivityForResult(intent, 100)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0)?.trim() ?: return

            when (step) {
                0 -> {
                    logisticType = answer
                    step++
                    speak("कृपया बताइए किस राज्य से लोड भेजना है।")
                    listenHindi()
                }
                1 -> {
                    sourceState = translateToEnglish(answer, isState = true)
                    step++
                    speak("कृपया बताइए किस शहर से भेजना है।")
                    listenHindi()
                }
                2 -> {
                    sourceCity = translateToEnglish(answer)
                    step++
                    speak("कृपया बताइए किस राज्य में भेजना है।")
                    listenHindi()
                }
                3 -> {
                    destinationState = translateToEnglish(answer, isState = true)
                    step++
                    speak("कृपया बताइए किस शहर में भेजना है।")
                    listenHindi()
                }
                4 -> {
                    destinationCity = translateToEnglish(answer)
                    step++
                    speak("लोड कितने टन का है?")
                    listenHindi()
                }
                5 -> {
                    tons = extractNumber(answer)
                    step++
                    speak("प्रति टन कीमत बताइए।")
                    listenHindi()
                }
                6 -> {
                    pricePerTon = extractNumber(answer)
                    totalPrice = tons * pricePerTon
                    step++
                    speak("किस तारीख को लोड भेजना है?")
                    listenHindi()
                }
                7 -> {
                    date = answer
                    step++
                    val summary = "आपका लोड $logisticType है, $sourceCity $sourceState से $destinationCity $destinationState तक, $tons टन, प्रति टन ₹$pricePerTon, कुल ₹$totalPrice, दिनांक $date. क्या यह सही है?"
                    speak(summary)
                    listenHindi()
                }
                8 -> {
                    if (answer.contains("हां") || answer.contains("सही")) {
                        saveToFirebase()
                    } else {
                        speak("कृपया बताइए कौन सा हिस्सा गलत है — राज्य, शहर, टन, या कीमत?")
                        listenHindi()
                    }
                }
            }
        }
    }

    private fun translateToEnglish(input: String, isState: Boolean = false): String {
        for ((hindi, pair) in hindiToEnglishMap) {
            if (input.contains(hindi)) {
                return if (isState) pair.second else pair.first
            }
        }
        return input
    }

    private fun extractNumber(text: String): Double {
        return text.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
    }

    private fun saveToFirebase() {
        val user = mAuth.currentUser ?: return
        val data = hashMapOf(
            "uid" to user.uid,
            "logisticType" to logisticType,
            "vehicleType" to vehicleType,
            "tons" to tons,
            "pricePerTon" to pricePerTon,
            "totalPrice" to totalPrice,
            "contact" to (user.phoneNumber ?: ""),
            "sourceCity" to sourceCity,
            "sourceState" to sourceState,
            "destinationCity" to destinationCity,
            "destinationState" to destinationState,
            "date" to date,
            "status" to "Pending"
        )

        db.collection("logistics").add(data)
            .addOnSuccessListener {
                speak("आपका लोड सफलतापूर्वक जोड़ दिया गया है। धन्यवाद।")
            }
            .addOnFailureListener {
                speak("कुछ त्रुटि हुई है। कृपया दोबारा कोशिश करें।")
            }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
