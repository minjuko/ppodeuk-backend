-- Minimal deterministic data for legacy integration/controller tests.
-- These identities are intentionally test-only and are unrelated to demo or production data.

CREATE ALIAS IF NOT EXISTS ST_Distance_Sphere AS
    'double distance(double first, double second) { return 0.0; }';
CREATE ALIAS IF NOT EXISTS POINT AS
    'double point(double longitude, double latitude) { return 0.0; }';

INSERT INTO member (id, email, username, password, tel, role) VALUES
    (1, 'owner@nate.com', 'test-fixture-owner', '{noop}test-placeholder-password', '010-0000-0001', 'ROLE_OWNER'),
    (2, 'user@nate.com', 'test-fixture-user', '{noop}test-placeholder-password', '010-0000-0002', 'ROLE_USER');

INSERT INTO location (id, address, latitude, longitude) VALUES
    (1, 'Test Fixture Address', 37.5665, 126.9780),
    (2, 'Test Fixture Address 2', 37.5666, 126.9781);

INSERT INTO carwash (id, name, rate, tel, des, price, l_id, m_id) VALUES
    (1, 'Test Fixture Carwash', 0.0, '02-0000-0001', 'Integration test fixture', 10000, 1, 1),
    (2, 'Test Fixture Carwash 2', 0.0, '02-0000-0002', 'Owner reporting fixture', 10000, 2, 1);

INSERT INTO optime (id, day_type, start_time, end_time, c_id) VALUES
    (1, 'WEEKDAY', '00:00:00', '23:59:59', 1),
    (2, 'WEEKEND', '00:00:00', '23:59:59', 1),
    (3, 'WEEKDAY', '00:00:00', '23:59:59', 2),
    (4, 'WEEKEND', '00:00:00', '23:59:59', 2);

INSERT INTO bay (id, bay_num, w_id, status) VALUES
    (1, 1, 1, 1),
    (2, 1, 2, 1);

-- ReviewService validates review keyword IDs as 1-7 and carwash keyword IDs as 8-14.
INSERT INTO keyword (id, name, type) VALUES
    (1, 'test-review-keyword-1', 2),
    (2, 'test-review-keyword-2', 2),
    (3, 'test-review-keyword-3', 2),
    (4, 'test-review-keyword-4', 2),
    (5, 'test-review-keyword-5', 2),
    (6, 'test-review-keyword-6', 2),
    (7, 'test-review-keyword-7', 2),
    (8, 'test-carwash-keyword', 1);

INSERT INTO carwash_keyword (id, c_id, k_id) VALUES
    (1, 1, 8);

INSERT INTO reservation
    (id, price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id)
VALUES
    (1, 20000, '2023-01-02 14:00:00', '2023-01-02 15:00:00', false,
     '2023-01-01 00:00:00', '2023-01-01 00:00:00', 1, 2);
