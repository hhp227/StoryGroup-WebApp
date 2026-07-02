package kr.hhp227.groupsns_webapp.group

import kr.hhp227.groupsns_webapp.group.dto.CreateGroupRequest
import kr.hhp227.groupsns_webapp.group.dto.CreateInviteRequest
import kr.hhp227.groupsns_webapp.group.dto.GroupResponse
import kr.hhp227.groupsns_webapp.group.dto.InviteResponse
import kr.hhp227.groupsns_webapp.group.dto.MemberResponse
import kr.hhp227.groupsns_webapp.group.dto.UpdateGroupRequest
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

    @GetMapping
    fun listMyGroups(@AuthenticationPrincipal principal: UserPrincipal): List<GroupResponse> =
        groupService.listMyGroups(principal.id)

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

    @PostMapping("/{groupId}/leave")
    fun leaveGroup(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): ResponseEntity<Void> {
        groupService.leaveGroup(principal.id, groupId)
        return ResponseEntity.noContent().build()
    }
}
