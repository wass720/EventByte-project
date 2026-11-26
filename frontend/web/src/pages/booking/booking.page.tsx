import { FC, useState } from "react";
import { useSelector } from "react-redux";
import { BookingViewmodel, selectBookingViewmodel } from "./booking.viewmodel";
import { exhaustiveCheck } from "@/lib/exhaustive-check";
import { Button } from "@/design-system/components/ui/button";
import { Check, ChevronLeft, Hourglass, X } from "@/components/icons";
import { Link, useParams } from "@tanstack/react-router";
import {
    Elements,
    PaymentElement,
    useElements,
} from "@stripe/react-stripe-js";
import { Card, CardContent } from "@/design-system/components/ui/card";
import { Skeleton } from "@/design-system/components/ui/skeleton";
import { Separator } from "@/design-system/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/design-system/components/ui/tooltip";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/design-system/components/ui/collapsible";
import { useAppDispatch } from "@/hooks/store-hooks";
import { payBooking } from "@/core/booking/usecases/pay-booking/pay-booking";
import { Loader, Calendar, MapPin, Info, ChevronDown } from "lucide-react";
import { useDependencies } from "@/context/dependencies.context";
import { selectIsPaying } from "@/core/booking/booking.slice";

export const BookingPage: FC = () => {
    const params = useParams({ from: '/booking/booking/$id' })
    const viewmodel = useSelector(selectBookingViewmodel({ bookingId: params.id }))

    switch (viewmodel.type) {
        case 'loading':
            return <Loading />
        case 'loaded':
            return <Loaded viewmodel={viewmodel} />
        case 'expired':
            return <Timeout eventId={viewmodel.eventId} />
        case 'confirmed':
            return <Approved eventId={viewmodel.eventId} />
        case 'rejected':
            return <Rejected eventId={viewmodel.eventId} />
        case 'processing':
            return <Processing />
        default:
            exhaustiveCheck(viewmodel)
    }
}

type LoadedProps = {
    viewmodel: Extract<BookingViewmodel, { type: 'loaded' }>
}

