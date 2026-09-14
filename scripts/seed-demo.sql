-- Portfolio deployment demo data for Team10.
--
-- SAFETY:
--   * This file is never loaded by Spring Boot. Run it manually against a
--     dedicated local/demo `bdbd` database only.
--   * It deletes and recreates only the fixed demo IDs documented below.
--     Every other row is left untouched.
--   * Deletes are ordered from FK children to parents. If non-demo rows refer
--     to these demo parents, the transaction fails instead of deleting them.
--   * Do not run this script against a shared or production database.

START TRANSACTION;

-- Remove only rows owned by this seed, in FK-safe order.
DELETE FROM review_keyword WHERE id BETWEEN 1001 AND 1020;
DELETE FROM review WHERE id BETWEEN 1001 AND 1010;
DELETE FROM reservation WHERE id BETWEEN 1001 AND 1014;
DELETE FROM reservation WHERE id BETWEEN 2001 AND 10000;
DELETE FROM reservation WHERE id IN (10001, 10002) OR id BETWEEN 11001 AND 11025;
DELETE FROM carwash_keyword WHERE id BETWEEN 1001 AND 1015;
DELETE FROM optime WHERE id BETWEEN 1001 AND 1010;
DELETE FROM bay WHERE id BETWEEN 1001 AND 1013;
DELETE FROM file WHERE id BETWEEN 1001 AND 1015;
DELETE FROM carwash WHERE id BETWEEN 1001 AND 1005;
DELETE FROM location WHERE id BETWEEN 1001 AND 1005;
-- The explicitly named local demo login is also reclaimed if it was created
-- manually with a different auto-generated ID. Existing FK references make
-- the transaction fail rather than cascade-delete unrelated activity.
DELETE FROM member WHERE email IN ('test-owner@example.com', 'test-user@example.com', 'test1@test.com');
DELETE FROM member WHERE id BETWEEN 101 AND 202;
DELETE FROM keyword WHERE id BETWEEN 1 AND 14;

-- IDs 1-7 are REVIEW (type=2); IDs 8-14 are CARWASH (type=1).
-- These numeric ranges are validated directly by production service/controller code.
INSERT INTO keyword (id, name, type) VALUES
    (1,  '시설이 깔끔해요',       2),
    (2,  '응대가 친절해요',       2),
    (3,  '이용이 빨라요',           2),
    (4,  '장비 관리가 잘돼요',    2),
    (5,  '가격이 합리적이에요',     2),
    (6,  '예약이 편리해요',        2),
    (7,  '다시 방문하고 싶어요',   2),
    (8,  '노터치 세차',             1),
    (9,  '폼 세차',                 1),
    (10, '하부 세차',                1),
    (11, '진공청소기 이용',          1),
    (12, '대기 공간',               1),
    (13, '카드 결제',               1),
    (14, '프리미엄 코팅',            1);

-- Passwords use Spring Security's DelegatingPasswordEncoder BCrypt format.
-- Existing portfolio credentials are intentionally preserved:
--   OWNER: test-owner@example.com / test1234!
--   USER: test-user@example.com / test1234!
-- Never reuse these public demo credentials outside an isolated demo DB.
INSERT INTO member (id, email, password, role, tel, username) VALUES
    (101, 'test-owner@example.com',
     '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW',
     'ROLE_OWNER', '010-0000-1001', '홍길동'),
    (102, 'test-user@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1002', '사용자1'),
    (103, 'test-user2@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1003', '사용자2'),
    (104, 'test-user3@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1004', '사용자3'),
    (105, 'test-user4@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1005', '사용자4'),
    (106, 'test-user5@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1006', '사용자5'),
    (107, 'test-user6@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1007', '사용자6'),
    (108, 'test-user7@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1008', '사용자7'),
    (109, 'test-user8@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1009', '사용자8'),
    (110, 'test-user9@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1010', '사용자9'),
    (111, 'test-user10@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1011', '사용자10'),
    (112, 'test-user11@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1012', '사용자11'),
    (113, 'test-user12@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1013', '사용자12'),
    (114, 'test-user13@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1014', '사용자13'),
    (115, 'test-user14@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1015', '사용자14'),
    (116, 'test-user15@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1016', '사용자15'),
    (117, 'test-user16@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1017', '사용자16'),
    (118, 'test-user17@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1018', '사용자17'),
    (119, 'test-user18@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1019', '사용자18'),
    (120, 'test-user19@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1020', '사용자19'),
    (121, 'test-user20@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1021', '사용자20'),
    (122, 'test-user21@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1022', '사용자21'),
    (123, 'test-user22@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1023', '사용자22'),
    (124, 'test-user23@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1024', '사용자23'),
    (125, 'test-user24@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1025', '사용자24'),
    (126, 'test-user25@example.com', '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW', 'ROLE_USER', '010-0000-1026', '사용자25');

