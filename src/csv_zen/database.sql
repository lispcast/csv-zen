-- :name install-uuid-module
-- :command :execute
-- :result :raw
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- :name create-endpoint-table
-- :command :execute
-- :result :raw
create table endpoints (
  id         UUID PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
)

-- :name all-endpoints
SELECT * FROM endpoints;

-- :name create-endpoint* :<! :1
INSERT INTO endpoints (id) VALUES (uuid_generate_v4()) RETURNING id;
