import { createTestStore } from "@/core/create-store";
import { stateBuilder } from "@/core/state-builder";
import { describe, expect, it } from "vitest";
import { eventEntityBuilder, venueEntityBuilder } from "@/core/events/entities/event.entity";
import { createEventCardViewmodel } from "./event-card.viewmodel";

const event = eventEntityBuilder()
    .withId('1')
    .withTitle('Event 1')
    .withCover("https://cover.com")
    .withLowestPrice(1000)
    .withVenue(venueEntityBuilder().withName("Gare de Lyon").build())
    .startAt(new Date("2026-01-01T18:30:00Z"))
    .build()

describe('EventCardViewmodel', () => {
    it('contain event details', () => {
        const initialState = stateBuilder().withEvents([event]).build()
        const store = createTestStore({}, initialState);
        const viewmodel = createEventCardViewmodel({ id: '1' })(store.getState())
        expect(viewmodel).toEqual(expect.objectContaining({
            title: 'Event 1',
            cover: "https://cover.com",
            isSoldOut: false,
            price: "€10.00",
            date: "Thu, Jan 1",
            time: "6:30 PM",
            venue: "Gare de Lyon"
        }))
    })
})
