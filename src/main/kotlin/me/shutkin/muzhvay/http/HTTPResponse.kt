package me.shutkin.muzhvay.http

import java.io.InputStream

class HTTPResponse (val request: HTTPRequest?, val session: HTTPSession?, val code: Int, val body: InputStream, val contentType: String, val contentSize: Int) {
  val cookies = ArrayList<HTTPCookie>()

  constructor(request: HTTPRequest, session: HTTPSession, textBody: String): this(request, session, 200, textBody.toByteArray().inputStream(), HTTP.ContentType.HTML, textBody.length)

  override fun toString(): String {
    return "HTTPResponse(code=$code, contentType='$contentType', body.size=$contentSize cookies=$cookies)"
  }
}

data class HTTPCookie (val name: String, val value: String, val maxAge: Long, val isHttpOnly: Boolean)