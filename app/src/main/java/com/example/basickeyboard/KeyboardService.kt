package com.example.basickeyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout


class KeyboardService : InputMethodService() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    // Define a mapping of keys to rows
    private val emojis = "👍😎💩"
    private val lock = "\uD83D\uDD12"
    private val abcLayout = arrayOf(
        arrayOf("#", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ⌫ "),
        arrayOf("@", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", ":)"),
        arrayOf(lock, "a", "s", "d", "f", "g", "h", "j", "k", "l", "'", " ↵"),
        arrayOf("  ⇧ ", "z", "x", "c", "v", "b", "n", "m", ",", ".", "?", "!" ),
        arrayOf("123%","(", "🎤", "                  ",  ")",  emojis)
        // Add other special keys or rows as needed
    )
    private val capsLayout = arrayOf(
        arrayOf("@", "#", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ⌫ "),
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "(", ")"),
        arrayOf(lock, "A", "S", "D", "F", "G", "H", "J", "K", "L", "\"", " ↵ "),
        arrayOf("  ⇧ ", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "?", "!" ),
        arrayOf("123%", "👍", "                     ", ":)",  emojis)
        // Add other special keys or rows as needed
    )
    // Define rows of keys
    private val numLayout = arrayOf(
        arrayOf("#", "^", "!", "/", "*", "⌫"),
        arrayOf("$", "7", "8", "9", "%", "~"),
        arrayOf("(", "4", "5", "6", "+", ")"),
        arrayOf("[", "1", "2", "3", "-", "]"),
        arrayOf("abc", " 0 ", " . ", " = ", emojis + "  ")

        // Add other special keys or rows as needed
    )
    private val emojiLayout = arrayOf(
        // Row 1: Most Popular Emotions & Gestures
        arrayOf("😂", "😍", "😎", "💀", "😇", "😢", "🥳", "😡", "⌫"),

        // Row 2: More weird faces and expressions
        arrayOf("😈", "🤡", "🤠", "🤑", "🤓", "🤖", "👽", "👾", "👻"),

        // Row 2: Activities & Celebrations
        arrayOf(":)", "👀", "🙈", "💩", "💃",  "🎉", "👅", "🚴‍♀️", "!"),

        // Row 3: Animals, Nature & Weather
        arrayOf("<3", "🚗", "🐱", "😤", "🙏", "🌳", "🔥", "❄️", "?"),

        // Row 4: Food, Objects & Symbols
        arrayOf("123%", "🍕", "               ",  "👍", "❤️", "abc")
    )

    private val keyboards = mapOf(
        "abc" to abcLayout,
        lock to capsLayout,
        "123%" to numLayout,
        emojis to emojiLayout
    )

    private var currentKeyboardName = "abc"
    private var currentKeyboard = keyboards[currentKeyboardName]!!
    private var nextKeyboard: String? = null
    private var isListening = false
    private var typedInput = ""
    private var spokenInput = ""


    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (isListening) {
                    speechRecognizer.startListening(speechRecognizerIntent)
                }
            }
            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error: $error")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    spokenInput = matches[0]
                    compareAndCommit()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onCreateInputView(): View {
        // Initialize the keyboard layout
        return makeKeyboardLayout(currentKeyboard)
    }


    private fun switchKeyboard(keyboardName: String, temporary: Boolean = false) {
        if (temporary) {
            nextKeyboard = currentKeyboardName
        }else{
            nextKeyboard = null
        }
        currentKeyboardName = keyboardName
        currentKeyboard = keyboards[currentKeyboardName]!!
        setInputView(onCreateInputView())

    }

    private fun makeKeyboardLayout(keyboard: Array<Array<String>>): LinearLayout {
        val baseLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as LinearLayout
        keyboard.forEach { row ->
            val rowLayout = LinearLayout(this)
            rowLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rowLayout.orientation = LinearLayout.HORIZONTAL

            // Calculate the total weight for the row based on the length of the labels
            val totalWeight = row.map { it.length }.sum().toFloat()
            row.forEach { keyLabel ->
                val key = Button(this)
                key.text = keyLabel
                // Set the text size of the button
                key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) // 18sp font size, change as needed

                key.setOnClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "🎤" -> {
                            if (!isListening) {
                                speechRecognizer.startListening(speechRecognizerIntent)
                                isListening = true
                            } else {
                                speechRecognizer.stopListening()
                                isListening = false
                            }
                        }
                        "⇧" -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, currentKeyboardName == "abc")
                        "123%" -> switchKeyboard("123%")
                        "abc" -> switchKeyboard("abc")
                        lock -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock)
                        emojis -> switchKeyboard(emojis)
                        "→|" -> inputText('\t'.toString())
                        "⌫" -> handleBackspace()
                        "↵" -> inputText("\n")
                        "" -> inputText(" ")
                        else -> inputText(keyLabel)
                    }
                }

                // Set OnLongClick Listener
                key.setOnLongClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "⇧" -> Unit
                        "123%" -> Unit
                        "abc" -> Unit
                        lock -> Unit
                        emojis -> Unit
                        "⌫" -> clearText()
                        "↵" -> Unit
                        else -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, true)
                    }
                    true
                }
                key.setPadding(0, 0, 0, 0)

                // Calculate weight for each key based on its label length
                val keyWeight =  (2 + keyLabel.length.toFloat()) / totalWeight
                val keyLayoutParams = LinearLayout.LayoutParams(0, 130, keyWeight)
                keyLayoutParams.setMargins(-4, -4, -4, -4)
                key.layoutParams = keyLayoutParams

                rowLayout.addView(key)
            }

            baseLayout.addView(rowLayout)
        }

        return baseLayout
    }

    private fun inputText(text: String) {
        if (text == " ") {
            compareAndCommit()
            typedInput = ""
            spokenInput = ""
        } else {
            typedInput += text
        }
        val inputConnection = currentInputConnection
        inputConnection?.commitText(text, 1)
        if (nextKeyboard != null) {
            switchKeyboard(nextKeyboard!!)
        }
    }

    private fun handleBackspace() {
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)

        if (selectedText.isNullOrEmpty()) {
            // No text is selected, so delete the character before the cursor
            inputConnection?.deleteSurroundingText(1, 0)
        } else {
            // Text is selected, so delete the selection
            inputConnection?.commitText("", 1)
        }
    }

    private fun clearText() {
        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(100, 0)
    }

    private fun compareAndCommit() {
        // TODO: Implement a more sophisticated dictionary and word prediction algorithm.
        val bestResult = if (typedInput.length > spokenInput.length) {
            typedInput
        } else {
            spokenInput
        }

        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(typedInput.length, 0)
        inputConnection?.commitText(bestResult, 1)
    }
}
