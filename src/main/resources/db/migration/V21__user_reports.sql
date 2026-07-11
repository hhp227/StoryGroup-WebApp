-- 사용자 신고 (게시글 상세 작성자 메뉴 > 사용자 신고)
--
-- 접수만 저장하는 MVP: 같은 상대에 대한 신고는 한 번만(UNIQUE) — PRD의 "신고 관리"(관리자) 기능이
-- 생기면 처리 상태(접수/처리완료)와 재신고 정책을 그때 재설계한다.
-- 참고: post_reports(게시글 신고)는 V1부터 있지만 이것은 "사용자" 신고라 별도 테이블.

CREATE TABLE user_reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason      VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, reported_id),
    CHECK (reporter_id <> reported_id)
);

-- 관리자 신고 관리(추후)에서 피신고자별 집계용.
CREATE INDEX idx_user_reports_reported ON user_reports(reported_id);

-- RLS는 관례대로 2차 방어선. 신고자는 자기 신고만 보고 만들 수 있다.
-- 피신고자에게는 존재 자체를 노출하지 않는다(차단과 같은 원칙).
ALTER TABLE user_reports ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_reports_select ON user_reports
    FOR SELECT USING (reporter_id = current_app_user_id());

CREATE POLICY user_reports_insert ON user_reports
    FOR INSERT WITH CHECK (reporter_id = current_app_user_id());
