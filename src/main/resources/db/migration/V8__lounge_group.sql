-- 레거시 데이터 이전 작업 중 발견: 레거시 앱은 그룹 없이(group_id=0) 올리는 게시글/채팅이
-- 전체 공개 피드("라운지") 역할을 했음. 새 스키마는 posts.group_id가 그룹 필수라 이 개념이
-- 빠져 있었음 — 실제 그룹 테이블에 "라운지" 행을 하나 두고 모든 유저를 자동 가입시키는 방식으로
-- 기존 Post/Comment/Like/Chat 코드를 그대로 재사용한다(라운지도 그냥 그룹 하나일 뿐).
ALTER TABLE groups ADD COLUMN is_lounge BOOLEAN NOT NULL DEFAULT false;

-- 라운지는 앱 전체에 하나만 존재해야 한다.
CREATE UNIQUE INDEX idx_groups_single_lounge ON groups ((1)) WHERE is_lounge;
