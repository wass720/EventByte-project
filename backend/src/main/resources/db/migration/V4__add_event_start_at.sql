-- Date de début de l'événement (champ startAt de Strapi, ISO 8601 / UTC).
-- Nullable : les événements ingérés avant cette migration n'ont pas de date.
ALTER TABLE events ADD COLUMN start_at TIMESTAMP WITH TIME ZONE;
