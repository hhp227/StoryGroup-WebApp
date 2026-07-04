-- meetings/meeting_participants는 Phase 3 때 신규 테이블로 추가만 되고 RLS는 빠져 있었다.
-- Meeting API 구현 시점에 posts/replys와 동일 패턴(그룹 소속 확인, 소유자만 수정)으로 추가.
ALTER TABLE meetings ENABLE ROW LEVEL SECURITY;

CREATE POLICY meetings_select ON meetings
    FOR SELECT USING (is_group_member(group_id, current_app_user_id()));

CREATE POLICY meetings_insert ON meetings
    FOR INSERT WITH CHECK (
        is_group_member(group_id, current_app_user_id())
        AND host_id = current_app_user_id()
    );

CREATE POLICY meetings_update ON meetings
    FOR UPDATE USING (host_id = current_app_user_id());

ALTER TABLE meeting_participants ENABLE ROW LEVEL SECURITY;

CREATE POLICY meeting_participants_select ON meeting_participants
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM meetings m
            WHERE m.id = meeting_participants.meeting_id
              AND is_group_member(m.group_id, current_app_user_id())
        )
    );

CREATE POLICY meeting_participants_insert ON meeting_participants
    FOR INSERT WITH CHECK (
        user_id = current_app_user_id()
        AND EXISTS (
            SELECT 1 FROM meetings m
            WHERE m.id = meeting_participants.meeting_id
              AND is_group_member(m.group_id, current_app_user_id())
        )
    );

-- 호스트가 회의 종료 시 전체 참가자의 left_at을 일괄 갱신하는 것까지는 이 정책이 커버하지 못한다
-- (본인 행만 허용). 다른 도메인과 마찬가지로 RLS는 2차 방어선일 뿐이고 실제 인가는
-- 서비스 레이어(MeetingService)에서 처리하므로 여기서는 단순한 본인 행 수정만 표현한다.
CREATE POLICY meeting_participants_update ON meeting_participants
    FOR UPDATE USING (user_id = current_app_user_id());
