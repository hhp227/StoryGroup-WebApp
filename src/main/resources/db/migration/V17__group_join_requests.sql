-- 그룹 탐색/가입 기능 (레거시 승인제 복원)
--
-- 레거시 join_type 의미를 복원한다: 0=자동 승인(탐색에서 바로 가입), 1=승인제(가입 신청 후
-- 방장/부방장이 승인). 초대 코드 가입(joinByCode)은 join_type과 무관하게 계속 동작한다.
--
-- 신청 테이블은 "대기 중" 상태만 보관한다 - 승인되면 user_groups로 옮기고 행을 지우며,
-- 거절/취소도 행 삭제로 처리한다(이력 불필요, 재신청 가능).

CREATE TABLE group_join_requests (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);
CREATE INDEX idx_group_join_requests_group_id ON group_join_requests(group_id, created_at);

-- 그룹 탐색 목록의 멤버 수 표시용. user_groups는 멤버만 SELECT할 수 있는 RLS가 걸려 있어
-- 비멤버가 보는 탐색 화면에서는 일반 COUNT가 0이 되므로, is_group_member()와 같은
-- SECURITY DEFINER 우회 함수로 인원수만 노출한다.
CREATE OR REPLACE FUNCTION group_member_count(p_group_id BIGINT) RETURNS BIGINT AS $$
    SELECT COUNT(*) FROM user_groups WHERE group_id = p_group_id
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;

ALTER TABLE group_join_requests ENABLE ROW LEVEL SECURITY;

-- 신청자 본인(내 신청 상태 확인)과 그룹 모더레이터(승인/거절 목록)만 조회할 수 있다.
CREATE POLICY group_join_requests_select ON group_join_requests
    FOR SELECT USING (
        user_id = current_app_user_id()
        OR is_group_moderator(group_id, current_app_user_id())
    );

CREATE POLICY group_join_requests_insert ON group_join_requests
    FOR INSERT WITH CHECK (user_id = current_app_user_id());

-- 본인 취소 또는 모더레이터의 승인/거절 처리.
CREATE POLICY group_join_requests_delete ON group_join_requests
    FOR DELETE USING (
        user_id = current_app_user_id()
        OR is_group_moderator(group_id, current_app_user_id())
    );