const Loaded: FC<LoadedProps> = ({ viewmodel }) => {
    const { stripe } = useDependencies()
    const [isOpen, setIsOpen] = useState(false)

    return (
        <div className="space-y-8">
            <h1 className="text-3xl font-bold tracking-tight">Review & Pay</h1>

            <Card>
                <CardContent className="space-y-6">
                    <div className="flex gap-4">
                        <img
                            src={viewmodel.event.cover}
                            alt={viewmodel.event.title}
                            className="w-32 h-32 object-cover rounded aspect-square"
                        />
                        <div className="flex-1 space-y-4">
                            <h2 className="text-2xl font-semibold">{viewmodel.event.title}</h2>
                            <div className="space-y-2">
                                <div className="flex items-center gap-2">
                                    <Calendar className="h-4 w-4 text-muted-foreground" />
                                    <span className="text-highlight font-medium">{viewmodel.event.date}</span>
                                    <span className="text-muted-foreground">•</span>
                                    <span className="text-muted-foreground">{viewmodel.event.time}</span>
                                </div>
                                <div className="flex items-center gap-2 text-muted-foreground">
                                    <MapPin className="h-4 w-4" />
                                    <span>
                                        {viewmodel.event.venue}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Collapsible open={isOpen} onOpenChange={setIsOpen}>
                <Card>
                    <CardContent>
                        <CollapsibleTrigger asChild>
                            <button className="flex items-center justify-between w-full py-4 text-left hover:opacity-70 transition-opacity">
                                <span className="font-semibold">View Details</span>
                                <ChevronDown className={`h-5 w-5 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
                            </button>
                        </CollapsibleTrigger>

                        <CollapsibleContent className="space-y-4 pt-2">
                            {viewmodel.lines.map((line) => (
                                <div key={line.id} className="flex justify-between items-center">
                                    <div className="space-y-1">
                                        <p className="font-medium">{line.name}</p>
                                        <p className="text-sm text-muted-foreground">
                                            Quantity: {line.quantity} × {line.unitPrice}
                                        </p>
                                    </div>
                                    <p className="font-semibold">{line.total}</p>
                                </div>
                            ))}
                            <Separator />
                            <div className="flex justify-between items-center text-sm text-muted-foreground">
                                <span>Service Fees</span>
                                <div className="flex gap-2 items-center">
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <Info className="w-4 h-4" />
                                        </TooltipTrigger>
                                        <TooltipContent className="max-w-60">
                                            Service fees help cover technical costs related to the transaction, ticket issuance, and data security. They are non-refundable. In case of a refund, additional service fees may apply.
                                        </TooltipContent>
                                    </Tooltip>
                                    <span>{viewmodel.fees}</span>
                                </div>
                            </div>
                            <div className="flex justify-between items-center text-lg font-bold">
                                <span>Total</span>
                                <span>{viewmodel.total}</span>
                            </div>
                        </CollapsibleContent>
                    </CardContent>
                </Card>
            </Collapsible>

            <Card>
                <CardContent>
                    <Elements stripe={stripe} options={{
                        clientSecret: viewmodel.clientSecret,
                        appearance: {
                            theme: "night"
                        }
                    }}>
                        <PaymentForm price={viewmodel.total} id={viewmodel.id} />
                    </Elements>
                </CardContent>
            </Card>
        </div>
    );
}

type PaymentFormProps = {
    price: string
    id: string
}

const PaymentForm: FC<PaymentFormProps> = ({ price, id }) => {
    const [isComplete, setIsComplete] = useState(false)
    const elements = useElements()
    const dispatch = useAppDispatch()
    const isPaying = useSelector(selectIsPaying)
    const disableSubmit = !isComplete || isPaying

    const onSubmit = async () => {
        try {
            await dispatch(payBooking({ paymentContext: elements, bookingId: id })).unwrap()
        } catch (error) {

        }
    }

    return (
        <form className="space-y-4" action={onSubmit}>
            <PaymentElement options={{ layout: "tabs" }} onChange={(e) => setIsComplete(e.complete)} />
            <Button disabled={disableSubmit} type="submit" className="w-full">
                {isPaying && <Loader className=" animate-spin" />}
                PAY {price}
            </Button>
        </form>
    )
}

const Loading: FC = () => {
    return (
        <div className="space-y-8">
            <Skeleton className="h-10 w-64" />
            <Skeleton className="h-48" />
            <Skeleton className="h-16" />
            <Skeleton className="h-96" />
        </div>
    );
}

type TimeoutProps = {
    eventId: string
}

const Timeout: FC<TimeoutProps> = ({ eventId }) => {
    return (
        <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
            <div className="flex flex-col items-center gap-8 max-w-2xl">
                <Hourglass className="w-24 h-24 text-muted-foreground" />

                <h1 className="text-3xl font-bold">
                    TIME IS UP
                </h1>

                <div className="space-y-4 text-lg text-muted-foreground">
                    <p>
                        We had to interrupt your reservation because you have exceeded the 10 minutes limit.
                    </p>
                    <p>
                        Unfortunately, we cannot hold it longer for you, as this would not be fair for other users.
                    </p>
                    <p>
                        Please navigate back to the Tickets page and retry.
                    </p>
                </div>

                <Button
                    size="lg"
                    className="mt-4"
                    asChild
                >
                    <Link to="/event/$id" params={{ id: eventId }}>
                        <ChevronLeft />
                        GO BACK TO TICKETS
                    </Link>
                </Button>
            </div>
        </div >
    );
}

type ApprovedProps = {
    eventId: string
}

const Approved: FC<ApprovedProps> = ({ eventId }) => {
    return (
        <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
            <div className="flex flex-col items-center gap-8 max-w-2xl">
                <Check className="w-24 h-24 text-green-500" />
                <h1 className="text-3xl font-bold">
                    SUCCESS!
                </h1>
                <p className="text-lg text-muted-foreground">
                    You have successfully reserved your tickets.
                </p>
                <Button
                    size="lg"
                    className="mt-4"
                    asChild
                >
                    <Link to="/event/$id" params={{ id: eventId }}>
                        <ChevronLeft />
                        GO BACK TO TICKETS
                    </Link>
                </Button>
            </div>
        </div >
    );
}

const Processing: FC = () => {
    return (
        <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
            <div className="flex flex-col items-center gap-8 max-w-2xl">
                <Loader className="w-24 h-24 text-muted-foreground animate-spin" />

                <h1 className="text-3xl font-bold">
                    PROCESSING YOUR BOOKING
                </h1>

                <div className="space-y-4 text-lg text-muted-foreground">
                    <p>
                        Your booking is being processed. This may take a few moments.
                    </p>
                    <p>
                        This page updates automatically as soon as your payment is confirmed.
                    </p>
                </div>
            </div>
        </div >
    );
}

type RejectedProps = {
    eventId: string
}

const Rejected: FC<RejectedProps> = ({ eventId }) => {
    return (
        <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
            <div className="flex flex-col items-center gap-8 max-w-2xl">
                <X className="w-24 h-24 text-red-500" />

                <h1 className="text-3xl font-bold">
                    BOOKING REJECTED
                </h1>

                <div className="space-y-4 text-lg text-muted-foreground">
                    <p>
                        Unfortunately, your booking request has been rejected.
                    </p>
                    <p>
                        This could be due to insufficient ticket availability or payment issues.
                    </p>
                    <p>
                        Please navigate back to the Tickets page and try again.
                    </p>
                </div>

                <Button
                    size="lg"
                    className="mt-4"
                    asChild
                >
                    <Link to="/event/$id" params={{ id: eventId }}>
                        <ChevronLeft />
                        GO BACK TO TICKETS
                    </Link>
                </Button>
            </div>
        </div >
    );
}
