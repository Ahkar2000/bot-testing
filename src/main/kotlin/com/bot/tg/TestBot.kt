package com.bot.tg

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class TestBot(
    @Value("\${telegram.bot.token}") private val token: String,
    @Value("\${telegram.bot.name}") private val name: String
) : TelegramLongPollingBot(token) {

    override fun getBotUsername(): String = name

    data class QuizState(var currentQuestion: Int = 0, val answers: MutableList<Int> = mutableListOf(), val gender: String = "")

    private val questions = mapOf(
        "male" to listOf(
            "Do you like Double J songs?",
            "Do you shower two times a day?",
            "Do you like Myo Gyi?",
            "Do you wear Jean pants?",
            "Do you have more than 3 friends?",
            "Your opinion on iced coffee?",
            "Have you ever tried wearing makeup or lipstick?",
            "Favorite color: is it blue or black?",
            "Do you think yourself as a smart person?",
            "Do you watch drama movies?"
        ),
        "female" to listOf(
            "How often do you compliment other girls?",
            "Do you love watching BL or queer romance dramas?",
            "Rate how fabulous you feel today",
            "Do you own rainbow-colored clothes?",
            "How often do you use heart emojis?",
            "Do you love to watch football match?",
            "Do you have a male gay best friend?",
            "How often do you say 'queen'?",
            "Do you like pop divas (e.g., Lady Gaga, Ariana)?",
            "Would you go to cinema with a female friend?"
        )
    )

    private val quizSessions = mutableMapOf<Long, QuizState>()

    override fun onUpdateReceived(update: Update) {
        val message = update.message
        val callback = update.callbackQuery

        if (message != null && message.hasText()) {
            val chatId = message.chatId
            sendWelcome(chatId.toString())
        }

        if (callback != null) {
            val chatId = callback.message.chatId
            val data = callback.data

            if (data == "menu_start") {
                val genderKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(listOf(
                        InlineKeyboardButton.builder().text("♂️ Male").callbackData("gender_male").build(),
                        InlineKeyboardButton.builder().text("♀️ Female").callbackData("gender_female").build()
                    ))
                    .build()

                val msg = SendMessage(chatId.toString(), "What's your gender?")
                msg.replyMarkup = genderKeyboard
                execute(msg)
            } else if (data.startsWith("gender_")) {
                val gender = data.removePrefix("gender_")
                quizSessions[chatId] = QuizState(gender = gender)
                askNextQuestion(chatId)
            } else if (data.startsWith("answer_")) {
                val answer = data.removePrefix("answer_").toIntOrNull() ?: return
                val state = quizSessions[chatId] ?: return

                state.answers.add(answer)
                state.currentQuestion++
                askNextQuestion(chatId)
            }
        }
    }

    private fun sendWelcome (chatId: String) {
        val startButton = InlineKeyboardButton.builder()
            .text("▶️ Start Menu")
            .callbackData("menu_start")
            .build()

        val markup = InlineKeyboardMarkup.builder()
            .keyboardRow(listOf(startButton))
            .build()

        val welcome = SendMessage.builder()
            .chatId(chatId)
            .text("👋 Welcome to gay detector:")
            .replyMarkup(markup)
            .build()

        execute(welcome)
    }

    private fun askNextQuestion(chatId: Long) {
        val state = quizSessions[chatId] ?: return
        val genderQuestions = questions[state.gender] ?: return

        if (state.currentQuestion >= genderQuestions.size) {
            val score = state.answers.sum()
            val maxScore = genderQuestions.size * 5
            val percent = (score.toDouble() / maxScore * 100).toInt()

            val result = SendMessage(chatId.toString(), "🌈 Your gay rating is: $percent%")
            execute(result)
            quizSessions.remove(chatId)
            sendWelcome(chatId.toString())
            return
        }

        val questionText = genderQuestions[state.currentQuestion]
        val buttons = (1..5).map {
            InlineKeyboardButton.builder()
                .text(it.toString())
                .callbackData("answer_$it")
                .build()
        }

        val markup = InlineKeyboardMarkup.builder()
            .keyboardRow(buttons)
            .build()

        val msg = SendMessage(chatId.toString(), "Q${state.currentQuestion + 1}: $questionText")
        msg.replyMarkup = markup
        execute(msg)
    }
}