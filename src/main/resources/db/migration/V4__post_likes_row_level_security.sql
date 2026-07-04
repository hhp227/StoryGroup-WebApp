-- Like API 구현 시점에 post_likes RLS 추가(Phase 3 때는 replys/images와 달리 누락돼 있었음).
-- images_select/user_replys_select와 동일하게 post_id를 경유해 그룹 소속을 확인한다.
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;

CREATE POLICY post_likes_select ON post_likes
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_likes.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY post_likes_insert ON post_likes
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_likes.post_id
              AND is_group_member(p.group_id, current_app_user_id())
        )
    );

CREATE POLICY post_likes_delete ON post_likes
    FOR DELETE USING (user_id = current_app_user_id());
