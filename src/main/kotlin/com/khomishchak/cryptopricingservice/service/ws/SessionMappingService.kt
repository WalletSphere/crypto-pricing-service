package com.khomishchak.cryptopricingservice.service.ws

import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionMappingService {
    private val sessionMap = ConcurrentHashMap<Long, ConcurrentHashMap<String, WebSocketSession>>()

    fun registerSession(session: WebSocketSession, accountId: Long) {
        val sessions = sessionMap.computeIfAbsent(accountId) { ConcurrentHashMap() }
        sessions[session.id] = session
    }

    // TODO: implement remove session functionality
    fun removeSession(accountId: Long) = sessionMap.remove(accountId)

    fun getSession(accountId: Long): WebSocketSession? = sessionMap[accountId]?.values?.toList()?.get(0);

    fun sendMessageToSession(accountId: Long, message: String) {
        val session = getSession(accountId)
        session?.let {
            if (it.isOpen) {
                it.sendMessage(TextMessage(message))
            }
        }
    }
}