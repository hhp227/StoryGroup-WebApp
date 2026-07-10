-- 사용자 차단 (설정 > 차단 사용자 관리)
--
-- 의미론: 차단은 단방향 콘텐츠 숨김 + 양방향 DM 차단.
--  - 내가 차단한 사용자의 게시글/댓글/채팅 메시지/파일이 내 화면에서 숨겨진다(쿼리 레벨 필터,
--    상대 화면에는 영향 없음). 멤버 목록에는 계속 표시된다 - 같은 그룹의 구성원임은 사실이므로.
--  - DM은 어느 쪽이 차단했든 방 생성/메시지 전송이 막힌다(403 BLOCKED).
--  - 차단한 사용자가 일으킨 알림(댓글/좋아요 등)은 생성 시점에 걸러진다.

CREATE TABLE user_blocks (
    id         BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
-- 양방향 조회(DM 차단 검사)용 역방향 인덱스. 정방향은 UNIQUE 제약이 커버한다.
CREATE INDEX idx_user_blocks_blocked ON user_blocks(blocked_id);

ALTER TABLE user_blocks ENABLE ROW LEVEL SECURITY;

-- 차단 목록은 차단한 본인만 보고 관리한다. 차단 "당한" 쪽에는 존재 자체를 노출하지 않는다.
CREATE POLICY user_blocks_select ON user_blocks
    FOR SELECT USING (blocker_id = current_app_user_id());

CREATE POLICY user_blocks_insert ON user_blocks
    FOR INSERT WITH CHECK (blocker_id = current_app_user_id());

CREATE POLICY user_blocks_delete ON user_blocks
    FOR DELETE USING (blocker_id = current_app_user_id());
