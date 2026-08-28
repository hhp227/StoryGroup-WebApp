package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.push.dto.RegisterPushTokenRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushTokenService(private val pushTokenMapper: PushTokenMapper) {
    @Transactional
    fun register(userId: Long, request: RegisterPushTokenRequest) {
        pushTokenMapper.upsert(userId, request.platform.name, request.token)
    }

    @Transactional
    fun unregister(token: String) {
        pushTokenMapper.deleteByToken(token)
    }
}
