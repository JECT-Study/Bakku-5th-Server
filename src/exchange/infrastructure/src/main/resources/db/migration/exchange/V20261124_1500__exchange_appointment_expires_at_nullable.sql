-- 약속 제안 자동 만료(E-10-1)가 출시 후로 밀려 새 제안은 만료 시각을 비워 둔다 (#117)
ALTER TABLE exchange_appointment
    ALTER COLUMN expires_at DROP NOT NULL;
