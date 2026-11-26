-- Paiement Stripe d'une réservation (US-16) : renseignés au passage en "awaiting-payment".
-- payment_intent_id servira à retrouver la réservation depuis le webhook Stripe (US-17).
ALTER TABLE bookings ADD COLUMN payment_intent_id VARCHAR(255);
ALTER TABLE bookings ADD COLUMN client_secret     VARCHAR(255);

CREATE UNIQUE INDEX idx_bookings_payment_intent_id ON bookings (payment_intent_id)
    WHERE payment_intent_id IS NOT NULL;
