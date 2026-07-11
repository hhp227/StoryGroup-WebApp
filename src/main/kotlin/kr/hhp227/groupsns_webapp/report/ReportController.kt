package kr.hhp227.groupsns_webapp.report

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Size

data class ReportUserRequest(
    // 신고 사유는 선택 — 웹 MVP는 사유 없이 접수하고, 필드는 추후 신고 관리 UI를 위해 열어둔다.
    @field:Size(max = 500) val reason: String? = null
)

@RestController
class ReportController(private val reportService: ReportService) {

    @PostMapping("/api/users/{userId}/report")
    fun reportUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long,
        @Valid @RequestBody(required = false) request: ReportUserRequest?
    ): ResponseEntity<Void> {
        reportService.reportUser(principal.id, userId, request?.reason)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
