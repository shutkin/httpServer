package me.shutkin.muzhvay.http

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.*

class HTTPRequest(val method: HTTP.Method, val path: String, val protocol: HTTP.Protocol, private val headers: Map<String, String>) {
  fun getHeader(header: String): Map<String?, String> {
    if (headers[header] == null)
      return emptyMap()
    return parseHeaderValue(headers[header] ?: "")
  }

  data class MultipartEntity(val contentType: String, val name: String?, val filename: String?, val data: ByteArray) {
    override fun toString(): String {
      return "MultipartEntity(contentType='$contentType', name=$name, filename=$filename, data.size=${data.size})"
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as MultipartEntity

      if (contentType != other.contentType) return false
      if (name != other.name) return false
      if (filename != other.filename) return false

      return true
    }

    override fun hashCode(): Int {
      var result = contentType.hashCode()
      result = 31 * result + (name?.hashCode() ?: 0)
      result = 31 * result + (filename?.hashCode() ?: 0)
      return result
    }
  }

  fun readMultipartBody(inputStream: InputStream): List<MultipartEntity> {
    val contentTypeHeader = getHeader(HTTP.RequestFields.CONTENT_TYPE)
    if (contentTypeHeader[null] != "multipart/form-data")
      throw HTTPException(400, "Content type must be 'multipart/form-data'")
    val boundary = contentTypeHeader["boundary"] ?: throw HTTPException(400, "Multipart without 'boundary' value")
    val result = LinkedList<MultipartEntity>()
    val stream = BufferedInputStream(inputStream)
    readMultipartEntity(stream, boundary, false) // preamble
    while (true) {
      val c1 = stream.read()
      if (c1 < 0)
        throw HTTPException(400, "Unexpected end of stream after boundary in multipart body")
      val c2 = stream.read()
      if (c2 < 0)
        throw HTTPException(400, "Unexpected end of stream after boundary in multipart body")
      if (c1.toChar() == '-' && c2.toChar() == '-')
        break
      if (c1.toChar() != '\r' || c2.toChar() != '\n')
        throw HTTPException(400, "No new line after boundary in multipart body")
      result.add(readMultipartEntity(stream, boundary))
    }
    return result
  }

  override fun toString(): String {
    return "HTTPRequest(method=$method, path='$path', protocol=$protocol, headers=$headers)"
  }

  companion object {
    fun read(stream: InputStream): HTTPRequest {
      val requestLine = readHeaderLine(stream)
      val requestLineParts = requestLine.split(' ')
      if (requestLineParts.size != 3)
        throw HTTPException(400, "Invalid request line '$requestLine'")
      val headers = HashMap<String, String>()
      while (true) {
        val headerLine = readHeaderLine(stream)
        if (headerLine.isEmpty())
          return HTTPRequest(HTTP.parseMethod(requestLineParts[0]), requestLineParts[1], HTTP.parseProtocol(requestLineParts[2]), headers)
        val headerKeyValue = splitHeaderLine(headerLine)
        headers[headerKeyValue.first] = headerKeyValue.second
      }
    }
  }
}

private fun readMultipartEntity(stream: InputStream, boundary: String, readHeaders: Boolean = true): HTTPRequest.MultipartEntity {
  var contentType = HTTP.ContentType.OCTET_STREAM
  var name: String? = null
  var filename: String? = null
  while (readHeaders) {
    val headerLine = readHeaderLine(stream)
    if (headerLine.isEmpty())
      break
    val headerKeyValue = splitHeaderLine(headerLine)
    when (headerKeyValue.first) {
      HTTP.RequestFields.CONTENT_TYPE -> contentType = headerKeyValue.second
      HTTP.RequestFields.CONTENT_DISPOSITION -> {
        val headerValues = parseHeaderValue(headerKeyValue.second)
        if (headerValues[null] != "form-data")
          throw HTTPException(400, "Content-Disposition must be 'form-data'")
        name = headerValues["name"]
        filename = headerValues["filename"]
      }
    }
  }
  val data = ArrayList<Byte>()
  val boundaryLastByte = boundary[boundary.length - 1].toInt()
  while (true) {
    val i = stream.read()
    if (i < 0)
      throw HTTPException(400, "Unexpected end of stream while reading multipart entity body")
    data.add(i.toByte())
    if (i == boundaryLastByte && data.size >= boundary.length) {
      val cnt = boundary.filterIndexed { index, c ->
        data[data.size - boundary.length + index] != c.toByte()
      }.count()
      if (cnt == 0)
        break
    }
  }
  val size = data.size - boundary.length - 4
  return HTTPRequest.MultipartEntity(contentType, name, filename,
          if (size > 0) data.subList(0, size).toByteArray() else ByteArray(0))
}

private fun readHeaderLine(stream: InputStream): String {
  val builder = StringBuilder()
  while (true) {
    val r = stream.read()
    if (r < 0)
      throw HTTPException(400, "Unexpected end of stream while reading header line")
    if (r == '\n'.toInt())
      return builder.toString()
    if (r != '\r'.toInt())
      builder.append(r.toChar())
  }
}

private fun splitHeaderLine(headerLine: String): Pair<String, String> {
  val colonIndex = headerLine.indexOf(':')
  if (colonIndex < 0)
    throw HTTPException(400, "Invalid header line '$headerLine'")
  return Pair(headerLine.substring(0, colonIndex).trim(), headerLine.substring(colonIndex + 1).trim())
}

private fun parseHeaderValue(headerValue: String): Map<String?, String> {
  val result = HashMap<String?, String>()
  val closedHeader = "$headerValue;"
  var nameStartIndex = 0
  var nameEndIndex = -1
  var valueStartIndex = -1
  var valueEndIndex: Int
  var isInQuotes = false
  closedHeader.forEachIndexed { index, c ->
    if (c == '"')
      isInQuotes = !isInQuotes
    if (!isInQuotes) {
      if (c == '=') {
        nameEndIndex = index - 1
        valueStartIndex = index + 1
      } else if (c == ';') {
        if (valueStartIndex < 0) {
          nameEndIndex = index - 1
          if (nameEndIndex <= nameStartIndex)
            throw HTTPException(400, "Invalid headerValue $headerValue")
          result[null] = closedHeader.substring(nameStartIndex, nameEndIndex + 1).trim().trim('"')
        } else {
          valueEndIndex = index - 1
          if (nameEndIndex <= nameStartIndex || valueEndIndex <= nameStartIndex)
            throw HTTPException(400, "Invalid headerValue $headerValue")
          result[closedHeader.substring(nameStartIndex, nameEndIndex + 1).trim().trim('"')] =
                  closedHeader.substring(valueStartIndex, valueEndIndex + 1).trim().trim('"')
        }
        nameStartIndex = index + 1
        nameEndIndex = -1
        valueStartIndex = -1
        valueEndIndex = -1
      }
    }
  }
  return result
}