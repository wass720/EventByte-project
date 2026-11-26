-- Les prix venaient de Strapi en euros mais étaient stockés tels quels, alors que le backend,
-- l'API (README) et Stripe travaillent en centimes. L'ingestion convertit désormais en centimes ;
-- on convertit ici les données déjà présentes.

UPDATE tickets SET price = price * 100;

UPDATE booking_lines SET unit_price = unit_price * 100;

-- total = lignes + frais. Les lignes étaient en euros, les frais (US-16) déjà en centimes.
UPDATE bookings SET total = (total - fees) * 100 + fees;
