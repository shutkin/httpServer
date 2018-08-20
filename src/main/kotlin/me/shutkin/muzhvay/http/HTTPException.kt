package me.shutkin.muzhvay.http

class HTTPException(val code: Int, message: String) : Exception(message) {
  override fun toString(): String {
    return "HTTPException(code=$code, message=$message)"
  }
}