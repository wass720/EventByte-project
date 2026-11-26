import { formatPrice } from "@/lib/format-price";
import { RootState } from "@/core/create-store"
import { selectEventById, selectIsEventLoading } from "@/core/events/events.slice"
import { createSelector } from "@reduxjs/toolkit"

type LoadingState = {
    type: 'loading'
}

type LoadedState = {
    type: 'loaded'
    event: {
        id: string
        title: string
        cover: string
        date: string
        time: string
        venue: {
            name: string
            address: string
            coordinates: {
                latitude: number
                longitude: number
            }
        }
        likedCount: number
        lowestPrice: string
        description: string
    }
}

type NotFoundState = {
    type: 'not-found'
}

export type EventPageViewmodel = LoadingState | LoadedState | NotFoundState

export const createEventPageViewmodel = ({ id }: { id: string }): (state: RootState) => EventPageViewmodel => createSelector(
    [selectEventById(id), selectIsEventLoading(id)],
    (event, isLoading) => {
        if (!event && isLoading) {
            return {
                type: 'loading'
            }
        }

        if (!event) {
            return {
                type: 'not-found'
            }
        }

        const dateFormatter = new Intl.DateTimeFormat('en-US', {
            weekday: 'short',
            day: 'numeric',
            month: 'short'
        })
        const timeFormatter = new Intl.DateTimeFormat('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        })

        return {
            type: 'loaded',
            event: {
                id: event.id,
                title: event.title,
                cover: event.cover,
                likedCount: event.likedCount,
                lowestPrice: formatPrice(event.lowestPrice),
                date: dateFormatter.format(new Date(event.startAt)),
                time: timeFormatter.format(new Date(event.startAt)),
                venue: {
                    name: event.venue.name,
                    address: event.venue.adresse,
                    coordinates: {
                        latitude: event.venue.coordinates.latitude,
                        longitude: event.venue.coordinates.longitude
                    }
                },
                description: event.description
            }
        }
    }
)
