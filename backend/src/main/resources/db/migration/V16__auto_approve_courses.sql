-- OPERATOR 역할 미사용으로 기존 PENDING 과정을 자동 승인 처리
UPDATE courses SET approval_status = 'APPROVED' WHERE approval_status = 'PENDING';
