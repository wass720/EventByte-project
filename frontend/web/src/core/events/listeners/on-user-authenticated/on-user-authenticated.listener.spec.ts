import { userAuthenticated } from "@/core/auth/auth.slice";
import { authUserEntityBuilder } from "@/core/auth/entities/auth-user.entity";
import { createTestStore, EMPTY_ARGS } from "@/core/create-store";
import { describe, expect, test } from "vitest";
import { loadLikedEvents } from "../../usecases/load-liked-events/load-liked-events.usecase";
import { FakeBookingGateway } from "@/core/booking/adapters/fake-booking.gateway";
import { bookingEntityBuilder } from "@/core/booking/entities/booking.entity";

describe('Feature: Load liked events on user authenticated', () => {
    test('Example: Load liked events', async () => {
        const store = createTestStore()
        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('1').build()))
        expect(store.getDispatchedUseCaseArgs(loadLikedEvents)).toEqual(EMPTY_ARGS)
    })

    test('Example: User is not auth liked events is NOT loaded', async () => {
        const store = createTestStore()
        store.dispatch(userAuthenticated())
        expect(store.getDispatchedUseCaseArgs(loadLikedEvents)).toEqual(undefined)
    })
})

describe('Feature: Follow my bookings live (US-18)', () => {
    test('Example: the bookings stream is opened on login', () => {
        const bookingGateway = new FakeBookingGateway()
        const store = createTestStore({ bookingGateway })

        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('1').build()))

        expect(bookingGateway.openSubscriptions).toBe(1)
    })

    test('Example: logging in again does not open a second stream', () => {
        const bookingGateway = new FakeBookingGateway()
        const store = createTestStore({ bookingGateway })

        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('1').build()))
        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('2').build()))

        expect(bookingGateway.openSubscriptions).toBe(1)
    })

    test('Example: the stream is closed on logout', () => {
        const bookingGateway = new FakeBookingGateway()
        const store = createTestStore({ bookingGateway })

        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('1').build()))
        store.dispatch(userAuthenticated())

        expect(bookingGateway.openSubscriptions).toBe(0)
    })

    test('Example: a booking change received on the stream updates the store', () => {
        const bookingGateway = new FakeBookingGateway()
        const store = createTestStore({ bookingGateway })
        store.dispatch(userAuthenticated(authUserEntityBuilder().withId('1').build()))

        bookingGateway.simulateBookingChange({
            ...bookingEntityBuilder().withStatus('confirmed').build(),
            bookedAt: new Date('2026-01-01'),
        })

        expect(store.getState().booking.entities['booking-1']?.status).toBe('confirmed')
    })
})
