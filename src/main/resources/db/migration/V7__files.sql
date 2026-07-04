-- File API 구현 시점에 신규 추가하는 테이블(레거시/V1 스키마에 없음, PRD 11번 "파일 공유" MVP 범위).
-- 실제 업로드/스토리지 연동(GCS 등)은 아직 없고, Post의 images 테이블과 동일한 접근 방식으로
-- 클라이언트가 이미 업로드한 URL을 넘겨받아 메타데이터만 저장한다.
CREATE TABLE files (
    id            BIGSERIAL PRIMARY KEY,
    group_id      BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    name          VARCHAR(255) NOT NULL,
    url           VARCHAR(500) NOT NULL,
    size          BIGINT,
    content_type  VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_files_group_feed ON files(group_id, created_at DESC);

ALTER TABLE files ENABLE ROW LEVEL SECURITY;

CREATE POLICY files_select ON files
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

CREATE POLICY files_insert ON files
    FOR INSERT WITH CHECK (
        is_group_member(group_id, current_app_user_id())
        AND user_id = current_app_user_id()
    );

CREATE POLICY files_delete ON files
    FOR DELETE USING (user_id = current_app_user_id());
