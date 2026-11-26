import { formatPrice } from "@/lib/format-price";
import { selectBooking, selectBookingEvent, selectIsBookingLoading, selectIsBookingPaid } from "@/core/booking/booking.slice";
import { RootState } from "@/core/create-store";
import { createSelector } from "@reduxjs/toolkit";

type ExpiredState = {
    type: 'expired',
    eventId: string
}

type ConfirmedState = {
    type: 'confirmed',
    eventId: string
}

type RejectedState = {
    type: 'rejected',
    eventId: string
}

type ProcessingState = {
    type: 'processing',
    eventId: string
}

type LoadingState = {
    type: 'loading'
}

type LoadedState = {
    type: 'loaded',
    id: string
    event: {
        id: string
        title: string
        cover: string
        date: string
        time: string
        venue: string
    },
    lines: {
        id: string
        name: string
        quantity: number
        unitPrice: string
        total: string
    }[]
    itemsCount: number
    fees: string
    total: string
    clientSecret: string
}

export type BookingViewmodel = LoadingState | LoadedState | ExpiredState | ConfirmedState | RejectedState | ProcessingState

type Params = {
    bookingId: string
}

export const selectBookingViewmodel = ({ bookingId }: Params): (state: RootState) => BookingViewmodel => createSelector(
    [
        selectIsBookingLoading(bookingId),
        selectBooking(bookingId),
        selectBookingEvent(bookingId),
        selectIsBookingPaid(bookingId),
    ], (isBookingLoading, booking, event, isPaid) => {
        if (isBookingLoading || !booking || !event) {
            return {
                type: 'loading'
            }
        }

        if (booking.status === 'expired') {
            return {
                type: 'expired',
                eventId: booking.eventId
            }
        }

        if (booking.status === 'confirmed') {
            return {
                type: 'confirmed',
                eventId: booking.eventId
            }
        }

        if (booking.status === 'rejected') {
            return {
                type: 'rejected',
                eventId: booking.eventId
            }
        }

        // En cours côté serveur, ou payé et en attente de la confirmation Stripe (US-17),
        // qui arrive par le flux SSE (US-18) : pas de formulaire de paiement à afficher.
        if (booking.status === 'requested' || booking.status === 'reserved' || isPaid || !booking.clientSecret) {
            return {
                type: 'processing',
                eventId: booking.eventId
            }
        }

        const itemsCount = booking.lines.reduce((acc, line) => acc + line.quantity, 0)

        const eventDate = new Date(event.startAt);

        const dateFormatter = new Intl.DateTimeFormat('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
        });

        const timeFormatter = new Intl.DateTimeFormat('en-US', {
            hour: '2-digit',
            minute: '2-digit',
        });

        return {
            type: 'loaded',
            id: booking.id,
            event: {
                id: booking.eventId,
                title: event.title,
                cover: event.cover,
                date: dateFormatter.format(eventDate),
                time: timeFormatter.format(eventDate),
                venue: `${event.venue.name}, ${event.venue.adresse}`,
            },
            lines: booking.lines.map(line => {
                const total = line.price * line.quantity
                return {
                    id: line.id,
                    name: line.name,
                    quantity: line.quantity,
                    unitPrice: formatPrice(line.price),
                    total: formatPrice(total)
                }
            }),
            itemsCount,
            fees: formatPrice(booking.fees),
            total: formatPrice(booking.total),
            clientSecret: booking.clientSecret
        }
    })
