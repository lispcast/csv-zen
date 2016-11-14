-- :name install-uuid-module
-- :command :execute
-- :result :raw
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- :name create-entity-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS entities (
  id          UUID PRIMARY KEY,
  entity_type TEXT NOT NULL
)

-- :name create-event-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS events (
  id         BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  entity_id  UUID NOT NULL REFERENCES entities(id),
  event_name TEXT NOT NULL,
  event_data TEXT NOT NULL
)

-- :name create-entity* :<! :1
INSERT INTO entities (id, entity_type)
VALUES (uuid_generate_v4(), :entity-type)
RETURNING id;

-- :name dispatch-event* :<! :1
INSERT INTO events (entity_id, event_name, event_data)
SELECT :entity-id, :event-name, :event-data
WHERE :version = (SELECT max(id)
                   FROM events
                     WHERE entity_id = :entity-id)
      OR NOT EXISTS (SELECT id
                      FROM events
                        WHERE entity_id = :entity-id)
RETURNING id;

-- :name fetch-events*
SELECT * FROM events
WHERE entity_id = :entity-id
ORDER BY id;

-- :name fetch-entity-type*
SELECT * FROM entities
WHERE id = :entity-id;
