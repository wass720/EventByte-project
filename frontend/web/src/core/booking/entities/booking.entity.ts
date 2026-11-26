import { createEntityAdapter } from "@reduxjs/toolkit";
import { BookingLineEntity } from "./booking-line.entity";

export type BookingStatus = 'requested' | 'reserved' | 'rejected' | 'awaiting-payment' | 'expired' | 'confirmed'

export type BookingEntity = {
    id: string;
    eventId: string;
    bookedAt: string;
    status: BookingStatus
    total: number
    fees: number
    clientSecret?: string
    lines: BookingLineEntity[]
}

export const bookingsAdapter = createEntityAdapter<BookingEntity>()

export const bookingEntityBuilder = (entity: BookingEntity = {
    id: 'booking-1',
    eventId: 'event-1',
    bookedAt: new Date('2026-01-01').toISOString(),
    status: 'awaiting-payment',
    total: 0,
    fees: 0,
    clientSecret: 'pi_test_secret_test',
    lines: []
}) => ({
    withId: (id: string) => bookingEntityBuilder({ ...entity, id }),
    withEventId: (eventId: string) => bookingEntityBuilder({ ...entity, eventId }),
    withBookedAt: (bookedAt: Date) => bookingEntityBuilder({ ...entity, bookedAt: bookedAt.toISOString() }),
    withStatus: (status: BookingStatus) => bookingEntityBuilder({ ...entity, status }),
    withTotal: (total: number) => bookingEntityBuilder({ ...entity, total }),
    withFees: (fees: number) => bookingEntityBuilder({ ...entity, fees }),
    withClientSecret: (clientSecret: string) => bookingEntityBuilder({ ...entity, clientSecret }),
    withLines: (lines: BookingLineEntity[]) => bookingEntityBuilder({ ...entity, lines }),
    build: () => entity
})
