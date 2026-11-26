import { AxiosInstance } from "axios"
import { z } from "zod"
import { EventSource } from "eventsource"
import {
    Booking,
    BookingGateway,
    BookParams,
    OnBookingChangeCallback,
} from "../ports/booking.gateway"

const BookingLineSchema = z.object({
    id: z.string(),
    name: z.string(),
    quantity: z.number(),
    price: z.number(),
})

const BookingStatusSchema = z.enum(['requested', 'reserved', 'rejected', 'awaiting-payment', 'expired', 'confirmed'])

const ApiBookingResponseSchema = z.object({
    id: z.string(),
    eventId: z.string(),
    bookedAt: z.string(),
    lines: z.array(BookingLineSchema),
    status: BookingStatusSchema,
    total: z.number(),
    fees: z.number(),
    clientSecret: z.string().optional(),
})

const RECONNECT_DELAY_MS = 5_000

const BookingIdSchema = z.object({
    id: z.string(),
})

const BookingsArraySchema = z.array(ApiBookingResponseSchema)

type ApiBookingResponse = z.infer<typeof ApiBookingResponseSchema>

export class ApiBookingGateway implements BookingGateway {
    constructor(
        private readonly client: AxiosInstance,
        private readonly getToken: () => string | undefined,
        private readonly apiUrl: string
    ) { }

    async book(params: BookParams): Promise<string> {
        const response = await this.client.post("/bookings", {
            items: params,
        })
        const validated = BookingIdSchema.parse(response.data)
        return validated.id
    }

    async getById(id: string): Promise<Booking | undefined> {
        try {
            const response = await this.client.get(`/bookings/${id}`)
            const validated = ApiBookingResponseSchema.parse(response.data)
            return this.mapBooking(validated)
        } catch (error) {
            if (
                error &&
                typeof error === "object" &&
                "response" in error &&
                (error as { response?: { status?: number } }).response?.status === 404
            ) {
                return undefined
            }
            throw error
        }
    }

    async getUserBookings(): Promise<Booking[]> {
        const response = await this.client.get("/bookings/user")
        const validated = BookingsArraySchema.parse(response.data)
        return validated.map(this.mapBooking)
    }

    /**
     * US-18 : GET /bookings/stream (SSE), un événement "booking_updated" par changement de statut.
     *
     * - Le token est relu à chaque (re)connexion : Keycloak le renouvelle régulièrement.
     * - Coupure réseau ou fin du flux côté serveur (toutes les 30 min) : la lib se reconnecte seule.
     * - Refus du serveur (401 token expiré, 5xx...) : la lib abandonne ; on rouvre un flux
     *   après RECONNECT_DELAY_MS avec un token frais.
     */
    subscribeToBookingChanges(callback: OnBookingChangeCallback): () => void {
        let eventSource: EventSource | undefined
        let retryTimer: ReturnType<typeof setTimeout> | undefined
        let unsubscribed = false

        const connect = () => {
            const source = new EventSource(`${this.apiUrl}/bookings/stream`, {
                fetch: (input, init) =>
                    fetch(input, {
                        ...init,
                        headers: {
                            ...init.headers,
                            Authorization: `Bearer ${this.getToken()}`,
                        },
                    }),
            })
            eventSource = source

            source.addEventListener('booking_updated', (event) => {
                try {
                    const data = JSON.parse(event.data)
                    const validated = ApiBookingResponseSchema.parse(data)
                    callback(this.mapBooking(validated))
                } catch (error) {
                    console.error('Error parsing SSE booking data:', error)
                }
            })

            source.addEventListener('error', () => {
                if (unsubscribed || source.readyState !== EventSource.CLOSED) {
                    return // reconnexion automatique en cours
                }
                retryTimer = setTimeout(connect, RECONNECT_DELAY_MS)
            })
        }

        connect()

        return () => {
            unsubscribed = true
            clearTimeout(retryTimer)
            eventSource?.close()
        }
    }

    private mapBooking(data: ApiBookingResponse): Booking {
        return {
            ...data,
            bookedAt: new Date(data.bookedAt),
        }
    }
}
