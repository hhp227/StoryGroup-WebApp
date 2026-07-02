-- StoryGroup 스키마 v2
-- 레거시 PHP(Slim) 백엔드(../hong227, DbHandler.php/index.php)의 테이블/컬럼 구조를
-- 최대한 그대로 유지하고(정수 PK, users/groups/posts/replys/album/... 명명 규칙),
-- PRD MVP(18번 섹션)에 필요하지만 레거시에 없던 기능만 추가로 얹은 버전.
--
-- 레거시와 다르게 간 지점 (의도적 결정, "최대한 유사하게" 원칙의 예외):
--   1) users.api_key(만료 없는 고정 토큰) 컬럼 제거.
--      PRD 15번 보안 섹션이 JWT + Refresh Token을 명시하는데, 평문 고정 api_key는
--      한번 유출되면 영구 탈취되는 구식 패턴이라 정면으로 충돌함. 대신 refresh_tokens
--      테이블을 추가해 JWT 리프레시 흐름을 지원.
--   2) password_hash 포맷은 그대로 호환됨 — PassHash.php가 bcrypt($2a$10$...)를 쓰므로
--      Spring Security BCryptPasswordEncoder와 100% 호환. 기존 유저 비번 재설정 불필요.

-- =========================================================
-- 회원 (레거시 users 테이블 기준)
-- =========================================================
CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(50) NOT NULL,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255),              -- OAuth 전용 계정은 NULL, bcrypt $2a$10$... 포맷
    status                  INT NOT NULL DEFAULT 0,     -- 레거시 유지: 프로필 상태 플래그
    profile_img             VARCHAR(255),
    fcm_registration_id     VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);

-- 레거시에 없음: PRD 6번 섹션 OAuth 로그인(Google/Apple/Kakao/Naver) 지원용 추가
CREATE TABLE user_oauth_accounts (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            VARCHAR(20) NOT NULL,          -- GOOGLE, APPLE, KAKAO, NAVER
    provider_user_id    VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

-- 레거시에 없음: api_key 고정 토큰 대체용 (위 헤더 설명 참고)
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    device_info     VARCHAR(255),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 레거시에 없음: PRD 15번 섹션 "로그인 기록"
CREATE TABLE login_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    success         BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_login_history_user_id ON login_history(user_id, created_at DESC);

-- =========================================================
-- 그룹 (레거시 groups / user_groups 그대로)
-- =========================================================
CREATE TABLE groups (
    id              BIGSERIAL PRIMARY KEY,
    author_id       BIGINT NOT NULL REFERENCES users(id), -- 레거시 명명 유지 (owner_id 아님)
    name            VARCHAR(100) NOT NULL,
    image           VARCHAR(255),
    description     VARCHAR(1000),
    join_type       INT NOT NULL DEFAULT 0,               -- 레거시 유지: 0=초대전용 등 가입 방식 플래그
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

-- 레거시 user_groups: 멤버십 + 가입 상태를 status 하나로 표현하던 구조 그대로 유지
CREATE TABLE user_groups (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id        BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    status          INT NOT NULL DEFAULT 0,               -- 레거시 유지: 0=owner/가입승인, 그외=가입요청 등
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, group_id)
);
CREATE INDEX idx_user_groups_group_id ON user_groups(group_id);
CREATE INDEX idx_user_groups_user_id ON user_groups(user_id);

-- 레거시에 없음: PRD "그룹 관리자 > 초대 링크 생성" 지원용 추가
CREATE TABLE group_invites (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    code        VARCHAR(20) NOT NULL UNIQUE,
    created_by  BIGINT NOT NULL REFERENCES users(id),
    max_uses    INT,
    used_count  INT NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_group_invites_group_id ON group_invites(group_id);

-- =========================================================
-- 게시글 (레거시 posts / images 그대로)
-- =========================================================
CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    text        TEXT NOT NULL,                 -- 레거시 컬럼명 유지(content 아님)
    status      INT NOT NULL DEFAULT 0,        -- 레거시 유지
    is_notice   BOOLEAN NOT NULL DEFAULT false, -- 레거시에 없음: PRD 공지 상단고정
    is_pinned   BOOLEAN NOT NULL DEFAULT false, -- 레거시에 없음: PRD 공지 상단고정
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_posts_group_feed ON posts(group_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_user_id ON posts(user_id);

CREATE TABLE images (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    image       VARCHAR(255) NOT NULL,
    tag         VARCHAR(100),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_images_post_id ON images(post_id);

-- 레거시 album: getAllAlbum()이 status/profile_img를 select하지만 createAlbum()은
-- name/image만 insert하는 명백한 버그(존재하지 않는 컬럼 select)라 그대로 옮기지 않고
-- 실제로 쓰이는 컬럼만 정리함.
CREATE TABLE album (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    image       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_album (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    album_id    BIGINT NOT NULL REFERENCES album(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, album_id)
);

-- =========================================================
-- 댓글 (레거시 replys / user_replys 그대로 - 오타 포함 유지)
-- 레거시는 replys에 post_id가 없고 user_replys가 (user_id, post_id, reply_id)를
-- 묶어서 보관하는 특이 구조. 그대로 유지하되 reply_id를 PK로 둬서 1:1 확장 테이블화.
-- =========================================================
CREATE TABLE replys (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    parent_reply_id     BIGINT REFERENCES replys(id) ON DELETE CASCADE, -- 레거시에 없음: PRD 대댓글
    reply               TEXT NOT NULL,
    status              INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_replys_parent_id ON replys(parent_reply_id);

CREATE TABLE user_replys (
    reply_id    BIGINT PRIMARY KEY REFERENCES replys(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_replys_post_id ON user_replys(post_id, reply_id);

-- =========================================================
-- 좋아요 / 신고 / 친구 (레거시 post_likes / post_reports / user_friends 그대로)
-- =========================================================
CREATE TABLE post_likes (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE post_reports (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE user_friends (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, friend_id)
);

-- =========================================================
-- 채팅 (레거시 chat_rooms / messages 그대로, PK명도 유지: chat_room_id/message_id)
-- =========================================================
CREATE TABLE chat_rooms (
    chat_room_id    BIGSERIAL PRIMARY KEY,
    group_id        BIGINT REFERENCES groups(id) ON DELETE CASCADE, -- 레거시에 없음: 그룹 스코프 추가
    name            VARCHAR(100) NOT NULL DEFAULT '전체 채팅방',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_rooms_group_id ON chat_rooms(group_id);

CREATE TABLE messages (
    message_id      BIGSERIAL PRIMARY KEY,
    chat_room_id    BIGINT NOT NULL REFERENCES chat_rooms(chat_room_id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    message         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_messages_room_feed ON messages(chat_room_id, created_at DESC);

-- =========================================================
-- 화상회의 / 알림 (레거시에 아예 없음, PRD MVP 필수 기능이라 신규 추가)
-- =========================================================
CREATE TABLE meetings (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    host_id     BIGINT NOT NULL REFERENCES users(id),
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ
);
CREATE INDEX idx_meetings_group_id ON meetings(group_id);

CREATE TABLE meeting_participants (
    id          BIGSERIAL PRIMARY KEY,
    meeting_id  BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at     TIMESTAMPTZ
);
CREATE INDEX idx_meeting_participants_meeting_id ON meeting_participants(meeting_id);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL, -- NEW_POST, COMMENT, LIKE, MENTION, CHAT, MEETING_STARTED, NOTICE, INVITE
    target_type VARCHAR(30),          -- POST, REPLY, MESSAGE, MEETING, GROUP
    target_id   BIGINT,
    is_read     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
