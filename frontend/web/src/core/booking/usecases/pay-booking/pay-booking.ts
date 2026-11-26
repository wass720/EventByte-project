import { createAppAsyncThunk } from "@/core/create-app-async-thunks";
import { selectBooking } from "../../booking.slice";


type Params = {
    paymentContext: unknown
    bookingId: string
}

export const payBooking = createAppAsyncThunk("booking/pay", async ({ paymentContext, bookingId }: Params, { extra: { paymentGateway }, getState, rejectWithValue }) => {
    const booking = selectBooking(bookingId)(getState())
    if (!booking) {
        return rejectWithValue("Booking not found")
    }
    if (!booking.clientSecret) {
        return rejectWithValue("Booking is not awaiting payment")
    }

    await paymentGateway.payBooking(booking.clientSecret, paymentContext)
})