DROP PROCEDURE IF EXISTS seed_extra_users;
DELIMITER //
CREATE PROCEDURE seed_extra_users()
BEGIN
    DECLARE v_user_no INT DEFAULT 26;
    WHILE v_user_no <= 101 DO
        INSERT INTO member (id, email, password, role, tel, username)
        VALUES (
            101 + v_user_no,
            CONCAT('test-user', v_user_no, '@example.com'),
            '{bcrypt}$2a$10$zr.GEsRl57PlYdh0eyKSUedpnKhTLNwkLrSfCyCaS89OEm2qAodsW',
            'ROLE_USER',
            CONCAT('010-0000-', LPAD(1000 + v_user_no, 4, '0')),
            CONCAT('사용자', v_user_no)
        );
        SET v_user_no = v_user_no + 1;
    END WHILE;
END//
DELIMITER ;
CALL seed_extra_users();
DROP PROCEDURE IF EXISTS seed_extra_users;

-- Fictional locations spread across Gwangju for visibly distinct map markers.
INSERT INTO location (id, address, latitude, longitude) VALUES
    (1001, '광주광역시 북구 첨단연신로 100', 35.221300, 126.851400),
    (1002, '광주광역시 광산구 수완로 120', 35.190700, 126.824900),
    (1003, '광주광역시 서구 상무중앙로 80', 35.153600, 126.851500),
    (1004, '광주광역시 남구 봉선로 150', 35.121500, 126.907800),
    (1005, '광주광역시 동구 증심사길 30', 35.133200, 126.934600);

-- All names are fictional and intended only for a portfolio demonstration.
INSERT INTO carwash (id, name, rate, tel, des, price, l_id, m_id) VALUES
    (1001, '맑은결 워시', 4.5, '062-000-1001',
     '넓은 베이와 편안한 대기 공간을 갖춘 세차장입니다.', 6000, 1001, 101),
    (1002, '클린웨이브', 4.5, '062-000-1002',
     '아침부터 이용할 수 있어 출근 전에도 편하게 방문할 수 있습니다.', 5500, 1002, 101),
    (1003, '라이트 버블', 4.5, '062-000-1003',
     '점심과 퇴근 시간대에도 여유롭게 이용할 수 있습니다.', 7000, 1003, 101),
    (1004, '화이트 폼', 4.25, '062-000-1004',
     '늦은 저녁까지 운영해 일과 후 방문하기 좋습니다.', 7500, 1004, 101),
    (1005, '프레시 드라이브', 4.5, '062-000-1005',
     '주말에도 여유롭게 이용할 수 있고 관리가 편리합니다.', 8000, 1005, 101);

