-- :name install-uuid-module
-- :command :execute
-- :result :raw
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- :name create-endpoint-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS endpoints (
  id         UUID PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- :name all-endpoints
SELECT * FROM endpoints;

-- :name create-endpoint* :<! :1
INSERT INTO endpoints (id) VALUES (uuid_generate_v4()) RETURNING id;

-- :name create-upload-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS uploads (
  id         UUID PRIMARY KEY,
  endpoint_id UUID NOT NULL REFERENCES endpoints (id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- :name create-upload* :<! :1
INSERT INTO uploads (id, endpoint_id)
VALUES (uuid_generate_v4(), :endpoint-id)
RETURNING id;

-- :name create-row-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS rows (
  id         UUID PRIMARY KEY,
  upload_id UUID NOT NULL REFERENCES uploads (id)
)

-- :name create-row* :<! :1
INSERT INTO rows (id, upload_id)
VALUES (uuid_generate_v4(), :upload-id)
RETURNING id;

-- :name create-cell-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS cells (
  id         UUID PRIMARY KEY,
  row_id UUID NOT NULL REFERENCES rows (id),
  key TEXT NOT NULL,
  value TEXT NOT NULL
)

-- :name create-cell* :<! :1
INSERT INTO cells (id, row_id, key, value)
VALUES (uuid_generate_v4(), :row-id, :key, :value)
RETURNING id;


-- :name how-many-rows
SELECT count(*) FROM rows
WHERE upload_id = :upload-id

-- :name uploads-for-endpoint
SELECT id FROM uploads
WHERE endpoint_id = :endpoint-id

-- :name keys-for-upload*
SELECT DISTINCT key
FROM cells
  JOIN rows ON cells.row_id = rows.id
WHERE rows.upload_id = :upload-id

-- :name rows-for-upload*
SELECT row_id, key, value
FROM cells
  JOIN rows on cells.row_id = rows.id
WHERE rows.upload_id = :upload-id

-- :name delete-all-endpoints :!
DELETE FROM endpoints

-- :name delete-all-uploads :!
DELETE FROM uploads

-- :name delete-all-rows :!
DELETE FROM rows

-- :name delete-all-cells :!
DELETE FROM cells
