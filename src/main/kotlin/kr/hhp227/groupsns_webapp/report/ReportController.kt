package kr.hhp227.groupsns_webapp.report

import kr.hhp227.groupsns_webapp.report.dto.PostReportResponse
import kr.hhp227.groupsns_webapp.report.dto.ProcessReportRequest
import kr.hhp227.groupsns_webapp.report.dto.ReportRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class ReportController(private val reportService: ReportService) {

    @PostMapping("/api/users/{userId}/report")
    fun reportUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long,
        @Valid @RequestBody(required = false) request: ReportRequest?
    ): ResponseEntity<Void> {
        reportService.reportUser(principal.id, userId, request?.reason)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/api/groups/{groupId}/posts/{postId}/report")
    fun reportPost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @Valid @RequestBody(required = false) request: ReportRequest?
    ): ResponseEntity<Void> {
        reportService.reportPost(principal.id, groupId, postId, request?.reason)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    // 그룹 신고함(방장/부방장 전용). status 미지정이면 전체.
    @GetMapping("/api/groups/{groupId}/reports")
    fun listGroupReports(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<PostReportResponse> =
        reportService.listGroupReports(principal.id, groupId, status, page, size.coerceIn(1, 50))

    @PatchMapping("/api/groups/{groupId}/reports/{reportId}")
    fun processGroupReport(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable reportId: Long,
        @Valid @RequestBody request: ProcessReportRequest
    ): PostReportResponse = reportService.processGroupReport(principal.id, groupId, reportId, request.status)
}