-- Local-only static images served by Spring Boot for the portfolio demo.
INSERT INTO file (id, name, url, uploaded_at, is_deleted, c_id) VALUES
    (1001, '01-main.jpg',     'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1001/01-main.jpg',     NOW(6), false, 1001),
    (1002, '02-bay.jpg',      'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1001/02-bay.jpg',      NOW(6), false, 1001),
    (1003, '03-facility.jpg', 'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1001/03-facility.jpg', NOW(6), false, 1001),
    (1004, '01-main.jpg',     'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1002/01-main.jpg',     NOW(6), false, 1002),
    (1005, '02-bay.jpg',      'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1002/02-bay.jpg',      NOW(6), false, 1002),
    (1006, '03-facility.jpg', 'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1002/03-facility.jpg', NOW(6), false, 1002),
    (1007, '01-main.jpg',     'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1003/01-main.jpg',     NOW(6), false, 1003),
    (1008, '02-bay.jpg',      'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1003/02-bay.jpg',      NOW(6), false, 1003),
    (1009, '03-facility.jpg', 'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1003/03-facility.jpg', NOW(6), false, 1003),
    (1010, '01-main.jpg',     'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1004/01-main.jpg',     NOW(6), false, 1004),
    (1011, '02-bay.jpg',      'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1004/02-bay.jpg',      NOW(6), false, 1004),
    (1012, '03-facility.jpg', 'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1004/03-facility.jpg', NOW(6), false, 1004),
    (1013, '01-main.jpg',     'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1005/01-main.jpg',     NOW(6), false, 1005),
    (1014, '02-bay.jpg',      'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1005/02-bay.jpg',      NOW(6), false, 1005),
    (1015, '03-facility.jpg', 'https://ppodeuk-backend.up.railway.app/demo-images/carwash-1005/03-facility.jpg', NOW(6), false, 1005);

-- Different operating-hour patterns per carwash.
-- Reservations use the intersection of each WEEKDAY/WEEKEND range, so
-- CURDATE-relative dates remain valid regardless of the day this seed runs.
INSERT INTO optime (id, day_type, start_time, end_time, c_id) VALUES
    (1001, 'WEEKDAY', '09:00:00', '20:00:00', 1001),
    (1002, 'WEEKEND', '10:00:00', '18:00:00', 1001),
    (1003, 'WEEKDAY', '07:30:00', '20:30:00', 1002),
    (1004, 'WEEKEND', '08:00:00', '18:30:00', 1002),
    (1005, 'WEEKDAY', '11:00:00', '21:00:00', 1003),
    (1006, 'WEEKEND', '10:00:00', '19:00:00', 1003),
    (1007, 'WEEKDAY', '12:00:00', '22:00:00', 1004),
    (1008, 'WEEKEND', '10:00:00', '22:00:00', 1004),
    (1009, 'WEEKDAY', '08:30:00', '19:30:00', 1005),
    (1010, 'WEEKEND', '09:00:00', '20:00:00', 1005);

-- status=1 is the repository's active/available bay value.
-- Bays 1003, 1008, 1010, 1012 and 1013 intentionally have no reservations.
INSERT INTO bay (id, bay_num, status, w_id) VALUES
    (1001, 1, 1, 1001), (1002, 2, 1, 1001), (1003, 3, 1, 1001),
    (1004, 1, 1, 1002), (1005, 2, 1, 1002),
    (1006, 1, 1, 1003), (1007, 2, 1, 1003), (1008, 3, 1, 1003),
    (1009, 1, 1, 1004), (1010, 2, 1, 1004),
    (1011, 1, 1, 1005), (1012, 2, 1, 1005), (1013, 3, 1, 1005);

INSERT INTO carwash_keyword (id, c_id, k_id) VALUES
    (1001, 1001, 8),  (1002, 1001, 11), (1003, 1001, 13),
    (1004, 1002, 9),  (1005, 1002, 12), (1006, 1002, 13),
    (1007, 1003, 8),  (1008, 1003, 10), (1009, 1003, 14),
    (1010, 1004, 9),  (1011, 1004, 11), (1012, 1004, 14),
    (1013, 1005, 10), (1014, 1005, 12), (1015, 1005, 13);

-- Dynamic dates keep the dataset useful on every demo deployment.
-- On bay 1001, reservations 1001 -> 1002 are consecutive, while 1002 ->
-- 1003 has a two-hour gap. Several bays above remain completely unreserved.
INSERT INTO reservation
    (id, price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id)
