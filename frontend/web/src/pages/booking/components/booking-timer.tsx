import { FC, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { selectBookingTimerViewmodel } from "./booking-timer.viewmodel";
import { Skeleton } from "@/design-system/components/ui/skeleton";
import { useParams } from "@tanstack/react-router";

export const BookingTimer: FC = () => {
    const [now, setNow] = useState(new Date())
    const params = useParams({ from: '/booking/booking/$id' })
    const viewmodel = useSelector(selectBookingTimerViewmodel({ now, bookingId: params.id }))

    useEffect(() => {
        const interval = setInterval(() => {
            setNow(new Date())
        }, 1000)

        return () => clearInterval(interval)
    }, [])

    if (viewmodel.type === 'loading') return (
        <Skeleton className="h-6 w-12" />
    )

    if (viewmodel.type === 'hidden') return null

    return (
        <span>
            {viewmodel.timeLeft}
        </span>
    );
}
