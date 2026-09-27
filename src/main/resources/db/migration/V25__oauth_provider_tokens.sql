-- 애플 로그인(설계 2026-09-27 §2.5): 탈퇴 시 /auth/revoke에 원문 refresh_token이 필요해 평문 저장.
-- client_id는 토큰을 받은 앱(iOS 번들 ID 또는 Services ID) — 폐기 때 같은 값으로 client_secret을 만든다
ALTER TABLE user_oauth_accounts ADD COLUMN refresh_token TEXT NULL;
ALTER TABLE user_oauth_accounts ADD COLUMN client_id VARCHAR(255) NULL;
