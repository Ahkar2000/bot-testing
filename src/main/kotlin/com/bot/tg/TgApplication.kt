package com.bot.tg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TgApplication

fun main(args: Array<String>) {
	runApplication<TgApplication>(*args)
}