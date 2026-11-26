import { createTestStore, EMPTY_ARGS } from "@/core/create-store";
import { describe, expect, it, vitest } from "vitest";
import { selectCartViewmodel, SelectCartViewmodelProps } from "./cart.viewmodel";
import { stateBuilder } from "@/core/state-builder";
import { cartItemEntityBuilder } from "@/core/cart/entities/cart-item.entity";
import { ticketEntityBuilder } from "@/core/tickets/entities/ticket.entity";
import { addToCart } from "@/core/cart/usecases/add-ticket-to-cart/add-ticket-to-cart";
import { removeFromCart } from "@/core/cart/usecases/remove-ticket-from-cart/remove-ticket-from-cart";
import { bookCart } from "@/core/booking/usecases/book-cart/book-cart";
import { FakeBookingGateway } from "@/core/booking/adapters/fake-booking.gateway";

const selectCartViewmodelTest = ({ dispatch = vitest.fn(), onBookSuccess = vitest.fn() }: Partial<SelectCartViewmodelProps> = {}) => selectCartViewmodel({ dispatch, onBookSuccess });

describe('Cart viewmodel', () => {
    describe('items count', () => {
        it('cart is empty', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.itemsCount).toBe(0);
        })

        it('cart has many lines', () => {
            const store = createTestStore({}, stateBuilder().withCartTickets([cartItemEntityBuilder().withId('1').build(), cartItemEntityBuilder().withId('2').build(),]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.itemsCount).toBe(2);
        })

        it('cart has one line with many quantity', () => {
            const store = createTestStore({}, stateBuilder().withCartTickets([cartItemEntityBuilder().withId('1').withQuantity(2).build(),]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.itemsCount).toBe(2);
        })
    })

    describe('total', () => {
        it('cart is empty', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.total).toBe('€0.00');
        })

        it('one item one quantity', () => {
            const store = createTestStore({}, stateBuilder().withTickets([ticketEntityBuilder().withId('1').withPrice(1000).build()]).withCartTickets([cartItemEntityBuilder().withId('1').withQuantity(1).build(),]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.total).toBe('€10.30');
        })

        it('two items one quantity', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build(),
                    ticketEntityBuilder().withId('2').withPrice(1000).build()
                ]).withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(1).build(),
                    cartItemEntityBuilder().withId('2').withQuantity(1).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.total).toBe('€20.60');
        })

        it('two items 2 quantity', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build(),
                    ticketEntityBuilder().withId('2').withPrice(1000).build()
                ]).withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(2).build(),
                    cartItemEntityBuilder().withId('2').withQuantity(2).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.total).toBe('€41.20');
        })

        it('two items 2 quantity with different prices', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build(),
                    ticketEntityBuilder().withId('2').withPrice(550).build()
                ]).withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(2).build(),
                    cartItemEntityBuilder().withId('2').withQuantity(2).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.total).toBe('€32.20');
        })
    })

    describe('fees', () => {
        it('cart is empty', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.fees).toBe('€0.00');
        })

        it('one item one quantity', () => {
            const store = createTestStore({}, stateBuilder().withCartTickets([cartItemEntityBuilder().withId('1').withQuantity(1).build(),]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.fees).toBe('€0.30');
        })

        it('two items one quantity', () => {
            const store = createTestStore({}, stateBuilder().withCartTickets([
                cartItemEntityBuilder().withId('1').withQuantity(1).build(),
                cartItemEntityBuilder().withId('2').withQuantity(1).build(),
            ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.fees).toBe('€0.60');
        })

        it('one item many quantity', () => {
            const store = createTestStore({}, stateBuilder().withCartTickets([
                cartItemEntityBuilder().withId('1').withQuantity(3).build(),
            ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.fees).toBe('€0.90');
        })
    })

    describe('lines', () => {
        it('cart is empty', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([]);
        })

        it('one item one quantity', () => {
            const store = createTestStore({}, stateBuilder().withTickets([ticketEntityBuilder().withId('1').withPrice(1000).build()]).withCartTickets([cartItemEntityBuilder().withId('1').withQuantity(1).build(),]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                quantity: 1,
                total: '€10.00'
            })])
        })

        it('one item 2 quantity price 10', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build()])
                .withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(2).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                quantity: 2,
                total: '€20.00'
            })])
        })

        it('one item 1 quantity price 20', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(2000).build()])
                .withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(1).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                quantity: 1,
                total: '€20.00'
            })])
        })

        it('many items', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build(),
                    ticketEntityBuilder().withId('2').withPrice(550).build()
                ])
                .withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(1).build(),
                    cartItemEntityBuilder().withId('2').withQuantity(2).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                quantity: 1,
                total: '€10.00'
            }), expect.objectContaining({
                id: '2',
                quantity: 2,
                total: '€11.00'
            })])
        })

        it('contain ticket name', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withName('Regular').build(),
                    ticketEntityBuilder().withId('2').withName('Premium').build()
                ])
                .withCartTickets([
                    cartItemEntityBuilder().withId('1').build(),
                    cartItemEntityBuilder().withId('2').build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                name: 'Regular'
            }), expect.objectContaining({
                id: '2',
                name: 'Premium'
            })])
        })

        it('contain unit price', () => {
            const store = createTestStore({}, stateBuilder()
                .withTickets([
                    ticketEntityBuilder().withId('1').withPrice(1000).build(),
                    ticketEntityBuilder().withId('2').withPrice(550).build()
                ])
                .withCartTickets([
                    cartItemEntityBuilder().withId('1').withQuantity(2).build(),
                    cartItemEntityBuilder().withId('2').withQuantity(1).build(),
                ]).build());

            const viewmodel = selectCartViewmodelTest()(store.getState());

            expect(viewmodel.lines).toEqual([expect.objectContaining({
                id: '1',
                unitPrice: '€10.00',
            }), expect.objectContaining({
                id: '2',
                unitPrice: '€5.50',
            })])
        })
    })

    describe('actions', () => {
        it('call add to cart usecase when add is called', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest({ dispatch: store.dispatch })(store.getState());

            viewmodel.add('1');

            expect(store.getDispatchedAction(addToCart)).toEqual('1');
        })

        it('call remove from cart usecase when remove is called', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest({ dispatch: store.dispatch })(store.getState());

            viewmodel.remove('1');

            expect(store.getDispatchedAction(removeFromCart)).toEqual('1');
        })

        it('call book cart usecase when book is called', () => {
            const store = createTestStore();

            const viewmodel = selectCartViewmodelTest({ dispatch: store.dispatch })(store.getState());

            viewmodel.book();

            expect(store.getDispatchedUseCaseArgs(bookCart)).toEqual(EMPTY_ARGS);
        })

        it('call on book success when book is successfull', async () => {
            const bookingGateway = new FakeBookingGateway();
            bookingGateway.nextId = '1234';

            const store = createTestStore({ bookingGateway });
            const onBookSuccess = vitest.fn();

            const viewmodel = selectCartViewmodelTest({ dispatch: store.dispatch, onBookSuccess })(store.getState());

            await viewmodel.book();

            expect(onBookSuccess).toHaveBeenCalledWith('1234');
        })
    })
})
