-- 매너 지표 (F-06-1, P-01-02). 시작 50, 화면에는 0~100으로 잘라 보여준다.
-- 저장 값은 자르지 않는다 (교환 완료 되돌리기 · 노쇼 인정 취소 때 원래 값으로 돌아가게)
ALTER TABLE user_member
    ADD COLUMN manner_score INTEGER NOT NULL DEFAULT 50;
