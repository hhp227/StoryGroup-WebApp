-- Group API 설계 시 owner/member 2단계 권한 모델로 확정(부관리자·가입승인 큐는 이후 단계로 미룸).
-- 기존 user_groups.status는 owner/가입요청 의미를 겹쳐쓰던 레거시 플래그라 새 role 컬럼으로 대체.
-- status는 레거시 호환을 위해 남겨두되 신규 코드에서는 더 이상 사용하지 않는다.
ALTER TABLE user_groups ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE user_groups ADD CONSTRAINT chk_user_groups_role CHECK (role IN ('OWNER', 'MEMBER'));
