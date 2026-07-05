-- notifications 테이블에 SELECT/UPDATE 정책만 있고 INSERT 정책이 없어서(V2), 기본값(fail-closed)에
-- 막혀 애플리케이션이 알림을 아예 만들 수 없었다. 알림의 user_id는 수신자이지 행위자가 아니라서
-- (예: A가 B의 글에 댓글을 달면 행위자는 A, 알림 수신자는 B) 다른 테이블처럼
-- "user_id = current_app_user_id()" 체크를 걸 수 없다. 알림 생성은 서버 로직이 이미 검증을 마친 뒤
-- 임의의 수신자 앞으로 쓰는 시스템 동작이라 permissive하게 둔다.
CREATE POLICY notifications_insert ON notifications
    FOR INSERT WITH CHECK (true);
