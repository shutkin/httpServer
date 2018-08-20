package me.shutkin.muzhvay.http

interface HTTPLogger {
  fun debug(message: String)
  fun info(message: String)
  fun warn(message: String)
  fun error(message: String)
  fun error(e: Throwable)
}
