import { FakeTicketsGateway } from "@/core/tickets/adapters/fake-tickets.gateway";
import { Booking, BookingGateway, BookParams, OnBookingChangeCallback } from "../ports/booking.gateway";

export class FakeBookingGateway implements BookingGateway {
    nextId: string = ''
    bookings: Map<string, Booking> = new Map();
    private bookingSubscriptions: OnBookingChangeCallback[] = [];

    constructor(
        private readonly delay = 0,
        private readonly ticketsGateway?: FakeTicketsGateway,
        private readonly clientSecret?: string
    ) { }

    async book(params: BookParams): Promise<string> {
        await new Promise(resolve => setTimeout(resolve, this.delay))

        if (!this.ticketsGateway || params.length === 0) {
            return this.nextId
        }

        const firstTicketId = params[0].id
        const firstTicket = this.ticketsGateway.getTicketById(firstTicketId)

        if (!firstTicket) {
            return this.nextId
        }

        const lines = params.map(param => {
            const ticket = this.ticketsGateway?.getTicketById(param.id)
            if (!ticket) {
                throw new Error(`Ticket ${param.id} not found`)
            }
            return {
                id: ticket.id,
                name: ticket.name,
                quantity: param.quantity,
                price: ticket.price
            }
        })

        // Montants en centimes, comme l'API : 30 centimes de frais par ticket, inclus dans le total.
        const subtotal = lines.reduce((sum, line) => sum + (line.price * line.quantity), 0)
        const fees = lines.reduce((sum, line) => sum + (line.quantity * 30), 0)
        const total = subtotal + fees

        const booking: Booking = {
            id: this.nextId,
            eventId: firstTicket.eventId,
            bookedAt: new Date(),
            lines,
            status: 'awaiting-payment',
            total,
            fees,
            clientSecret: this.clientSecret || `client_secret_${this.nextId}`
        }

        this.bookings.set(booking.id, booking)

        return this.nextId
    }

    async getById(id: string): Promise<Booking | undefined> {
        await new Promise(resolve => setTimeout(resolve, this.delay))
        return this.bookings.get(id)
    }

    async getUserBookings(): Promise<Booking[]> {
        await new Promise(resolve => setTimeout(resolve, this.delay))
        return Array.from(this.bookings.values())
    }

    subscribeToBookingChanges(callback: OnBookingChangeCallback): () => void {
        this.bookingSubscriptions.push(callback);

        return () => {
            const index = this.bookingSubscriptions.indexOf(callback);
            if (index > -1) {
                this.bookingSubscriptions.splice(index, 1);
            }
        };
    }

    get openSubscriptions(): number {
        return this.bookingSubscriptions.length
    }

    addBooking(booking: Booking) {
        this.bookings.set(booking.id, booking)
    }

    simulateBookingChange(booking: Booking) {
        this.bookings.set(booking.id, booking);
        this.bookingSubscriptions.forEach(callback => callback(booking));
    }
}
