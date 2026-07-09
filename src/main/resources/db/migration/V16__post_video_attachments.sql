-- 게시글 동영상 첨부: 별도 테이블 대신 images 테이블에 media_type 구분 컬럼을 추가한다.
-- post_id 경유 RLS 정책(V2)과 앨범 파생 뷰 조인(ImageMapper)을 그대로 재사용하기 위한 선택.
-- 기존 행은 전부 이미지이므로 DEFAULT 'image'로 채운다.
ALTER TABLE images ADD COLUMN media_type VARCHAR(10) NOT NULL DEFAULT 'image';
ALTER TABLE images ADD CONSTRAINT images_media_type_check CHECK (media_type IN ('image', 'video'));
