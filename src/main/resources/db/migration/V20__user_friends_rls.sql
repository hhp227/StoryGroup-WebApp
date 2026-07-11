-- 친구 기능 활성화에 따른 user_friends RLS 정책 추가
--
-- user_friends 테이블 자체는 V1(레거시 스키마 이관)부터 있었지만 지금까지 쓰는 코드가 없었다.
-- 친구는 단방향 등록(즐겨찾기 성격, 승인 절차 없음) — user_id가 friend_id를 등록한 것.
-- 검색 페이지 친구 섹션 + 사용자 검색 결과의 친구 추가/해제가 진입점.
--
-- RLS는 관례대로 2차 방어선 문서화용(앱 DB 롤은 테이블 소유자라 우회 - 실제 검증은 서비스 레이어).
-- user_blocks(V18)와 동일하게 등록한 본인만 보고 관리한다.

ALTER TABLE user_friends ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_friends_select ON user_friends
    FOR SELECT USING (user_id = current_app_user_id());

CREATE POLICY user_friends_insert ON user_friends
    FOR INSERT WITH CHECK (user_id = current_app_user_id());

CREATE POLICY user_friends_delete ON user_friends
    FOR DELETE USING (user_id = current_app_user_id());

-- 레거시 테이블이라 자기 자신 등록 금지 제약이 없었다 - 서비스 검증의 2차 방어선으로 추가.
-- (레거시 이전 데이터는 어차피 스킵되어 비어 있으므로 기존 행 걱정 없음)
ALTER TABLE user_friends ADD CONSTRAINT user_friends_no_self CHECK (user_id <> friend_id);
