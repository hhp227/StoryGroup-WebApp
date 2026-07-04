-- Chat API 구현 시점에 chat_rooms/messages의 누락된 RLS 정책 보강.
-- chat_rooms_select/messages_select/messages_insert는 V2에 이미 있었으나,
-- INSERT(chat_rooms, 채팅방 생성)와 UPDATE/DELETE(messages, 메시지 수정/삭제)는
-- 이번에 새로 추가하는 기능이라 정책이 없었다. posts_update/delete, replys_update/delete와 동일 패턴.
CREATE POLICY chat_rooms_insert ON chat_rooms
    FOR INSERT WITH CHECK (
        group_id IS NULL OR is_group_member(group_id, current_app_user_id())
    );

CREATE POLICY messages_update ON messages
    FOR UPDATE USING (user_id = current_app_user_id());

CREATE POLICY messages_delete ON messages
    FOR DELETE USING (user_id = current_app_user_id());
