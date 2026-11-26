import { createTestStore } from "@/core/create-store";
import { stateBuilder } from "@/core/state-builder";
import { describe, expect, it } from "vitest";
import { selectBookingTimerViewmodel } from "./booking-timer.viewmodel";
import { bookingEntityBuilder } from "@/core/booking/entities/booking.entity";
import { Duration } from "@/core/shared/duration";

describe('Booking timer viewmodel', () => {
    it('is in loading state when booking is loading', () => {
        const store = createTestStore({}, stateBuilder().withBookingLoading({ id: 'booking-1', loading: true }).build())
        const viewmodel = selectBookingTimerViewmodel({ now: new Date(), bookingId: 'booking-1' })(store.getState())

        expect(viewmodel.type).toBe('loading')
    })

    it('is in loaded state when booking is loaded', () => {
        const store = createTestStore({}, stateBuilder()
            .withBooking(bookingEntityBuilder()
                .build())
            .withBookingLoading({ id: 'booking-1', loading: false })
            .build()
        )
        const viewmodel = selectBookingTimerViewmodel({ now: new Date(), bookingId: 'booking-1' })(store.getState())

        expect(viewmodel.type).toBe('loaded')
    })

    describe('loaded', () => {
        it('timeLeft is 10:00 when booking is booked at current time', () => {
            const now = new Date("2023-01-01T00:00:00.000Z")
            const store = createTestStore({}, stateBuilder()
                .withBooking(bookingEntityBuilder()
                    .withBookedAt(now).build()
                ).build()
            )
            const viewmodel = selectBookingTimerViewmodel({ now, bookingId: 'booking-1' })(store.getState())

            expect(viewmodel).toEqual(expect.objectContaining({
                timeLeft: '10:00'
            }))
        })

        it('timeLeft is 09:00 when booking is booked 1 minute ago', () => {
            const now = new Date("2023-01-01T00:00:00.000Z")
            const store = createTestStore({}, stateBuilder()
                .withBooking(bookingEntityBuilder()
                    .withBookedAt(new Date(now.getTime() - 60 * 1000)).build()
                ).build()
            )
            const viewmodel = selectBookingTimerViewmodel({ now, bookingId: 'booking-1' })(store.getState())

            expect(viewmodel).toEqual(expect.objectContaining({
                timeLeft: '09:00'
            }))
        })

        it('timeLeft is 09:59 when booking is booked 1 second ago', () => {
            const now = new Date("2023-01-01T00:00:00.000Z")
            const store = createTestStore({}, stateBuilder()
                .withBooking(bookingEntityBuilder()
                    .withBookedAt(Duration.fromDate(now).sub(Duration.fromSeconds(1)).toDate()).build()
                ).build()
            )
            const viewmodel = selectBookingTimerViewmodel({ now, bookingId: 'booking-1' })(store.getState())

            expect(viewmodel).toEqual(expect.objectContaining({
                timeLeft: '09:59'
            }))
        })

        it('timeLeft is 00:00 when booking is booked more than 10 minutes ago', () => {
            const now = new Date("2023-01-01T00:00:00.000Z")
            const store = createTestStore({}, stateBuilder()
                .withBooking(bookingEntityBuilder()
                    .withBookedAt(Duration.fromDate(now).sub(Duration.fromMinutes(11).sub(Duration.fromSeconds(1))).toDate()).build()
                ).build()
            )
            const viewmodel = selectBookingTimerViewmodel({ now, bookingId: 'booking-1' })(store.getState())

            expect(viewmodel).toEqual(expect.objectContaining({
                timeLeft: '00:00'
            }))
        })
    })

    it.each(["confirmed", "expired", "rejected"] as const)("is hidden once the booking is %s", (status) => {
        const store = createTestStore({}, stateBuilder().withBooking(bookingEntityBuilder().withStatus(status).build()).build())
        const viewmodel = selectBookingTimerViewmodel({ now: new Date(), bookingId: "booking-1" })(store.getState())

        expect(viewmodel.type).toBe("hidden")
    })
})
