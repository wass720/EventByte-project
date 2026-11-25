-- Date limite de paiement (US-14). Une réservation non payée expire à cette date.
ALTER TABLE bookings ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
UPDATE bookings SET expires_at = booked_at + INTERVAL '10 minutes';
ALTER TABLE bookings ALTER COLUMN expires_at SET NOT NULL;

-- Retrouver vite les réservations à faire expirer.
CREATE INDEX idx_bookings_expirable ON bookings (expires_at)
    WHERE status IN ('reserved', 'awaiting-payment');