VALUES
    (1001, 12000,
     TIMESTAMP('2026-04-15', '10:00:00'),
     TIMESTAMP('2026-04-15', '11:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_SUB(NOW(6), INTERVAL 12 DAY), 1001, 102),
    (1002, 12000,
     TIMESTAMP('2026-04-15', '11:00:00'),
     TIMESTAMP('2026-04-15', '12:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_SUB(NOW(6), INTERVAL 12 DAY), 1001, 103),
    (1003, 12000,
     TIMESTAMP('2026-04-29', '14:00:00'),
     TIMESTAMP('2026-04-29', '15:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_SUB(NOW(6), INTERVAL 12 DAY), 1001, 102),
    (1004, 11000,
     TIMESTAMP('2026-04-22', '08:30:00'),
     TIMESTAMP('2026-04-22', '09:30:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 9 DAY), 1004, 104),
    (1005, 14000,
     TIMESTAMP('2026-04-25', '12:00:00'),
     TIMESTAMP('2026-04-25', '13:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY), 1006, 103),
    (1006, 15000,
     TIMESTAMP('2026-04-28', '18:00:00'),
     TIMESTAMP('2026-04-28', '19:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY), 1009, 104),
    (1007, 16000,
     TIMESTAMP('2026-05-01', '09:30:00'),
     TIMESTAMP('2026-05-01', '10:30:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY), 1011, 105),
    (1008, 12000,
     TIMESTAMP('2026-05-14', '10:00:00'),
     TIMESTAMP('2026-05-14', '11:00:00'),
     b'0', NOW(6), NOW(6), 1002, 103),
    (1009, 11000,
     TIMESTAMP('2026-05-15', '16:00:00'),
     TIMESTAMP('2026-05-15', '17:00:00'),
     b'0', NOW(6), NOW(6), 1005, 104),
    (1010, 14000,
     TIMESTAMP('2026-05-16', '15:00:00'),
     TIMESTAMP('2026-05-16', '16:00:00'),
     b'0', NOW(6), NOW(6), 1007, 105),
    (1011, 11000,
     TIMESTAMP('2026-05-03', '10:00:00'),
     TIMESTAMP('2026-05-03', '11:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY), 1005, 103),
    (1012, 14000,
     TIMESTAMP('2026-05-05', '14:00:00'),
     TIMESTAMP('2026-05-05', '15:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY), 1007, 104),
    (1013, 15000,
     TIMESTAMP('2026-05-06', '16:00:00'),
     TIMESTAMP('2026-05-06', '17:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY), 1010, 102),
    (1014, 16000,
     TIMESTAMP('2026-05-08', '11:00:00'),
     TIMESTAMP('2026-05-08', '12:00:00'),
     b'0', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY), 1012, 104);

-- Add 80 reservations per carwash for each month of 2026-01 through 2026-09.
-- October is also populated so the owner calendar has next-month data.
-- Four time slots across twenty days create varied bay schedules.
DROP PROCEDURE IF EXISTS seed_calendar_reservations;
DELIMITER //
CREATE PROCEDURE seed_calendar_reservations()
BEGIN
    DECLARE v_month INT DEFAULT 1;
    DECLARE v_carwash INT;
    DECLARE v_day INT;
    DECLARE v_slot INT;
    DECLARE v_reservation_id INT;
    DECLARE v_reservation_date DATE;
    DECLARE v_start_time DATETIME;
    DECLARE v_bay_id INT;
    DECLARE v_member_id INT;
    DECLARE v_price INT;
    DECLARE v_bookings INT;
    DECLARE v_duration_minutes INT;

    WHILE v_month <= 10 DO
        SET v_carwash = 1001;
        WHILE v_carwash <= 1005 DO
            SET v_day = 1;
            WHILE v_day <= DAY(LAST_DAY(DATE_ADD('2026-01-01', INTERVAL (v_month - 1) MONTH))) DO
                SET v_bookings = CASE v_carwash
                    WHEN 1001 THEN CASE MOD(v_day, 5) WHEN 0 THEN 5 WHEN 1 THEN 4 WHEN 2 THEN 3 ELSE 4 END
                    WHEN 1002 THEN CASE MOD(v_day, 4) WHEN 0 THEN 4 ELSE 3 END
                    WHEN 1003 THEN CASE MOD(v_day, 4) WHEN 0 THEN 3 WHEN 1 THEN 4 WHEN 2 THEN 2 ELSE 3 END
                    WHEN 1004 THEN CASE MOD(v_day, 3) WHEN 0 THEN 5 WHEN 1 THEN 3 ELSE 4 END
                    ELSE CASE MOD(v_day, 4) WHEN 0 THEN 2 WHEN 1 THEN 3 WHEN 2 THEN 4 ELSE 3 END
                END;
                SET v_slot = 0;
                WHILE v_slot < v_bookings DO
                    SET v_reservation_id = 2000
                        + ((v_month - 1) * 700)
                        + ((v_day - 1) * 25)
                        + ((v_carwash - 1001) * 5)
                        + v_slot + 1;
                    SET v_reservation_date = DATE_ADD(
                        DATE_ADD('2026-01-01', INTERVAL (v_month - 1) MONTH),
                        INTERVAL (v_day - 1) DAY
                    );
                    SET v_start_time = TIMESTAMP(
                        v_reservation_date,
                        MAKETIME(8 + (v_slot * 2) + MOD(v_carwash, 2), IF(MOD(v_slot, 2) = 0, 0, 30), 0)
                    );
                    SET v_bay_id = CASE v_carwash
                        WHEN 1001 THEN 1001 + MOD(v_slot, 3)
                        WHEN 1002 THEN 1004 + MOD(v_slot, 2)
                        WHEN 1003 THEN 1006 + MOD(v_slot, 3)
                        WHEN 1004 THEN 1009 + MOD(v_slot, 2)
                        ELSE 1011 + MOD(v_slot, 3)
                    END;
                    SET v_member_id = 108 + MOD(
                        ((v_month - 1) * 20) + ((v_day - 1) * 20)
                        + ((v_carwash - 1001) * 5) + v_slot, 95);
                    SET v_price = CASE v_carwash
                        WHEN 1001 THEN 12000
                        WHEN 1002 THEN 11000
                        WHEN 1003 THEN 14000
                        WHEN 1004 THEN 15000
                        ELSE 16000
                    END + (v_month * 250) + (MOD(v_day, 3) * 500);
                    SET v_duration_minutes = IF(MOD(v_day + v_slot + v_carwash, 3) = 0, 120, 60);

                    INSERT INTO reservation
                        (id, price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id)
                    VALUES
                        (v_reservation_id, v_price, v_start_time,
                         DATE_ADD(v_start_time, INTERVAL v_duration_minutes MINUTE), b'0',
                         IF(v_reservation_date < CURDATE(),
                            TIMESTAMP(v_reservation_date, '08:00:00'), NOW(6)),
                         IF(v_reservation_date < CURDATE(),
                            TIMESTAMP(v_reservation_date, '08:00:00'), NOW(6)),
                         v_bay_id, v_member_id);
                    SET v_slot = v_slot + 1;
                END WHILE;
                SET v_day = v_day + 1;
            END WHILE;
            SET v_carwash = v_carwash + 1;
        END WHILE;
        SET v_month = v_month + 1;
    END WHILE;
END//
DELIMITER ;
CALL seed_calendar_reservations();
DROP PROCEDURE IF EXISTS seed_calendar_reservations;

-- User 1 is the screen-capture account: one active reservation now and one
-- upcoming reservation two weeks later, with no other future reservations.
INSERT INTO reservation
    (id, price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id)
VALUES
    (10001, 12000, TIMESTAMP('2026-05-30', '12:00:00'), TIMESTAMP('2026-05-30', '13:00:00'),
     b'0', TIMESTAMP('2026-05-30', '11:00:00'), TIMESTAMP('2026-05-30', '11:00:00'), 1003, 102),
    (10002, 14000, TIMESTAMP('2026-06-13', '18:00:00'),
     TIMESTAMP('2026-06-13', '20:00:00'),
     b'0', NOW(6), NOW(6), 1002, 102);

-- Every review references a completed reservation whose end_time is before
-- CURDATE(). The review carwash/member also matches the linked reservation.
INSERT INTO review
    (id, comment, created_at, rate, updated_at, c_id, m_id, r_id)
VALUES
    (1001, '시설이 깔끔하고 베이가 넓어서 편했어요.',
     DATE_SUB(NOW(6), INTERVAL 11 DAY), 5.0, DATE_SUB(NOW(6), INTERVAL 11 DAY), 1001, 102, 1001),
    (1002, '예약 시간에 바로 이용할 수 있어서 좋았어요.',
     DATE_SUB(NOW(6), INTERVAL 11 DAY), 4.0, DATE_SUB(NOW(6), INTERVAL 11 DAY), 1001, 103, 1002),
    (1003, '아침에 이용하기 편하고 응대도 친절했어요.',
     DATE_SUB(NOW(6), INTERVAL 8 DAY), 5.0, DATE_SUB(NOW(6), INTERVAL 8 DAY), 1002, 104, 1004),
    (1004, '도심에서 예약하기 쉽고 장비 관리가 잘되어 있어요.',
     DATE_SUB(NOW(6), INTERVAL 6 DAY), 4.0, DATE_SUB(NOW(6), INTERVAL 6 DAY), 1003, 103, 1005),
    (1005, '저녁 시간대에도 여유롭게 이용했고 가격도 합리적이에요.',
     DATE_SUB(NOW(6), INTERVAL 4 DAY), 4.5, DATE_SUB(NOW(6), INTERVAL 4 DAY), 1004, 104, 1006),
    (1006, '주변이 조용하고 세차 공간이 쾌적해서 다시 방문하고 싶어요.',
     DATE_SUB(NOW(6), INTERVAL 2 DAY), 5.0, DATE_SUB(NOW(6), INTERVAL 2 DAY), 1005, 105, 1007),
    (1007, '대기 공간이 편안하고 결제 과정도 간단했어요.',
     DATE_SUB(NOW(6), INTERVAL 7 DAY), 4.0, DATE_SUB(NOW(6), INTERVAL 7 DAY), 1002, 103, 1011),
    (1008, '하부 세차가 꼼꼼하고 시설 동선이 편리했습니다.',
     DATE_SUB(NOW(6), INTERVAL 5 DAY), 5.0, DATE_SUB(NOW(6), INTERVAL 5 DAY), 1003, 104, 1012),
    (1009, '퇴근 후 방문하기 좋지만 대기 시간이 조금 있었어요.',
     DATE_SUB(NOW(6), INTERVAL 3 DAY), 4.0, DATE_SUB(NOW(6), INTERVAL 3 DAY), 1004, 102, 1013),
    (1010, '주말에도 여유롭고 직원 안내가 친절했습니다.',
     DATE_SUB(NOW(6), INTERVAL 1 DAY), 4.0, DATE_SUB(NOW(6), INTERVAL 1 DAY), 1005, 104, 1014);

INSERT INTO review_keyword (id, k_id, r_id) VALUES
    (1001, 1, 1001), (1002, 4, 1001),
    (1003, 3, 1002), (1004, 6, 1002),
    (1005, 2, 1003), (1006, 7, 1003),
    (1007, 4, 1004), (1008, 6, 1004),
    (1009, 5, 1005), (1010, 7, 1005),
    (1011, 1, 1006), (1012, 7, 1006),
    (1013, 3, 1007), (1014, 12, 1007),
    (1015, 4, 1008), (1016, 10, 1008),
    (1017, 5, 1009), (1018, 6, 1009),
    (1019, 2, 1010), (1020, 7, 1010);

COMMIT;
