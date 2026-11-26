import { createAction, createSlice } from "@reduxjs/toolkit";
import { bookingsAdapter } from "./entities/booking.entity";
import { loadBookingById } from "./usecases/load-booking-by-id/load-booking-by-id";
import { RootState } from "../create-store";
import { selectEventById } from "../events/events.slice";
import { payBooking } from "./usecases/pay-booking/pay-booking";
import { loadUserBookings } from "./usecases/load-user-bookings/load-user-bookings";
import { Booking } from "./ports/booking.gateway";

// Le gateway fournit bookedAt en Date ; une action Redux doit rester sérialisable :
// on le convertit en ISO dès la création de l'action.
export const bookingChanged = createAction(
    'booking/changed',
    (booking: Booking) => ({
        payload: {
            ...booking,
            bookedAt: booking.bookedAt instanceof Date ? booking.bookedAt.toISOString() : booking.bookedAt,
        }
    })
)

const initialState = bookingsAdapter.getInitialState<{
    loadingBookings: Record<string, boolean>,
    isPaying: boolean,
    loadingUserBookings: boolean,
    // Paiement accepté par Stripe côté navigateur, en attente de la confirmation du backend
    // (webhook US-17, reçue par le flux SSE US-18).
    paidBookings: Record<string, boolean>
}>({
    loadingBookings: {},
    isPaying: false,
    loadingUserBookings: false,
    paidBookings: {}
})

export const bookingSlice = createSlice({
    name: 'booking',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder.addCase(loadBookingById.fulfilled, (state, action) => {
            const bookingId = action.payload.booking.id
            delete state.loadingBookings[bookingId]
            bookingsAdapter.setOne(state, {
                ...action.payload.booking,
            })
        }).addCase(loadBookingById.pending, (state, action) => {
            state.loadingBookings[action.meta.arg] = true
        }).addCase(loadBookingById.rejected, (state, action) => {
            delete state.loadingBookings[action.meta.arg]
        }).addCase(payBooking.pending, (state) => {
            state.isPaying = true
        }).addCase(payBooking.fulfilled, (state, action) => {
            state.isPaying = false
            state.paidBookings[action.meta.arg.bookingId] = true
        }).addCase(payBooking.rejected, (state) => {
            state.isPaying = false
        }).addCase(loadUserBookings.pending, (state) => {
            state.loadingUserBookings = true
        }).addCase(loadUserBookings.fulfilled, (state, action) => {
            state.loadingUserBookings = false
            const bookings = action.payload.bookings.map(b => ({
                id: b.id,
                eventId: b.eventId,
                bookedAt: b.bookedAt,
                status: b.status,
                total: b.total,
                fees: b.fees,
                clientSecret: b.clientSecret,
                lines: b.lines
            }))
            bookingsAdapter.setMany(state, bookings)
        }).addCase(loadUserBookings.rejected, (state) => {
            state.loadingUserBookings = false
        }).addCase(bookingChanged, (state, action) => {
            bookingsAdapter.setOne(state, {
                id: action.payload.id,
                eventId: action.payload.eventId,
                bookedAt: action.payload.bookedAt,
                status: action.payload.status,
                total: action.payload.total,
                fees: action.payload.fees,
                clientSecret: action.payload.clientSecret,
                lines: action.payload.lines
            })
        })
    }
})

export const bookingsSelectors = bookingsAdapter.getSelectors<RootState>((state) => state.booking)

export const selectIsBookingLoading = (bookingId: string) => (state: RootState) =>
    !!state.booking.loadingBookings[bookingId]

export const selectBookingEvent = (bookingId: string) => (state: RootState) => {
    const booking = bookingsSelectors.selectById(state, bookingId)
    if (!booking) {
        return null
    }
    return selectEventById(booking.eventId)(state)
}

export const selectBooking = (bookingId: string) => (state: RootState) =>
    bookingsSelectors.selectById(state, bookingId)

export const selectIsPaying = (state: RootState) => state.booking.isPaying

export const selectIsBookingPaid = (bookingId: string) => (state: RootState) =>
    !!state.booking.paidBookings[bookingId]

export const selectUserBookings = (state: RootState) =>
    bookingsSelectors.selectAll(state)

export const selectIsLoadingUserBookings = (state: RootState) => state.booking.loadingUserBookings
