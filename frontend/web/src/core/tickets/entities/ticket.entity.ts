import { createEntityAdapter } from "@reduxjs/toolkit"

export type TicketEntity = {
    id: string
    eventId: string
    name: string
    price: number
    quantity: number
}

export const ticketsEntityAdapter = createEntityAdapter<TicketEntity>()

export const ticketEntityBuilder = (ticket: TicketEntity = {
    id: '1',
    name: 'Regular',
    quantity: 1,
    eventId: '1',
    price: 1599,
}) => ({
    withId: (id: string) => ticketEntityBuilder({ ...ticket, id }),
    withName: (name: string) => ticketEntityBuilder({ ...ticket, name }),
    withPrice: (price: number) => ticketEntityBuilder({ ...ticket, price }),
    withQuantity: (quantity: number) => ticketEntityBuilder({ ...ticket, quantity }),
    withEventId: (eventId: string) => ticketEntityBuilder({ ...ticket, eventId }),
    build: () => ticket
})
