package fr.soft_labs.eventbyte.ticket.application.port;

/**
 * Port sortant : nombre de places bloquées par des réservations en cours
 * (statuts "reserved" et "awaiting-payment") pour un type de ticket.
 *
 * C'est le point d'accroche avec l'Epic 4 (Réservation) : son implémentation
 * Redis remplacera NoReservedQuantities.
 */
public interface ReservedQuantities {

    int reservedQuantity(String ticketId);
}
