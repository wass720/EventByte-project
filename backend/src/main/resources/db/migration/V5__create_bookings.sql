-- Pas de clé étrangère vers events / tickets : Strapi supprime puis réinsère les tickets
-- à chaque mise à jour (et supprime les événements), ce qui serait bloqué ou cascadé.
-- Une réservation est un historique : le nom et le prix sont donc copiés dans la ligne.
CREATE TABLE bookings (
    id        VARCHAR(255) PRIMARY KEY,
    user_id   VARCHAR(255) NOT NULL,
    event_id  VARCHAR(255) NOT NULL,
    booked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status    VARCHAR(32)  NOT NULL,
    total     INTEGER      NOT NULL,
    fees      INTEGER      NOT NULL
);

-- Lister les réservations d'un utilisateur (US-15).
CREATE INDEX idx_bookings_user_id ON bookings (user_id);

CREATE TABLE booking_lines (
    booking_id VARCHAR(255) NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    ticket_id  VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price INTEGER      NOT NULL,
    PRIMARY KEY (booking_id, ticket_id)
);

-- Calculer les places déjà bloquées pour un ticket.
CREATE INDEX idx_booking_lines_ticket_id ON booking_lines (ticket_id);
