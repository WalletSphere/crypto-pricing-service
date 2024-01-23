package com.khomishchak.cryptopricingservice.service.ws

import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionMappingService {

    private val idToSessionMap = ConcurrentHashMap<Long, WebSocketSession>()
    private val sessionToIdMap = ConcurrentHashMap<WebSocketSession, Long>()

    fun registerSession(session: WebSocketSession, accountId: Long) {
        idToSessionMap[accountId] = session
        sessionToIdMap[session] = accountId
    }

    fun removeSession(accountId: Long) {
        idToSessionMap[accountId].let {
            idToSessionMap[accountId]
            sessionToIdMap[it]
        }
    }

    fun getSession(accountId: Long) = idToSessionMap[accountId]
    fun getUserId(session: WebSocketSession) = sessionToIdMap[session] ?: 0L

    fun sendMessageToSession(accountId: Long, message: String) =
            getSession(accountId)?.takeIf { it.isOpen
            }?.sendMessage(TextMessage(message))

    fun sendMessageToSession(session: WebSocketSession, message: String) = session.sendMessage(TextMessage(message))
}