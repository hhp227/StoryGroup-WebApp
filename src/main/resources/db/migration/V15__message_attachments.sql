-- 채팅 첨부(이미지/파일). 메시지당 첨부 1개 — 여러 파일을 보내면 메시지 여러 개가 된다
-- (별도 attachments 테이블 대신 컬럼 확장: 조회/브로드캐스트 경로가 단순해지고,
--  messages의 기존 RLS(V9에서 그룹+DM 참가자 검사로 재정의됨)가 그대로 적용된다).
-- 파일 실체는 Supabase Storage(공개 URL), 여기엔 메타데이터만 둔다 — files 테이블과 같은 방식.
ALTER TABLE messages ADD COLUMN attachment_url  TEXT;
ALTER TABLE messages ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE messages ADD COLUMN attachment_type VARCHAR(100);
ALTER TABLE messages ADD COLUMN attachment_size BIGINT;

-- 첨부만 있는 메시지는 message=''(빈 문자열)로 저장한다(NOT NULL 유지).
-- 둘 다 비어 있는 행은 생기지 않도록 DB에서도 막는다.
ALTER TABLE messages ADD CONSTRAINT chk_messages_content CHECK (
    message <> '' OR attachment_url IS NOT NULL
);
