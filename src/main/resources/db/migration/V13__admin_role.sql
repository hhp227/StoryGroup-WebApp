-- 부관리자(ADMIN, 부방장) 등급 도입 (PRD 7번 "멤버: 관리자/부관리자/일반회원", 14번 "권한 변경")
ALTER TABLE user_groups DROP CONSTRAINT chk_user_groups_role;
ALTER TABLE user_groups ADD CONSTRAINT chk_user_groups_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'));

-- 조정 권한(남의 게시글/댓글 삭제, 공지)을 부방장까지 확장 - V12의 방장 전용 정책을 대체한다.
CREATE OR REPLACE FUNCTION is_group_moderator(p_group_id BIGINT, p_user_id BIGINT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM user_groups
        WHERE group_id = p_group_id AND user_id = p_user_id AND role IN ('OWNER', 'ADMIN')
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;

DROP POLICY posts_update ON posts;
CREATE POLICY posts_update ON posts
    FOR UPDATE USING (
        user_id = current_app_user_id()
        OR is_group_moderator(group_id, current_app_user_id())
    );

DROP POLICY replys_update ON replys;
CREATE POLICY replys_update ON replys
    FOR UPDATE USING (
        user_id = current_app_user_id()
        OR EXISTS (
            SELECT 1 FROM user_replys ur
            JOIN posts p ON p.id = ur.post_id
            WHERE ur.reply_id = replys.id
              AND is_group_moderator(p.group_id, current_app_user_id())
        )
    );
