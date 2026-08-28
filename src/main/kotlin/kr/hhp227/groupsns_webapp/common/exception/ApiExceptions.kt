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

class AlreadyRequestedException : RuntimeException("이미 가입 신청한 그룹입니다")

// 존재하지 않거나 이미 처리(승인/거절/취소)된 가입 신청.
class JoinRequestNotFoundException : RuntimeException("가입 신청을 찾을 수 없습니다")

// 존재하지 않거나 다른 그룹 소속인 게시글. 그룹과 마찬가지로 비멤버에게는 403이 아니라 404로 통일한다.
class PostNotFoundException : RuntimeException("게시글을 찾을 수 없습니다")

// 존재하지 않거나 다른 게시글 소속인 댓글(대댓글의 부모 댓글 포함). Post/Group과 마찬가지로 404로 통일한다.
class CommentNotFoundException : RuntimeException("댓글을 찾을 수 없습니다")

class AlreadyLikedException : RuntimeException("이미 좋아요를 누른 게시글입니다")

// 존재하지 않거나 다른 그룹 소속인 일정. Post/Group과 마찬가지로 404로 통일한다.
class EventNotFoundException : RuntimeException("일정을 찾을 수 없습니다")

// 존재하지 않거나 다른 그룹 소속인 채팅방. Post/Group과 마찬가지로 404로 통일한다.
class ChatRoomNotFoundException : RuntimeException("채팅방을 찾을 수 없습니다")

// 존재하지 않거나 다른 채팅방 소속인 메시지.
class MessageNotFoundException : RuntimeException("메시지를 찾을 수 없습니다")

// 존재하지 않거나 다른 그룹 소속인 회의.
class MeetingNotFoundException : RuntimeException("회의를 찾을 수 없습니다")

// 존재하지 않거나 다른 그룹 소속인 파일. java.io.FileNotFoundException과의 혼동을 피하기 위해 명명.
class GroupFileNotFoundException : RuntimeException("파일을 찾을 수 없습니다")

// 존재하지 않거나 다른 사용자 소속인 알림.
class NotificationNotFoundException : RuntimeException("알림을 찾을 수 없습니다")

// JWT principal은 있는데 그 시점 유저가 탈퇴 등으로 사라진 경우에 대한 방어적 처리(정상 흐름에선 발생 안 함).
class UserNotFoundException : RuntimeException("사용자를 찾을 수 없습니다")

class AlreadyBlockedException : RuntimeException("이미 차단한 사용자입니다")

class AlreadyFriendException : RuntimeException("이미 친구로 등록한 사용자입니다")

// 사용자/게시글 신고 공용 - "대기중" 신고가 이미 있는 경우(처리된 뒤에는 재신고 가능).
class AlreadyReportedException(message: String = "이미 신고한 사용자입니다") : RuntimeException(message)

// 존재하지 않거나 다른 그룹 소속인 신고 내역. Post/Group과 마찬가지로 404로 통일한다.
class ReportNotFoundException : RuntimeException("신고 내역을 찾을 수 없습니다")

// 존재하지 않거나 이미 해제된 친구 등록.
class FriendNotFoundException : RuntimeException("친구 등록 내역을 찾을 수 없습니다")

// 존재하지 않거나 이미 해제된 차단 내역.
class BlockNotFoundException : RuntimeException("차단 내역을 찾을 수 없습니다")

// 차단 관계에서 막히는 상호작용(DM 방 생성/메시지 전송). 어느 쪽이 차단했는지는 노출하지 않는다.
class BlockedUserException : RuntimeException("차단 관계인 사용자에게는 보낼 수 없습니다")

// 회원탈퇴 시 소유하는 그룹이 존재하는 경우. 그룹을 먼저 삭제하고 탈퇴해야 한다(설계 §2).
class OwnedGroupsExistException(groupNames: List<String>) :
    RuntimeException("'${groupNames.joinToString(", ")}' 그룹을 삭제한 후 탈퇴할 수 있습니다")
