const euroFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
});

/**
 * Tous les montants de l'API sont en centimes (voir README) : 2550 -> "€25.50".
 * Ne jamais formater un montant de l'API sans passer par ici.
 */
export const formatPrice = (cents: number): string => euroFormatter.format(cents / 100)
