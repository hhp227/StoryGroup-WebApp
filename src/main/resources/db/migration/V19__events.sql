-- 그룹 일정 (그룹 상세 상단 "일정" 탭 + 캘린더 페이지 + 사이드바 "다가오는 일정" 패널)
--
-- 의미론:
--  - 일정은 그룹 멤버 누구나 만들 수 있다. 수정은 작성자만, 삭제는 작성자 또는 방장/부방장(댓글과 동일).
--  - RSVP(참석/미정/불참)는 일정당 멤버 하나(UNIQUE), 변경은 upsert. 작성자는 생성 시 자동 참석 처리.
--  - 차단은 게시글과 동일한 단방향 콘텐츠 숨김: 내가 차단한 사용자가 만든 일정은 내 목록에서 숨긴다(쿼리 레벨).
--    참석자 명단은 멤버 목록과 같은 성격이라 차단 여부와 무관하게 표시한다.

CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    location    VARCHAR(200),
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CHECK (ends_at IS NULL OR ends_at >= starts_at)
);

-- 캘린더 월 범위 조회와 "다가오는 일정" 조회 공용.
CREATE INDEX idx_events_group_starts ON events(group_id, starts_at) WHERE deleted_at IS NULL;

CREATE TABLE event_rsvps (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status     VARCHAR(16) NOT NULL CHECK (status IN ('GOING', 'MAYBE', 'NOT_GOING')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id)
);

-- RLS는 2차 방어선 문서화용(앱 DB 롤은 테이블 소유자라 RLS를 우회한다 - 실제 접근 제어는
-- 서비스 레이어의 requireMembership과 쿼리 레벨 필터가 담당).
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_rsvps ENABLE ROW LEVEL SECURITY;

CREATE POLICY events_select ON events
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

CREATE POLICY events_insert ON events
    FOR INSERT WITH CHECK (
        is_group_member(group_id, current_app_user_id())
        AND user_id = current_app_user_id()
    );

CREATE POLICY events_update ON events
    FOR UPDATE USING (user_id = current_app_user_id());

CREATE POLICY events_delete ON events
    FOR DELETE USING (user_id = current_app_user_id());

CREATE POLICY event_rsvps_select ON event_rsvps
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM events e
            WHERE e.id = event_id AND is_group_member(e.group_id, current_app_user_id())
        )
    );

CREATE POLICY event_rsvps_write ON event_rsvps
    FOR ALL USING (user_id = current_app_user_id())
    WITH CHECK (user_id = current_app_user_id());
