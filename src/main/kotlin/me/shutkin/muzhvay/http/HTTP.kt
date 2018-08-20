package me.shutkin.muzhvay.http

class HTTP {
  enum class Protocol { HTTP10, HTTP11 }

  enum class Method { GET, HEAD, POST, PUT, DELETE }

  class RequestFields {
    companion object {
      const val A_IM = "A-IM"
      const val ACCEPT = "Accept"
      const val ACCEPT_CHARSET = "Accept-Charset"
      const val ACCEPT_ENCODING = "Accept-Encoding"
      const val ACCEPT_LANGUAGE = "Accept-Language"
      const val ACCEPT_DATETIME = "Accept-Datetime"
      const val ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"
      const val ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"
      const val AUTHORIZATION = "Authorization"
      const val CACHE_CONTROL = "Cache-Control"
      const val CONNECTION = "Connection"
      const val CONTENT_LENTH = "Content-Lenth"
      const val CONTENT_MD5 = "Content-MD5"
      const val CONTENT_TYPE = "Content-Type"
      const val COOKIE = "Cookie"
      const val DATE = "Date"
      const val EXPECT = "Expect"
      const val FORWARDED = "Forwarded"
      const val FROM = "From"
      const val HOST = "Host"
      const val IF_MATCH = "If-Match"
      const val IF_MODIFIED_SINCE = "If-Modified-Since"
      const val IF_NONE_MATCH = "If-None-Match"
      const val IF_RANGE = "If-Range"
      const val IF_INMODIFIED_SINCE = "If-Unmodified-Since"
      const val MAX_FORWARDS = "Max-Forwards"
      const val ORIGIN = "Origin"
      const val PRAGMA = "Pragma"
      const val PROXY_AUTHORIZATION = "Proxy-Autorization"
      const val RANGE = "Range"
      const val REFERER = "Referer"
      const val TE = "TE"
      const val USER_AGENT = "User-Agent"
      const val UPGRADE = "Upgrade"
      const val VIA = "Via"
      const val WARNING = "Warning"

      const val CONTENT_DISPOSITION = "Content-Disposition"
    }
  }

  class ResponseFields {
    companion object {
      const val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
      const val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"
      const val ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers"
      const val ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age"
      const val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"
      const val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"
      const val ACCEPT_PATCH = "Accept-Patch"
      const val ACCEPT_RANGES = "Accept-Ranges"
      const val AGE = "Age"
      const val ALLOW = "Allow"
      const val ALT_SVC = "Alt-Svc"
      const val CACHE_CONTROL = "Cache-Control"
      const val CONNECTION = "Connection"
      const val CONTENT_DISPOSITION = "Content-Disposition"
      const val CONTENT_ENCODING = "Content-Encoding"
      const val CONTENT_LENTH = "Content-Lenth"
      const val CONTENT_LOCATION = "Content-Location"
      const val CONTENT_MD5 = "Content-MD5"
      const val CONTENT_RANGE = "Content-Range"
      const val CONTENT_TYPE = "Content-Type"
      const val COOKIE = "Cookie"
      const val DATE = "Date"
      const val DELTA_BASE = "Delta-Base"
      const val ETAG = "ETag"
      const val EXPIRES = "Expires"
      const val IM = "IM"
      const val LAST_MODIFIED = "Last-Modified"
      const val LINK = "Link"
      const val LOCATION = "Location"
      const val P3P = "P3P"
      const val PRAGMA = "Pragma"
      const val PROXY_AUTHENTICATE = "Proxy-Authenticate"
      const val PUBLIC_KEY_PINS = "Public-Key-Pins"
      const val RETRY_AFTER = "Retry-After"
      const val SERVER = "HTTPServer"
      const val SET_COOKIE = "Set-Cookie"
      const val STRICT_TRANSFER_SECURITY = "Strict-Transfer-Security"
      const val TRAILER = "Trailer"
      const val TRANSFER_ENCODING = "Transfer-Encoding"
      const val TK = "Tk"
      const val UPGRADE = "Upgrade"
      const val VARY = "Vary"
      const val VIA = "Via"
      const val WARNING = "Warning"
      const val WWW_AUTHENTICATE = "WWW-Authenticate"
      const val X_FRAME_OPTIONS = "X-Frame-Options"
    }
  }

  class ContentType {
    companion object {
      const val PLAINT_TEXT = "text/plain"
      const val HTML = "text/html"
      const val CSS = "text/css"
      const val JAVASCRIPT = "application/javascript"
      const val JSON = "application/json"
      const val XML = "application/xml"
      const val JPEG = "image/jpeg"
      const val PNG = "image/png"
      const val CSV = "text/csv"
      const val OCTET_STREAM = "application/octet-stream"
    }
  }

  companion object {
    fun parseProtocol(value: String) = when (value) {
      "HTTP/1.0" -> Protocol.HTTP10
      "HTTP/1.1" -> Protocol.HTTP11
      else -> throw HTTPException(505, "Unknown protocol $value")
    }

    fun parseMethod(value: String) =
            Method.values().firstOrNull { it.name == value } ?: throw HTTPException(400, "Unknown method $value")
  }
}
