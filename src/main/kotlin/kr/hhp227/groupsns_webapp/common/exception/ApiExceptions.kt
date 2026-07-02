package kr.hhp227.groupsns_webapp.common.exception

class DuplicateEmailException : RuntimeException("이미 가입된 이메일입니다")

class InvalidCredentialsException : RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다")

class InvalidRefreshTokenException : RuntimeException("유효하지 않거나 만료된 리프레시 토큰입니다")
