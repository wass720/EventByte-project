import { selectBooking, selectIsBookingLoading } from "@/core/booking/booking.slice";
import { RootState } from "@/core/create-store";
import { Duration } from "@/core/shared/duration";
import { createSelector } from "@reduxjs/toolkit";

type LoadingState = {
    type: 'loading'
}
type LoadedState = {
    type: 'loaded'
    timeLeft: string
}

/** Réservation confirmée, expirée ou rejetée : plus de délai de paiement à afficher. */
type HiddenState = {
    type: 'hidden'
}

type Viewmodel = LoadingState | LoadedState | HiddenState

const PAYMENT_PENDING_STATUSES = ['requested', 'reserved', 'awaiting-payment']

const EXPIRATION_DURATION = Duration.fromMinutes(10)

type Params = {
    now: Date,
    bookingId: string
}

export const selectBookingTimerViewmodel = ({ now, bookingId }: Params): (state: RootState) => Viewmodel => createSelector(
    [
        selectBooking(bookingId),
        selectIsBookingLoading(bookingId)
    ], (booking, isLoading) => {
        if (isLoading || !booking) {
            return {
                type: 'loading'
            }
        }

        if (!PAYMENT_PENDING_STATUSES.includes(booking.status)) {
            return {
                type: 'hidden'
            }
        }

        const expirationDate = Duration.fromDate(new Date(booking.bookedAt)).add(EXPIRATION_DURATION)
        const remaining = expirationDate.sub(Duration.fromDate(now))

        const minutes = Math.max(0, Math.floor(remaining.minutes))
        const seconds = Math.max(0, Math.floor(remaining.seconds) % 60)

        const format = (num: number) => num.toString().padStart(2, '0')

        return {
            type: 'loaded',
            timeLeft: `${format(minutes)}:${format(seconds)}`
        }
    })
