-- 1:1 개인 채팅(DM) 지원. chat_rooms.group_id는 원래부터 nullable이었지만(그룹 스코프 없는
-- 채팅방을 위해 남겨둔 자리) 지금까지는 그룹 채팅방만 써서 실제로 NULL인 행이 없었다.
-- 이제 DM방은 group_id=NULL, user_a_id/user_b_id로 두 참가자를 표시한다.
ALTER TABLE chat_rooms ADD COLUMN user_a_id BIGINT REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE chat_rooms ADD COLUMN user_b_id BIGINT REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chat_rooms ADD CONSTRAINT chk_chat_rooms_scope CHECK (
    (group_id IS NOT NULL AND user_a_id IS NULL AND user_b_id IS NULL)
    OR (group_id IS NULL AND user_a_id IS NOT NULL AND user_b_id IS NOT NULL AND user_a_id <> user_b_id)
);

-- 같은 두 사람 사이에 DM방이 중복 생성되지 않도록(순서 무관하게 유일).
CREATE UNIQUE INDEX idx_chat_rooms_dm_pair ON chat_rooms (LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id))
    WHERE group_id IS NULL;

-- 기존 chat_rooms_select/insert, messages_select/insert 정책은 "group_id IS NULL"이면
-- 무조건 통과하도록 짜여 있었다(그때는 NULL 행이 없어서 문제 없었지만, DM방이 실제로 생기는
-- 지금은 아무나 남의 DM을 보거나 남의 이름으로 DM을 만들 수 있는 구멍이 된다). 참가자 검사를
-- 포함하도록 다시 정의한다.
DROP POLICY chat_rooms_select ON chat_rooms;
CREATE POLICY chat_rooms_select ON chat_rooms
    FOR SELECT USING (
        (group_id IS NOT NULL AND is_group_member(group_id, current_app_user_id()))
        OR (group_id IS NULL AND (user_a_id = current_app_user_id() OR user_b_id = current_app_user_id()))
    );

DROP POLICY chat_rooms_insert ON chat_rooms;
CREATE POLICY chat_rooms_insert ON chat_rooms
    FOR INSERT WITH CHECK (
        (group_id IS NOT NULL AND is_group_member(group_id, current_app_user_id()))
        OR (group_id IS NULL AND (user_a_id = current_app_user_id() OR user_b_id = current_app_user_id()))
    );

DROP POLICY messages_select ON messages;
CREATE POLICY messages_select ON messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = messages.chat_room_id
              AND (
                (r.group_id IS NOT NULL AND is_group_member(r.group_id, current_app_user_id()))
                OR (r.group_id IS NULL AND (r.user_a_id = current_app_user_id() OR r.user_b_id = current_app_user_id()))
              )
        )
    );

DROP POLICY messages_insert ON messages;
CREATE POLICY messages_insert ON messages
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = messages.chat_room_id
              AND (
                (r.group_id IS NOT NULL AND is_group_member(r.group_id, current_app_user_id()))
                OR (r.group_id IS NULL AND (r.user_a_id = current_app_user_id() OR r.user_b_id = current_app_user_id()))
              )
        )
    );
