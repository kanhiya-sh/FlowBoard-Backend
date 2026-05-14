-- ═══════════════════════════════════════════════════════════════════════════
-- FlowBoard – MySQL bootstrap script
-- This file is auto-executed by the official mysql:8.0 image on first start
-- (mounted at /docker-entrypoint-initdb.d). It runs ONLY when the data
-- volume is empty — to re-run, `docker compose down -v` first.
-- ═══════════════════════════════════════════════════════════════════════════

-- One database per microservice (DB-per-service pattern)
CREATE DATABASE IF NOT EXISTS flowboard_auth          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_workspace     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_board         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_list          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_card          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_comment       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_label         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowboard_notification  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant the application user full access to every flowboard_* schema.
-- The username/password come from MYSQL_USER / MYSQL_PASSWORD (compose env).
-- We re-grant explicitly so the user can touch ALL 8 dbs (MySQL only auto-
-- grants on the single MYSQL_DATABASE — which we don't use).
GRANT ALL PRIVILEGES ON flowboard_auth.*         TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_workspace.*    TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_board.*        TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_list.*         TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_card.*         TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_comment.*      TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_label.*        TO 'flowboard'@'%';
GRANT ALL PRIVILEGES ON flowboard_notification.* TO 'flowboard'@'%';

FLUSH PRIVILEGES;
