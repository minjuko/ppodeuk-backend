-- Portfolio/local smoke-test data for the Team10 USER frontend.
--
-- SAFETY:
--   * This file is never loaded by Spring Boot. Run it manually against a local
--     `bdbd` database only.
--   * It owns only the fixed demo IDs listed below. Re-running replaces those
--     rows and leaves every other row untouched.
--   * Do not run this script against a shared or production database.

USE bdbd;

START TRANSACTION;

-- Remove only this script's dependent demo rows, in FK-safe order.
DELETE FROM review_keyword WHERE id IN (1001, 1002);
DELETE FROM review WHERE id = 1001;
-- Also remove reservations created by local payment smoke tests for the owned
-- demo user and demo bays. No other member or carwash data is touched.
DELETE FROM reservation WHERE m_id = 102 AND b_id IN (1001, 1002, 1003);
DELETE FROM carwash_keyword WHERE id IN (1001, 1002, 1003, 1004, 1005);
DELETE FROM optime WHERE id IN (1001, 1002, 1003, 1004);
DELETE FROM bay WHERE id IN (1001, 1002, 1003);
DELETE FROM carwash WHERE id IN (1001, 1002);
DELETE FROM location WHERE id IN (1001, 1002);
DELETE FROM member WHERE id IN (101, 102, 1001, 1002);

-- IDs 1-7 are REVIEW (type=2); IDs 8-14 are CARWASH (type=1).
-- These numeric ranges are validated directly by production service/controller code.
INSERT INTO keyword (id, name, type) VALUES
    (1,  'Clean facility',       2),
    (2,  'Friendly service',     2),
    (3,  'Fast service',         2),
    (4,  'Good equipment',       2),
    (5,  'Good value',           2),
    (6,  'Easy reservation',     2),
    (7,  'Would visit again',    2),
    (8,  'Touchless wash',       1),
    (9,  'Foam wash',            1),
    (10, 'Undercarriage wash',   1),
    (11, 'Vacuum available',     1),
    (12, 'Waiting area',         1),
    (13, 'Card payment',         1),
    (14, 'Premium coating',      1)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type);

-- Passwords use Spring Security's DelegatingPasswordEncoder BCrypt format.
-- USER is a local-only portfolio account: test-user@example.com / test1234!
-- Never reuse this public demo credential in shared or production environments.
INSERT INTO member (id, email, password, role, tel, username) VALUES
    (101, 'portfolio-owner@example.com',
     '{bcrypt}$2a$10$y7/woZaew9KVDgL2LCJ7UOsJkflCBMMmju8XAeL4TzKJrKP9/upT6',
     'ROLE_OWNER', '010-0000-1001', 'Portfolio Owner'),
    (102, 'test-user@example.com',
     '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW',
     'ROLE_USER', '010-0000-1002', 'Portfolio User');

INSERT INTO location (id, address, latitude, longitude) VALUES
    (1001, '10 Demo-ro, Buk-gu, Gwangju', 35.176000, 126.910000),
    (1002, '14 Portfolio-ro, Dong-gu, Gwangju', 35.151000, 126.924000);

INSERT INTO carwash (id, name, rate, tel, des, price, l_id, m_id) VALUES
    (1001, 'Portfolio Day Carwash', 5.0, '062-000-1001',
     'Local portfolio example with regular daytime hours.', 6000, 1001, 101),
    (1002, 'Portfolio Afternoon Carwash', 0.0, '062-000-1002',
     'Local portfolio example for afternoon opening boundaries.', 7000, 1002, 101);

INSERT INTO optime (id, day_type, start_time, end_time, c_id) VALUES
    (1001, 'WEEKDAY', '09:30:00', '18:30:00', 1001),
    (1002, 'WEEKEND', '09:30:00', '18:30:00', 1001),
    (1003, 'WEEKDAY', '14:00:00', '18:00:00', 1002),
    (1004, 'WEEKEND', '14:00:00', '18:00:00', 1002);

-- status=1 is the repository's active/available bay value.
INSERT INTO bay (id, bay_num, status, w_id) VALUES
    (1001, 1, 1, 1001),
    (1002, 2, 1, 1001),
    (1003, 1, 1, 1002);

INSERT INTO carwash_keyword (id, c_id, k_id) VALUES
    (1001, 1001, 8),
    (1002, 1001, 9),
    (1003, 1001, 11),
    (1004, 1002, 12),
    (1005, 1002, 13);

-- Dynamic dates keep the same dataset useful on every local run.
INSERT INTO reservation
    (id, price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id)
VALUES
    (1001, 12000,
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00'),
     b'0', NOW(6), NOW(6), 1001, 102),
    (1002, 12000,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '15:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY), 1001, 102);

INSERT INTO review
    (id, comment, created_at, rate, updated_at, c_id, m_id, r_id)
VALUES
    (1001, 'Clean and easy to use in this local portfolio example.',
     DATE_SUB(NOW(6), INTERVAL 1 DAY), 5.0, DATE_SUB(NOW(6), INTERVAL 1 DAY),
     1001, 102, 1002);

INSERT INTO review_keyword (id, k_id, r_id) VALUES
    (1001, 1, 1001),
    (1002, 6, 1001);

COMMIT;
