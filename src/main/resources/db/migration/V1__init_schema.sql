-- StoryGroup 초기 스키마
-- 범위: PRD 18번 섹션 MVP (회원가입/로그인/그룹/게시글/댓글/좋아요/채팅/파일첨부/화상회의)
-- 제외: 일정/캘린더/투표/설문/Bot 등 향후 기능(19번 섹션)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 회원
-- =========================================================
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255),               -- OAuth 전용 계정은 NULL
    nickname            VARCHAR(50) NOT NULL,
    profile_image_url   TEXT,
    bio                 VARCHAR(300),
    status_message      VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE user_oauth_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            VARCHAR(20) NOT NULL,       -- GOOGLE, APPLE, KAKAO, NAVER
    provider_user_id    VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash          VARCHAR(255) NOT NULL UNIQUE,
    device_info         VARCHAR(255),
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE login_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(255),
    success             BOOLEAN NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_login_history_user_id ON login_history(user_id, created_at DESC);

-- =========================================================
-- 그룹
-- =========================================================
CREATE TABLE groups (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL,
    description             VARCHAR(1000),
    category                VARCHAR(50),
    cover_image_url         TEXT,
    background_image_url    TEXT,
    visibility              VARCHAR(20) NOT NULL DEFAULT 'INVITE_ONLY', -- PRIVATE, INVITE_ONLY
    owner_id                UUID NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);
CREATE INDEX idx_groups_owner_id ON groups(owner_id);

CREATE TABLE group_members (
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);

CREATE TABLE group_invites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    code        VARCHAR(20) NOT NULL UNIQUE,
    created_by  UUID NOT NULL REFERENCES users(id),
    max_uses    INT,
    used_count  INT NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_group_invites_group_id ON group_invites(group_id);

-- =========================================================
-- 게시글 / 댓글 / 좋아요 / 북마크
-- =========================================================
CREATE TABLE posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL REFERENCES users(id),
    content     TEXT NOT NULL,
    is_notice   BOOLEAN NOT NULL DEFAULT false,
    is_pinned   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
-- 그룹 피드 무한스크롤(16번 비기능요구사항) 조회 패턴에 맞춘 복합 인덱스
CREATE INDEX idx_posts_group_feed ON posts(group_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_author_id ON posts(author_id);

CREATE TABLE comments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id             UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    parent_comment_id   UUID REFERENCES comments(id) ON DELETE CASCADE, -- 대댓글
    author_id           UUID NOT NULL REFERENCES users(id),
    content             TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_comments_post_id ON comments(post_id, created_at);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);

CREATE TABLE post_likes (
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE post_bookmarks (
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

-- =========================================================
-- 실시간 채팅
-- =========================================================
CREATE TABLE chat_rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL DEFAULT '전체 채팅방',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_rooms_group_id ON chat_rooms(group_id);

CREATE TABLE chat_messages (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id                 UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id               UUID NOT NULL REFERENCES users(id),
    content                 TEXT,
    message_type            VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT, IMAGE, VIDEO, FILE, EMOJI, GIF, NOTICE
    reply_to_message_id     UUID REFERENCES chat_messages(id),
    is_notice               BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);
CREATE INDEX idx_chat_messages_room_feed ON chat_messages(room_id, created_at DESC);

CREATE TABLE chat_message_reads (
    room_id                 UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_message_id    UUID REFERENCES chat_messages(id),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE chat_message_mentions (
    message_id          UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    mentioned_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (message_id, mentioned_user_id)
);

-- =========================================================
-- 파일 공유 (게시글 첨부 / 채팅 첨부 / 그룹 파일·사진 탭 겸용)
-- post_id, message_id 둘 다 NULL이면 그룹의 일반 파일 저장소(Files 탭) 항목
-- =========================================================
CREATE TABLE files (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id            UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    uploader_id         UUID NOT NULL REFERENCES users(id),
    post_id             UUID REFERENCES posts(id) ON DELETE CASCADE,
    message_id          UUID REFERENCES chat_messages(id) ON DELETE CASCADE,
    file_type           VARCHAR(20) NOT NULL, -- IMAGE, VIDEO, PDF, WORD, EXCEL, PPT, ZIP, OTHER
    file_name           VARCHAR(255) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    storage_url         TEXT NOT NULL,         -- Supabase Storage object URL
    thumbnail_url       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_files_group_id ON files(group_id, created_at DESC);
CREATE INDEX idx_files_post_id ON files(post_id);
CREATE INDEX idx_files_message_id ON files(message_id);
-- Photos 탭(사진만) 조회 최적화
CREATE INDEX idx_files_group_photos ON files(group_id, created_at DESC) WHERE file_type = 'IMAGE';

-- =========================================================
-- 화상회의
-- =========================================================
CREATE TABLE meetings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    host_id     UUID NOT NULL REFERENCES users(id),
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ
);
CREATE INDEX idx_meetings_group_id ON meetings(group_id);

CREATE TABLE meeting_participants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id  UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at     TIMESTAMPTZ
);
CREATE INDEX idx_meeting_participants_meeting_id ON meeting_participants(meeting_id);

-- =========================================================
-- 알림 / 신고
-- =========================================================
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL, -- NEW_POST, COMMENT, LIKE, MENTION, CHAT, MEETING_STARTED, NOTICE, INVITE
    target_type VARCHAR(30),          -- POST, COMMENT, CHAT_MESSAGE, MEETING, GROUP
    target_id   UUID,
    is_read     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);

CREATE TABLE reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(20) NOT NULL, -- POST, COMMENT, USER
    target_id   UUID NOT NULL,
    reporter_id UUID NOT NULL REFERENCES users(id),
    reason      VARCHAR(500) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, REVIEWED, REJECTED
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_status ON reports(status, created_at);
