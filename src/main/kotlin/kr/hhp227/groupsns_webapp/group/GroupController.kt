package kr.hhp227.groupsns_webapp.group

import kr.hhp227.groupsns_webapp.group.dto.CreateGroupRequest
import kr.hhp227.groupsns_webapp.group.dto.CreateInviteRequest
import kr.hhp227.groupsns_webapp.group.dto.DiscoverGroupResponse
import kr.hhp227.groupsns_webapp.group.dto.GroupResponse
import kr.hhp227.groupsns_webapp.group.dto.InviteResponse
import kr.hhp227.groupsns_webapp.group.dto.JoinGroupResponse
import kr.hhp227.groupsns_webapp.group.dto.JoinRequestResponse
import kr.hhp227.groupsns_webapp.group.dto.MemberResponse
import kr.hhp227.groupsns_webapp.group.dto.UpdateGroupRequest
import kr.hhp227.groupsns_webapp.group.dto.UpdateMemberRoleRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/groups")
class GroupController(private val groupService: GroupService) {

    @PostMapping
    fun createGroup(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateGroupRequest
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(principal.id, request))

    // page/size 미지정 시 전체 반환 — 기존 클라이언트(웹, 앱 라운지 해석)와의 하위호환.
    // 지정 시 페이징 — 레거시 user_groups?offset&load_size 계약의 복원(앱 그룹 탭 무한스크롤).
    @GetMapping
    fun listMyGroups(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): List<GroupResponse> = groupService.listMyGroups(principal.id, page, size)

    @GetMapping("/{groupId}")
    fun getGroup(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): GroupResponse =
        groupService.getGroup(principal.id, groupId)

    @PatchMapping("/{groupId}")
    fun updateGroup(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupRequest
    ): GroupResponse = groupService.updateGroup(principal.id, groupId, request)

    @DeleteMapping("/{groupId}")
    fun deleteGroup(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): ResponseEntity<Void> {
        groupService.deleteGroup(principal.id, groupId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{groupId}/invites")
    fun createInvite(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateInviteRequest
    ): ResponseEntity<InviteResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.createInvite(principal.id, groupId, request))

    @PostMapping("/join/{code}")
    fun joinByCode(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable code: String): GroupResponse =
        groupService.joinByCode(principal.id, code)

    // 그룹 탐색 - 미가입 그룹을 포함한 전체 그룹 목록(라운지 제외). 문자열 경로라 /{groupId}보다 우선 매칭된다.
    @GetMapping("/discover")
    fun discoverGroups(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "recent") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<DiscoverGroupResponse> = groupService.discoverGroups(principal.id, query, sort, page, size)

    @PostMapping("/{groupId}/join")
    fun joinGroup(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): JoinGroupResponse =
        groupService.joinGroup(principal.id, groupId)

    @DeleteMapping("/{groupId}/join")
    fun cancelJoinRequest(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): ResponseEntity<Void> {
        groupService.cancelJoinRequest(principal.id, groupId)
        return ResponseEntity.noContent().build()
    }

    // 내가 가입 신청중(PENDING)인 그룹 목록 - 문자열 경로라 /{groupId}/join-requests와 충돌하지 않는다.
    @GetMapping("/join-requests/mine")
    fun listMyJoinRequestedGroups(@AuthenticationPrincipal principal: UserPrincipal): List<DiscoverGroupResponse> =
        groupService.listMyJoinRequestedGroups(principal.id)

    @GetMapping("/{groupId}/join-requests")
    fun listJoinRequests(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long
    ): List<JoinRequestResponse> = groupService.listJoinRequests(principal.id, groupId)

    @PostMapping("/{groupId}/join-requests/{userId}/approve")
    fun approveJoinRequest(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        groupService.approveJoinRequest(principal.id, groupId, userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{groupId}/join-requests/{userId}")
    fun rejectJoinRequest(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        groupService.rejectJoinRequest(principal.id, groupId, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{groupId}/members")
    fun listMembers(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): List<MemberResponse> =
        groupService.listMembers(principal.id, groupId)

    @DeleteMapping("/{groupId}/members/{userId}")
    fun kickMember(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        groupService.kickMember(principal.id, groupId, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{groupId}/members/{userId}/role")
    fun updateMemberRole(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateMemberRoleRequest
    ): ResponseEntity<Void> {
        groupService.updateMemberRole(principal.id, groupId, userId, request.role)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{groupId}/leave")
    fun leaveGroup(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): ResponseEntity<Void> {
        groupService.leaveGroup(principal.id, groupId)
        return ResponseEntity.noContent().build()
    }
}
