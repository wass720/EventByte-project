import { describe, expect, it } from "vitest";
import { createTestStore } from "@/core/create-store";
import { eventEntityBuilder, venueEntityBuilder } from "@/core/events/entities/event.entity";
import { createEventPageViewmodel } from "./event.viewmodel";
import { stateBuilder } from "@/core/state-builder";

const event = eventEntityBuilder()
    .withId('1')
    .withTitle('TELETECH PARIS')
    .withCover('https://cover.com')
    .withLikedCount(8300)
    .withLowestPrice(4900)
    .startAt(new Date('2025-11-22T21:30:00Z'))
    .withDescription("Description")
    .withVenue(venueEntityBuilder().withName('Bourse').withAdresse('Place de la Bourse').withCoordinates({ latitude: 48.8566, longitude: 2.3522 }).build())
    .build()

describe('Event Page Viewmodel', () => {
    it('is in loaded state when event exist', () => {
        const store = createTestStore({}, stateBuilder().withEvents([event]).build())

        const viewmodel = createEventPageViewmodel({ id: '1' })(store.getState())

        expect(viewmodel).toEqual(expect.objectContaining({
            type: 'loaded',
        }))
    })

    it('is in loaded state when event exists even if loading', () => {
        const store = createTestStore({}, stateBuilder().withEvents([event]).withEventLoading('1').build())

        const viewmodel = createEventPageViewmodel({ id: '1' })(store.getState())

        expect(viewmodel).toEqual(expect.objectContaining({
            type: 'loaded',
        }))
    })

    it('is in loading state when event does not exist and is loading', () => {
        const store = createTestStore({}, stateBuilder().withEventLoading('1').build())

        const viewmodel = createEventPageViewmodel({ id: '1' })(store.getState())

        expect(viewmodel).toEqual(expect.objectContaining({
            type: 'loading',
        }))
    })

    it('is in not found state when event does not exist and is not loading', () => {
        const store = createTestStore({}, stateBuilder().build())

        const viewmodel = createEventPageViewmodel({ id: '1' })(store.getState())

        expect(viewmodel).toEqual(expect.objectContaining({
            type: 'not-found',
        }))
    })

    it('should return event details', () => {
        const store = createTestStore({}, stateBuilder().withEvents([event]).build())

        const viewmodel = createEventPageViewmodel({ id: '1' })(store.getState())

        expect(viewmodel).toEqual(
            expect.objectContaining({
                event: {
                    id: '1',
                    title: 'TELETECH PARIS',
                    cover: 'https://cover.com',
                    date: "Sat, Nov 22",
                    time: "9:30 PM",
                    likedCount: 8300,
                    lowestPrice: "€49.00",
                    venue: {
                        name: 'Bourse',
                        address: 'Place de la Bourse',
                        coordinates: {
                            latitude: 48.8566,
                            longitude: 2.3522
                        }
                    },
                    description: "Description"
                }
            })
        )
    })
})
