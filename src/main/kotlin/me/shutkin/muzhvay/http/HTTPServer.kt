package me.shutkin.muzhvay.http

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class HTTPServer(private val port: Int, val log: HTTPLogger) {
  private val executor = Executors.newCachedThreadPool()
  private val sessionsContainer = ConcurrentHashMap<String, HTTPSession>()
  private val processors = HashMap<String, (HTTPRequest, HTTPSession, InputStream) -> HTTPResponse>()
  private var sessionKillListener: (HTTPSession) -> Unit = {}

  init {
    processors["/"] = this::processRequestRoot
  }

  /**
   * Setting the method which will be invoked by the server to construct a response to request with the most appropriate path.
   * Processors with the same path will override each other
   */
  fun setRequestProcessor(path: String, processor: (HTTPRequest, HTTPSession, InputStream) -> HTTPResponse) {
    processors[path] = processor
  }

  /**
   * Settings listener which will be invoked when session is removed by timeout. The listener may be only one per server.
   */
  fun setSessionKillListener(listener: (HTTPSession) -> Unit) {
    sessionKillListener = listener
  }

  /**
   * Force delete specific session. Session killer listener will be invoked by this method.
   */
  fun killSession(sessionId: String) {
    val session = sessionsContainer[sessionId]
    if (session != null) {
      sessionsContainer.remove(sessionId)
      sessionKillListener(session)
    }
  }

  /**
   * Start HTTP server. This method blocks until thread is interrupted
   */
  fun run() {
    val sessionsKillThread = thread {
      while (!Thread.interrupted()) {
        val killTime = System.currentTimeMillis() - HTTPSession.LIVE_TIME
        val filtered = sessionsContainer.filter { it.value.started < killTime }
        filtered.forEach {
          sessionsContainer.remove(it.key)
          sessionKillListener(it.value)
        }
        Thread.sleep(1000)
      }
    }

    val serverSocket = ServerSocket(port)
    log.info("HTTPServer started")
    while (!Thread.interrupted()) {
      try {
        val socket = serverSocket.accept()
        executor.submit { handleClientCall(socket) }
      } catch (e: Exception) {
        log.error(e)
      }
    }
    sessionsKillThread.interrupt()
  }

  private fun handleClientCall(socket: Socket) {
    log.info("Connection accepted with client socket: $socket")
    socket.use {
      socket.getInputStream().use { inputStream ->
        socket.getOutputStream().use { outputStream ->
          val response =
                  try {
                    val request = HTTPRequest.read(inputStream)
                    log.info("Request: $request")
                    processRequest(request, inputStream)
                  } catch (e: HTTPException) {
                    log.error(e)
                    val message = e.message ?: "Unknown HTTP processing error"
                    HTTPResponse(null, null, e.code, message.toByteArray().inputStream(), HTTP.ContentType.PLAINT_TEXT, message.length)
                  } catch (e: Exception) {
                    log.error(e)
                    val message = e.message ?: "Unknown HTTP processing error"
                    HTTPResponse(null, null, 500, message.toByteArray().inputStream(), HTTP.ContentType.PLAINT_TEXT, message.length)
                  }
          log.info(response.toString())
          if (response.session != null && response.request != null &&
                  response.session.sessionId != response.request.getHeader(HTTP.RequestFields.COOKIE)[HTTPSession.COOKIE_NAME])
            response.cookies.add(HTTPCookie(HTTPSession.COOKIE_NAME, response.session.sessionId, HTTPSession.LIVE_TIME, true))
          outputStream.write(buildResponseHeaders(response).toByteArray())
          if (response.contentSize > 0)
            copyStreams(response.body, outputStream)
        }
      }
    }
  }

  private fun copyStreams(input: InputStream, output: OutputStream) {
    val buf = ByteArray(4096)
    while (true) {
      val r = input.read(buf)
      if (r < 0)
        break
      output.write(buf, 0, r)
    }
    input.close()
  }

  private fun findSession(request: HTTPRequest): HTTPSession {
    val cookies = request.getHeader(HTTP.RequestFields.COOKIE)
    val cookieSessionId = cookies[HTTPSession.COOKIE_NAME]
    val cookieSession = if (cookieSessionId != null && sessionsContainer.containsKey(cookieSessionId)) sessionsContainer[cookieSessionId] else null
    if (cookieSession != null)
      return cookieSession
    val newSession = HTTPSession.generate()
    sessionsContainer[newSession.sessionId] = newSession
    log.info("Created session ${newSession.sessionId}")
    return newSession
  }

  private fun processRequest(request: HTTPRequest, stream: InputStream): HTTPResponse {
    val session = findSession(request)
    val processorKey = processors.keys.filter { request.path.startsWith(it) }.sortedByDescending { it.length }[0]
    return processors[processorKey]?.invoke(request, session, stream)
            ?: throw HTTPException(404, "Resource '${request.path}' not found")
  }

  private fun processRequestRoot(request: HTTPRequest, session: HTTPSession, stream: InputStream): HTTPResponse {
    if (request.path != "/")
      throw HTTPException(404, "Resource '${request.path}' not found")
    else
      return HTTPResponse(request, session, "HTTPServer is working")
  }

  private fun buildResponseHeaders(response: HTTPResponse) = "" +
          "HTTP/1.1 ${response.code} ${if (response.code == 200) "OK" else "ERROR"}\r\n" +
          "${HTTP.ResponseFields.SERVER}: Muzhvay 0.1\r\n" +
          response.cookies.joinToString(separator = "\r\n") {
            "${HTTP.ResponseFields.SET_COOKIE}: ${it.name}=${it.value}; Max-Age=${it.maxAge}" +
                    (if (it.isHttpOnly) "; HttpOnly" else "")
          } +
          "${HTTP.ResponseFields.CONTENT_LENTH}: ${response.contentSize}\r\n" +
          "${HTTP.ResponseFields.CONTENT_TYPE}: ${response.contentType}\r\n" +
          "${HTTP.ResponseFields.CONNECTION}: close\r\n" +
          "\r\n"
}