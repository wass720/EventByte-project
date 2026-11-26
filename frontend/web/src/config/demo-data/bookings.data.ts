import { Booking } from "@/core/booking/ports/booking.gateway";

export const demoBookings: Booking[] = [
    {
        id: "booking-pending-1",
        eventId: "1",
        bookedAt: new Date(),
        lines: [
            {
                id: "early-bird",
                name: "Early Bird",
                quantity: 1,
                price: 1300,
            },
            {
                id: "regular",
                name: "Regular",
                quantity: 3,
                price: 2000,
            },
        ],
        status: "awaiting-payment",
        total: 7420,
        fees: 120,
        clientSecret: "pi_3STmGtQRC8413tQ31GYgZl8E_secret_8Hb4ERO0fjgNDw4pJUXADHtf8",
    },
    {
        id: "booking-upcoming-1",
        eventId: "1",
        bookedAt: new Date("2025-11-15T10:00:00Z"),
        lines: [
            {
                id: "vip",
                name: "VIP Pass",
                quantity: 2,
                price: 4599,
            },
        ],
        status: "confirmed",
        total: 9348,
        fees: 150,
        clientSecret: "",
    },
    {
        id: "booking-upcoming-2",
        eventId: "2",
        bookedAt: new Date("2025-11-20T14:30:00Z"),
        lines: [
            {
                id: "general",
                name: "General Admission",
                quantity: 1,
                price: 1820,
            },
        ],
        status: "confirmed",
        total: 1920,
        fees: 100,
        clientSecret: "",
    },
    {
        id: "booking-past-1",
        eventId: "5",
        bookedAt: new Date("2024-05-15T09:00:00Z"),
        lines: [
            {
                id: "standard",
                name: "Standard",
                quantity: 2,
                price: 3500,
            },
        ],
        status: "confirmed",
        total: 7150,
        fees: 150,
        clientSecret: "",
    },
];

export const demoBookingNextId = "1234";
