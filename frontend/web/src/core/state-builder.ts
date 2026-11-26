import { ActionCreatorWithPayload, createAction, createReducer } from "@reduxjs/toolkit";
import { rootReducer } from "./root-reducer";
import { EventEntity, eventsAdapter } from "./events/entities/event.entity";
import { AuthUserEntity } from "./auth/entities/auth-user.entity";
import { TicketEntity, ticketsEntityAdapter } from "./tickets/entities/ticket.entity";
import { CartItemEntity, cartItemEntityAdapter } from "./cart/entities/cart-item.entity";
import { BookingEntity, bookingsAdapter } from "./booking/entities/booking.entity";

const initialState = rootReducer(undefined, createAction('')() as never);

const withEvents = createAction<EventEntity[]>('withEvents');
const withLikedEvent = createAction<string>('withLikedEvent');
const withAuthUser = createAction<AuthUserEntity>('withAuthUser');
const withAuthChecked = createAction('withAuthChecked');
const withDiscoveredEvents = createAction<string[]>('withDiscoveredEvents');
const withDiscoverLoading = createAction<boolean>('withDiscoverLoading');
const withEventLoading = createAction<string>('withEventLoading');
const withLikedEventsLoading = createAction<boolean>('withLikedEventsLoading');
const withTickets = createAction<TicketEntity[]>('withTickets');
const withLoadingTicketsForEvent = createAction<string>('withLoadingTicketsForEvent');
const withCartTickets = createAction<CartItemEntity[]>('withCartTickets');
const withCartPromoCode = createAction<string | undefined>('withCartPromoCode');
const withBooking = createAction<BookingEntity>('withBooking');
const withBookingLoading = createAction<{ id: string, loading: boolean }>('withBookingLoading');
const withIsPaying = createAction<boolean>('withIsPaying');
const withUserBookingsLoading = createAction<boolean>('withUserBookingsLoading');
const withBookingPaid = createAction<string>('withBookingPaid');

const reducer = createReducer(initialState, (builder) => {
    builder
        .addCase(withEvents, (state, action) => {
            eventsAdapter.setMany(state.events, action.payload);
        }).addCase(withLikedEvent, (state, action) => {
            state.events.liked[action.payload] = true;
        }).addCase(withAuthUser, (state, action) => {
            state.auth.user = action.payload
        }).addCase(withAuthChecked, (state) => {
            state.auth.isAuthChecked = true
        }).addCase(withDiscoveredEvents, (state, action) => {
            state.events.discovered = action.payload
        }).addCase(withDiscoverLoading, (state, action) => {
            state.events.loadingDiscover = action.payload
        }).addCase(withEventLoading, (state, action) => {
            state.events.loadingEvents[action.payload] = true
        }).addCase(withLikedEventsLoading, (state, action) => {
            state.events.loadingLikedEvents = action.payload
        }).addCase(withTickets, (state, action) => {
            ticketsEntityAdapter.upsertMany(state.tickets, action.payload)
        }).addCase(withLoadingTicketsForEvent, (state, action) => {
            state.tickets.loading[action.payload] = true
        }).addCase(withCartTickets, (state, action) => {
            cartItemEntityAdapter.upsertMany(state.cart, action.payload)
        }).addCase(withCartPromoCode, (state, action) => {
            state.cart.promocode = action.payload
        }).addCase(withBooking, (state, action) => {
            bookingsAdapter.upsertOne(state.booking, action.payload)
        }).addCase(withIsPaying, (state, action) => {
            state.booking.isPaying = action.payload
        }).addCase(withBookingLoading, (state, action) => {
            state.booking.loadingBookings[action.payload.id] = action.payload.loading
        }).addCase(withUserBookingsLoading, (state, action) => {
            state.booking.loadingUserBookings = action.payload
        }).addCase(withBookingPaid, (state, action) => {
            state.booking.paidBookings[action.payload] = true
        })
});

export const stateBuilder = (baseState = initialState) => {
    const reduce =
        <T>(actionCreator: ActionCreatorWithPayload<T>) =>
            (payload: T) =>
                stateBuilder(reducer(baseState, actionCreator(payload)));

    const reduceNoPayload =
        (actionCreator: ActionCreatorWithPayload<undefined>) => () =>
            stateBuilder(reducer(baseState, actionCreator(undefined)));

    return {
        withAuthChecked: reduceNoPayload(withAuthChecked),
        withAuthUser: reduce(withAuthUser),
        withLiked: reduce(withLikedEvent),
        withEvents: reduce(withEvents),
        withDiscoveredEvents: reduce(withDiscoveredEvents),
        withDiscoverLoading: reduce(withDiscoverLoading),
        withEventLoading: reduce(withEventLoading),
        withLikedEventsLoading: reduce(withLikedEventsLoading),
        withTickets: reduce(withTickets),
        withLoadingTicketsForEvent: reduce(withLoadingTicketsForEvent),
        withCartTickets: reduce(withCartTickets),
        withCartPromoCode: reduce(withCartPromoCode),
        withBooking: reduce(withBooking),
        withBookingLoading: reduce(withBookingLoading),
        withIsPaying: reduce(withIsPaying),
        withUserBookingsLoading: reduce(withUserBookingsLoading),
        withBookingPaid: reduce(withBookingPaid),
        build: () => baseState
    }
}

export type StateBuilder = ReturnType<typeof stateBuilder>;
