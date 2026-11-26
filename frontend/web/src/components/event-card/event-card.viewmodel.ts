import { formatPrice } from "@/lib/format-price";
import { RootState } from "@/core/create-store";
import { selectEventById } from "@/core/events/events.slice";
import { createSelector } from "@reduxjs/toolkit";

type Params = {
    id: string
}

type EventCardViewmodel = {
    title: string
    cover: string
    isSoldOut: boolean
    price: string
    date: string
    time: string
    venue: string
}

export const createEventCardViewmodel = ({ id }: Params):
    (state: RootState) => EventCardViewmodel =>
    createSelector([selectEventById(id)], (event) => {

        const startAtFormatter = new Intl.DateTimeFormat('en-US', {
            weekday: 'short',
            day: 'numeric',
            month: 'short',
        })

        const startTimeFormatter = new Intl.DateTimeFormat('en-US', {
            hour: 'numeric',
            minute: 'numeric',
        })

        return {
            title: event.title,
            cover: event.cover,
            isSoldOut: event.isSoldOut,
            price: formatPrice(event.lowestPrice),
            date: startAtFormatter.format(new Date(event.startAt)),
            time: startTimeFormatter.format(new Date(event.startAt)),
            venue: event.venue.name
        }
    })
