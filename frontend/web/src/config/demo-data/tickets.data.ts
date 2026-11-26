import { Ticket } from "@/core/tickets/ports/tickets.gateway";

export const demoTickets: Ticket[] = [
    {
        id: "1-1",
        eventId: "1",
        name: "Early Bird",
        price: 1399,
        quantity: 0,
    },
    {
        id: "1-2",
        eventId: "1",
        name: "Regular",
        price: 1599,
        quantity: 5,
    },
    {
        id: "1-3",
        eventId: "1",
        name: "Last Chance",
        price: 2099,
        quantity: 2,
    },
    {
        id: "2-1",
        eventId: "2",
        name: "Last Chance",
        price: 2099,
        quantity: 2,
    },
    {
        id: "3-1",
        eventId: "3",
        name: "Last Chance",
        price: 2099,
        quantity: 2,
    },
    {
        id: "4-1",
        eventId: "4",
        name: "Last Chance",
        price: 2099,
        quantity: 2,
    },
];
