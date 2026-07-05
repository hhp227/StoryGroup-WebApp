-- PRD 6.1 "프로필" 섹션(닉네임/소개/프로필이미지/상태메시지) 중 소개·상태메시지는
-- 레거시 스키마에 자리가 없었다(status는 상태 텍스트가 아니라 legacy INT 플래그).
ALTER TABLE users ADD COLUMN bio VARCHAR(500);
ALTER TABLE users ADD COLUMN status_message VARCHAR(100);
