-- 확정 약속 취소 기록 (E-07-1 동작 2). 취소 시각은 resolved_at을 쓴다
ALTER TABLE exchange_appointment
    ADD COLUMN canceled_by_user_id BIGINT,
    ADD COLUMN cancel_imminent     BOOLEAN;
