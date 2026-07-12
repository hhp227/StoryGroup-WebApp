package kr.hhp227.groupsns_webapp.report

import kr.hhp227.groupsns_webapp.report.dto.ProcessReportRequest
import kr.hhp227.groupsns_webapp.report.dto.UserReportResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

// 앱 운영자(users.role = ADMIN) 전용 - 사용자 신고 관리. 인가는 서비스 레이어(requireAdmin)가 담당한다.
@RestController
@RequestMapping("/api/admin/user-reports")
class AdminReportController(private val reportService: ReportService) {

    @GetMapping
    fun listUserReports(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<UserReportResponse> =
        reportService.listUserReports(principal.id, status, page, size.coerceIn(1, 50))

    @PatchMapping("/{reportId}")
    fun processUserReport(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable reportId: Long,
        @Valid @RequestBody request: ProcessReportRequest
    ): UserReportResponse = reportService.processUserReport(principal.id, reportId, request.status)
}
