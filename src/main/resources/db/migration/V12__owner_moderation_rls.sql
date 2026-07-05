-- 그룹 방장(OWNER)의 조정 권한(남의 게시글/댓글 삭제, 공지 지정)을 RLS 레벨에서 허용한다.
--
-- 배경: 앱 레이어에서 "작성자 본인 OR 방장" 삭제를 허용하도록 바꿨지만, 삭제가 soft delete
-- (deleted_at UPDATE)라 V2의 posts_update/replys_update 정책("작성자 본인만")에 걸린다.
-- RLS는 조용히 0행을 반환하므로 API는 성공처럼 응답하지만 실제로는 아무것도 안 지워지는
-- 상태였다. 공지 지정(is_notice/is_pinned UPDATE)도 방장이 남의 글을 갱신하는 동작이라 같은
-- 정책 수정이 필요하다.
--
-- 텍스트 수정은 여전히 앱 레이어에서 작성자 본인만 허용한다 - RLS는 2차 방어선이므로
-- "작성자 또는 방장"으로 넓혀도 앱의 더 좁은 규칙이 그대로 유효하다.

CREATE OR REPLACE FUNCTION is_group_owner(p_group_id BIGINT, p_user_id BIGINT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM user_groups
        WHERE group_id = p_group_id AND user_id = p_user_id AND role = 'OWNER'
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;

DROP POLICY posts_update ON posts;
CREATE POLICY posts_update ON posts
    FOR UPDATE USING (
        user_id = current_app_user_id()
        OR is_group_owner(group_id, current_app_user_id())
    );

DROP POLICY replys_update ON replys;
CREATE POLICY replys_update ON replys
    FOR UPDATE USING (
        user_id = current_app_user_id()
        OR EXISTS (
            SELECT 1 FROM user_replys ur
            JOIN posts p ON p.id = ur.post_id
            WHERE ur.reply_id = replys.id
              AND is_group_owner(p.group_id, current_app_user_id())
        )
    );
