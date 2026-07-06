-- 읽음 표시(Read Receipt, Phase 6). 메시지별 행이 아니라 방×사용자당 "마지막으로 읽은
-- 메시지 위치" 한 행만 둔다 — 그 위치 이하의 메시지는 전부 읽은 것으로 본다.
-- 신규 테이블이라 V7(files)처럼 스키마+RLS를 한 파일에 같이 정의한다.
CREATE TABLE chat_room_reads (
    chat_room_id            BIGINT NOT NULL REFERENCES chat_rooms(chat_room_id) ON DELETE CASCADE,
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- 메시지는 soft delete(deleted_at)라 행이 남으므로 FK가 깨질 일 없음.
    last_read_message_id    BIGINT NOT NULL REFERENCES messages(message_id),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (chat_room_id, user_id)
);

ALTER TABLE chat_room_reads ENABLE ROW LEVEL SECURITY;

-- 조회: 그 방의 참가자(그룹 멤버 또는 DM 당사자)만. V9의 chat_rooms_select와 같은 검사.
CREATE POLICY chat_room_reads_select ON chat_room_reads
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = chat_room_reads.chat_room_id
              AND (
                (r.group_id IS NOT NULL AND is_group_member(r.group_id, current_app_user_id()))
                OR (r.group_id IS NULL AND (r.user_a_id = current_app_user_id() OR r.user_b_id = current_app_user_id()))
              )
        )
    );

-- 쓰기: 본인 행만, 그리고 그 방의 참가자일 때만.
CREATE POLICY chat_room_reads_insert ON chat_room_reads
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM chat_rooms r
            WHERE r.chat_room_id = chat_room_reads.chat_room_id
              AND (
                (r.group_id IS NOT NULL AND is_group_member(r.group_id, current_app_user_id()))
                OR (r.group_id IS NULL AND (r.user_a_id = current_app_user_id() OR r.user_b_id = current_app_user_id()))
              )
        )
    );

CREATE POLICY chat_room_reads_update ON chat_room_reads
    FOR UPDATE USING (user_id = current_app_user_id());
