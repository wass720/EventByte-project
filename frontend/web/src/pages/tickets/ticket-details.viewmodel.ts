import { formatPrice } from "@/lib/format-price";
import { selectBooking, selectBookingEvent, selectIsBookingLoading } from "@/core/booking/booking.slice";
import { createSelector } from "@reduxjs/toolkit";

type LoadingState = {
    type: 'loading'
}

type NotFoundState = {
    type: 'not-found'
}

type LoadedState = {
    type: 'loaded'
    booking: {
        id: string
        bookedAt: string
    }
    event: {
        id: string
        title: string
        cover: string
        date: string
        time: string
        location: string
        venue: string
    }
    tickets: {
        id: string
        type: string
        quantity: number
        price: string
        unitPrice: string
    }[]
    total: string
    fees: string
}

export type TicketDetailsViewmodel = NotFoundState | LoadedState | LoadingState

export const selectTicketDetailsViewmodel = (bookingId: string) => createSelector(
    [
        selectBooking(bookingId),
        selectIsBookingLoading(bookingId),
        selectBookingEvent(bookingId),
    ], (booking, isLoading, event): TicketDetailsViewmodel => {
        if (isLoading && !booking) {
            return {
                type: 'loading'
            }
        }

        if (!event || !booking) {
            return {
                type: 'not-found'
            }
        }

        if (booking.status !== 'confirmed') {
            return {
                type: 'not-found'
            }
        }

        const eventDate = new Date(event.startAt)

        const dateFormatter = new Intl.DateTimeFormat('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
        })

        const timeFormatter = new Intl.DateTimeFormat('en-US', {
            hour: '2-digit',
            minute: '2-digit',
        })

        const bookedAtDate = new Date(booking.bookedAt)
        const bookedAtFormatter = new Intl.DateTimeFormat('en-US', {
            month: 'long',
            day: 'numeric',
            year: 'numeric',
        })

        return {
            type: 'loaded',
            booking: {
                id: booking.id,
                bookedAt: bookedAtFormatter.format(bookedAtDate)
            },
            event: {
                id: event.id,
                title: event.title,
                cover: event.cover,
                date: dateFormatter.format(eventDate),
                time: timeFormatter.format(eventDate),
                location: event.venue.adresse,
                venue: event.venue.name,
            },
            tickets: booking.lines.map(line => ({
                id: line.id,
                type: line.name,
                quantity: line.quantity,
                price: formatPrice(line.price * line.quantity),
                unitPrice: formatPrice(line.price),
            })),
            total: formatPrice(booking.total),
            fees: formatPrice(booking.fees),
        }
    })
