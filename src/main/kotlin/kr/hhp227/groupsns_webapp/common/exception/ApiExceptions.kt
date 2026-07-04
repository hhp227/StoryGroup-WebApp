package kr.hhp227.groupsns_webapp.common.exception

class DuplicateEmailException : RuntimeException("이미 가입된 이메일입니다")

class InvalidCredentialsException : RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다")

class InvalidRefreshTokenException : RuntimeException("유효하지 않거나 만료된 리프레시 토큰입니다")

// 존재하지 않거나 요청자가 멤버가 아닌 그룹. 폐쇄형 그룹 특성상 비멤버에게는 존재 자체를 숨기기 위해
// 권한 없음(403)이 아니라 404로 통일한다.
class GroupNotFoundException : RuntimeException("그룹을 찾을 수 없습니다")

class GroupMemberNotFoundException : RuntimeException("그룹 멤버를 찾을 수 없습니다")

class ForbiddenException(message: String = "이 작업을 수행할 권한이 없습니다") : RuntimeException(message)

class InvalidInviteException : RuntimeException("유효하지 않거나 만료된 초대 코드입니다")

class AlreadyMemberException : RuntimeException("이미 가입된 그룹입니다")

// 존재하지 않거나 다른 그룹 소속인 게시글. 그룹과 마찬가지로 비멤버에게는 403이 아니라 404로 통일한다.
class PostNotFoundException : RuntimeException("게시글을 찾을 수 없습니다")

// 존재하지 않거나 다른 게시글 소속인 댓글(대댓글의 부모 댓글 포함). Post/Group과 마찬가지로 404로 통일한다.
class CommentNotFoundException : RuntimeException("댓글을 찾을 수 없습니다")

class AlreadyLikedException : RuntimeException("이미 좋아요를 누른 게시글입니다")

// 존재하지 않거나 다른 그룹 소속인 채팅방. Post/Group과 마찬가지로 404로 통일한다.
class ChatRoomNotFoundException : RuntimeException("채팅방을 찾을 수 없습니다")

// 존재하지 않거나 다른 채팅방 소속인 메시지.
class MessageNotFoundException : RuntimeException("메시지를 찾을 수 없습니다")

// 존재하지 않거나 다른 그룹 소속인 회의.
class MeetingNotFoundException : RuntimeException("회의를 찾을 수 없습니다")
