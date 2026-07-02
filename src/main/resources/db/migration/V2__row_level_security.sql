-- Row Level Security (PRD 22번 Phase 3 "RLS 정책")
--
-- 이 백엔드는 클라이언트가 DB에 직접 붙는 구조가 아니라 Spring Boot API를 통해서만
-- 접근하므로, 1차 권한 검증은 Spring Security + 서비스 레이어에서 이루어진다.
-- 아래 RLS는 애플리케이션 코드의 실수(그룹 필터 누락 등)로 인한 그룹 간 데이터 누출을
-- 막는 2차 방어선(defense-in-depth)이다.
--
-- 동작 방식: 백엔드가 트랜잭션마다
--   SET LOCAL app.current_user_id = '<로그인한 유저 id>';
-- 를 실행해야 정책이 통과된다. 설정하지 않으면 current_app_user_id()가 NULL이 되어
-- 기본적으로 모든 행이 차단된다(fail-closed).
--
-- user_groups 자기 자신을 참조하는 정책의 무한 재귀를 피하기 위해
-- is_group_member()를 SECURITY DEFINER로 선언해 RLS를 우회하고 멤버십만 조회한다.
-- (레거시 user_groups.status는 owner/가입요청 등 여러 의미로 겹쳐 쓰이던 컬럼이라,
--  여기서는 상태값과 무관하게 행 존재 여부만으로 멤버십을 판단한다.)

CREATE OR REPLACE FUNCTION current_app_user_id() RETURNS BIGINT AS $$
    SELECT NULLIF(current_setting('app.current_user_id', true), '')::BIGINT
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION is_group_member(p_group_id BIGINT, p_user_id BIGINT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM user_groups WHERE group_id = p_group_id AND user_id = p_user_id
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;

-- =========================================================
-- user_groups
-- =========================================================
ALTER TABLE user_groups ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_groups_select ON user_groups
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
        AND user_id = current_app_user_id()
    );

CREATE POLICY posts_update ON posts
    FOR UPDATE USING (user_id = current_app_user_id());

CREATE POLICY posts_delete ON posts
    FOR DELETE USING (user_id = current_app_user_id());

-- =========================================================
-- images (post_id를 경유해 그룹 소속 확인)
-- =========================================================
ALTER TABLE images ENABLE ROW LEVEL SECURITY;

CREATE POLICY images_select ON images
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = images.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

-- =========================================================
-- replys / user_replys (post_id를 경유해 그룹 소속 확인)
-- =========================================================
ALTER TABLE user_replys ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_replys_select ON user_replys
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = user_replys.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY user_replys_insert ON user_replys
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = user_replys.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

ALTER TABLE replys ENABLE ROW LEVEL SECURITY;

CREATE POLICY replys_select ON replys
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM user_replys ur
            JOIN posts p ON p.id = ur.post_id
            WHERE ur.reply_id = replys.id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY replys_update ON replys
    FOR UPDATE USING (user_id = current_app_user_id());

CREATE POLICY replys_delete ON replys
    FOR DELETE USING (user_id = current_app_user_id());

-- =========================================================
-- chat_rooms / messages
-- =========================================================
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;

CREATE POLICY chat_rooms_select ON chat_rooms
    FOR SELECT USING (
        group_id IS NULL OR is_group_member(group_id, current_app_user_id())
    );

ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY messages_select ON messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = messages.chat_room_id
              AND (r.group_id IS NULL OR is_group_member(r.group_id, current_app_user_id()))
        )
    );

CREATE POLICY messages_insert ON messages
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = messages.chat_room_id
              AND (r.group_id IS NULL OR is_group_member(r.group_id, current_app_user_id()))
        )
    );

-- =========================================================
-- notifications (그룹이 아닌 개인 소유 데이터)
-- =========================================================
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY notifications_select ON notifications
    FOR SELECT USING (user_id = current_app_user_id());

CREATE POLICY notifications_update ON notifications
    FOR UPDATE USING (user_id = current_app_user_id());
