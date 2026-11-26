import { userAuthenticated } from "@/core/auth/auth.slice";
import { AppDispatch, Dependencies, RootState } from "@/core/create-store";
import { isAnyOf, ListenerMiddlewareInstance } from "@reduxjs/toolkit";
import { loadLikedEvents } from "../../usecases/load-liked-events/load-liked-events.usecase";
import { bookingChanged } from "@/core/booking/booking.slice";

export const onUserAuthenticated = (listener: ListenerMiddlewareInstance<RootState, AppDispatch, Dependencies>,) => {
    // US-18 : un seul flux des réservations, celui de l'utilisateur connecté.
    let unsubscribeFromBookings: (() => void) | undefined

    listener.startListening({
        matcher: isAnyOf(userAuthenticated),
        effect: (action: ReturnType<typeof userAuthenticated>, { dispatch, extra }) => {
            // Déconnexion ou changement d'utilisateur : on ferme le flux précédent.
            unsubscribeFromBookings?.()
            unsubscribeFromBookings = undefined

            if (!action.payload) {
                return
            }
            dispatch(loadLikedEvents())

            unsubscribeFromBookings = extra.bookingGateway.subscribeToBookingChanges((booking) => {
                dispatch(bookingChanged(booking))
            })
        }
    })

    return listener.middleware
}
