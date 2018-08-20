package me.shutkin.muzhvay.http

import java.security.SecureRandom

class HTTPSession (val sessionId: String) {
  val started = System.currentTimeMillis()

  companion object {
    const val COOKIE_NAME = "MuSID"
    const val LIVE_TIME: Long = 30 * 60 * 1000

    private const val characters = "----AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz0123456789----"

    fun generate(): HTTPSession {
      val builder = StringBuilder()
      SecureRandom().ints(32, 0, characters.length).forEach { builder.append(characters[it]) }
      return HTTPSession(builder.toString())
    }
  }
}