-- 신고 관리 (PRD 14번 "신고 관리")
--
-- 두 층위의 관리자가 신고를 처리한다:
--  - 게시글 신고(post_reports): 그룹 관리자(OWNER/ADMIN)가 그룹 신고함에서 처리.
--  - 사용자 신고(user_reports): 앱 전체 데이터라 그룹 관리자와 무관 — 앱 운영자(users.role)를
--    여기서 신설한다. 운영자 지정 UI는 없고 직접 SQL로 지정한다.
--
-- 처리 의미론(두 신고 공통): PENDING(접수) → RESOLVED(확인)/DISMISSED(기각).
-- 상태는 처리 기록일 뿐이고 실제 조치(게시글 삭제/차단 등)는 기존 기능으로 관리자가 직접 한다.
-- 재신고 정책(V21에서 미룬 것): 같은 대상에 대한 "대기중" 신고는 1건만 — 처리된 뒤에는 재신고 가능.

-- 1) 앱 운영자
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- 2) 사용자 신고: 처리 상태
ALTER TABLE user_reports ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE user_reports ADD CONSTRAINT chk_user_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'));
ALTER TABLE user_reports ADD COLUMN processed_at TIMESTAMPTZ;
ALTER TABLE user_reports ADD COLUMN processed_by BIGINT REFERENCES users(id);

-- V21의 "같은 상대는 한 번만"(UNIQUE)을 "대기중 신고는 1건만"으로 완화.
ALTER TABLE user_reports DROP CONSTRAINT user_reports_reporter_id_reported_id_key;
CREATE UNIQUE INDEX uq_user_reports_pending ON user_reports(reporter_id, reported_id) WHERE status = 'PENDING';

-- 운영자의 조회/처리는 RLS 정책으로 표현하지 않는다 — 앱 DB 롤이 테이블 소유자라 RLS를 우회하는
-- 현 구조에서 운영자 인가는 서비스 레이어(requireAdmin)가 담당한다.

-- 3) 게시글 신고: 접수 활성화 (테이블은 V1부터 있었지만 API가 없었다)
-- 복합 PK(user_id, post_id)를 id PK로 교체 — 처리 이력이 행 단위로 쌓이는 user_reports와 같은 모양.
ALTER TABLE post_reports DROP CONSTRAINT post_reports_pkey;
ALTER TABLE post_reports ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE post_reports ADD COLUMN reason VARCHAR(500);
ALTER TABLE post_reports ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE post_reports ADD CONSTRAINT chk_post_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'));
ALTER TABLE post_reports ADD COLUMN processed_at TIMESTAMPTZ;
ALTER TABLE post_reports ADD COLUMN processed_by BIGINT REFERENCES users(id);

CREATE UNIQUE INDEX uq_post_reports_pending ON post_reports(user_id, post_id) WHERE status = 'PENDING';
-- 그룹 신고함 목록 조회용(post 조인 경유 그룹 스코프).
CREATE INDEX idx_post_reports_post ON post_reports(post_id);

-- post_reports는 V1부터 RLS가 아예 없었다(미리 만들어둔 테이블에 RLS가 빠져 있는 알려진 패턴).
-- 접수 = 본인, 조회 = 본인 신고 또는 그룹 관리자, 처리 = 그룹 관리자.
ALTER TABLE post_reports ENABLE ROW LEVEL SECURITY;

CREATE POLICY post_reports_select ON post_reports
    FOR SELECT USING (
        user_id = current_app_user_id()
        OR EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_reports.post_id
              AND is_group_moderator(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY post_reports_insert ON post_reports
    FOR INSERT WITH CHECK (user_id = current_app_user_id());

CREATE POLICY post_reports_update ON post_reports
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_reports.post_id
              AND is_group_moderator(p.group_id, current_app_user_id())
        )
    );
