-- Row Level Security (PRD 22번 Phase 3 "RLS 정책")
--
-- 이 백엔드는 클라이언트가 DB에 직접 붙는 구조가 아니라 Spring Boot API를 통해서만
-- 접근하므로, 1차 권한 검증은 Spring Security + 서비스 레이어에서 이루어진다.
-- 아래 RLS는 애플리케이션 코드의 실수(그룹 필터 누락 등)로 인한 그룹 간 데이터 누출을
-- 막는 2차 방어선(defense-in-depth)이다.
--
-- 동작 방식: 백엔드가 트랜잭션마다
--   SET LOCAL app.current_user_id = '<로그인한 유저 UUID>';
-- 를 실행해야 정책이 통과된다. 설정하지 않으면 current_app_user_id()가 NULL이 되어
-- 기본적으로 모든 행이 차단된다(fail-closed).
--
-- group_members 자기 자신을 참조하는 정책의 무한 재귀를 피하기 위해
-- is_group_member()를 SECURITY DEFINER로 선언해 RLS를 우회하고 멤버십만 조회한다.

CREATE OR REPLACE FUNCTION current_app_user_id() RETURNS UUID AS $$
    SELECT NULLIF(current_setting('app.current_user_id', true), '')::UUID
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION is_group_member(p_group_id UUID, p_user_id UUID) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM group_members WHERE group_id = p_group_id AND user_id = p_user_id
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;

-- =========================================================
-- group_members
-- =========================================================
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY group_members_select ON group_members
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

-- =========================================================
-- posts
-- =========================================================
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

CREATE POLICY posts_select ON posts
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

CREATE POLICY posts_insert ON posts
    FOR INSERT WITH CHECK (
        is_group_member(group_id, current_app_user_id())
        AND author_id = current_app_user_id()
    );

CREATE POLICY posts_update ON posts
    FOR UPDATE USING (author_id = current_app_user_id());

CREATE POLICY posts_delete ON posts
    FOR DELETE USING (author_id = current_app_user_id());

-- =========================================================
-- comments (그룹 소속 여부는 post_id를 경유해 확인)
-- =========================================================
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY comments_select ON comments
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = comments.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY comments_insert ON comments
    FOR INSERT WITH CHECK (
        author_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = comments.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY comments_update ON comments
    FOR UPDATE USING (author_id = current_app_user_id());

CREATE POLICY comments_delete ON comments
    FOR DELETE USING (author_id = current_app_user_id());

-- =========================================================
-- chat_rooms / chat_messages
-- =========================================================
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;

CREATE POLICY chat_rooms_select ON chat_rooms
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY chat_messages_select ON chat_messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.id = chat_messages.room_id
              AND is_group_member(r.group_id, current_app_user_id())
        )
    );

CREATE POLICY chat_messages_insert ON chat_messages
    FOR INSERT WITH CHECK (
        sender_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.id = chat_messages.room_id
              AND is_group_member(r.group_id, current_app_user_id())
        )
    );

-- =========================================================
-- files
-- =========================================================
ALTER TABLE files ENABLE ROW LEVEL SECURITY;

CREATE POLICY files_select ON files
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

CREATE POLICY files_insert ON files
    FOR INSERT WITH CHECK (
        uploader_id = current_app_user_id()
        AND is_group_member(group_id, current_app_user_id())
    );

-- =========================================================
-- notifications (그룹이 아닌 개인 소유 데이터)
-- =========================================================
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY notifications_select ON notifications
    FOR SELECT USING (user_id = current_app_user_id());

CREATE POLICY notifications_update ON notifications
    FOR UPDATE USING (user_id = current_app_user_id());
