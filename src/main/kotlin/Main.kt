package com.example

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class WeatherData(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
) {
    override fun toString(): String {
        return "Погода в городе ${this.name}:\n" +
                "Температура: ${this.main.temp}°C\n" +
                "Описание: ${this.weather.firstOrNull()?.description ?: "Неизвестно"}\n" +
                "Влажность: ${this.main.humidity}%\n" +
                "Скорость ветра: ${this.wind.speed} м/с\n"
    }
}

@Serializable
data class Main(val temp: Double, val humidity: Int) {
    override fun toString(): String {
        return super.toString()
    }
}

@Serializable
data class Weather(val description: String)

@Serializable
data class Wind(val speed: Double)

val client = OkHttpClient()
val json = Json { ignoreUnknownKeys = true }
val openWeatherApiKey = "ce19e1d1db5948e4b723a5016232b6a0"

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val bot = bot {
        token = "7429355221:AAH6HOkBWSDy3v25E4xtM1I9BIWeT9fCqvU"
        dispatch {
            command("help") {
                val result = bot.sendMessage(
                    ChatId.fromId(message.chat.id), text = "Напиши /start :)"
                )
            }
            command("start") {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(text = "Да", callbackData = "Yes")),
                    listOf(InlineKeyboardButton.CallbackData(text = "Пашол нафик", callbackData = "No")),
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Хочешь узнать мою функцию?",
                    replyMarkup = inlineKeyboardMarkup,


                    )
            }
            callbackQuery("yes") {
                bot.sendMessage(
                    chatId = ChatId.fromId(this.callbackQuery.from.id),
                    "Если ты напишешь команду /weather, то я расскажу тебе о погоде!"
                )
            }
            callbackQuery("no") {
                bot.sendMessage(
                    chatId = ChatId.fromId(this.callbackQuery.from.id),
                    "Ваш кампутер будет взломан!!!")
            }
            command("weather") {
                val cities = listOf("Нижний Новгород", "Москва", "Санкт-Петербург")
                val keyboard = InlineKeyboardMarkup.create(
                    cities.map { city ->
                        listOf(InlineKeyboardButton.CallbackData(text = city, callbackData = city))
                    }
                )
                bot.sendMessage(ChatId.fromId(message.chat.id), "Выбери город", replyMarkup = keyboard)
            }

            callbackQuery {
                val city = callbackQuery.data
                val chatId = this.callbackQuery.from.id
                GlobalScope.launch {
                    val weatherData = getWeather(city)
                    if (weatherData == null) {
                        bot.sendMessage(ChatId.fromId(chatId), "При получении данных о погоде произошла ошибка")
                    } else {
                        bot.sendMessage(ChatId.fromId(chatId), weatherData.toString())
                    }
                }
            }
        }
    }

    bot.startPolling()
}

suspend fun getWeather(city: String): WeatherData? {
    val request = Request.Builder()
        .url("http://api.openweathermap.org/data/2.5/weather?q=$city&appid=$openWeatherApiKey&units=metric&lang=ru")
        .build()
    return withContext(Dispatchers.IO) {
        Thread.sleep(3000)
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext null
        }
        json.decodeFromString<WeatherData>(response.body.string())
    }
}


fun buildWeatherMessage(weatherData: WeatherData): String = """
    Погода в городе ${weatherData.name}:
    Температура: ${weatherData.main.temp}°C
    Описание: ${weatherData.weather.firstOrNull()?.description ?: "Неизвестно"}
    Влажность: ${weatherData.main.humidity}%
    Скорость ветра: ${weatherData.wind.speed} м/с
""".trimIndent()