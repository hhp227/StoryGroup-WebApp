package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.user.dto.PublicProfileResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// 공개 프로필 조회. UserController가 /api/users/me로 클래스 매핑되어 있어 분리 —
// /api/users/me는 리터럴 매칭이 우선이라 {userId} 패턴과 충돌하지 않는다.
@RestController
class PublicProfileController(private val userService: UserService) {

    @GetMapping("/api/users/{userId}")
    fun getPublicProfile(@PathVariable userId: Long): PublicProfileResponse =
        userService.getPublicProfile(userId)
}
