import { v7 } from 'uuid';
import { OutboxRepository } from '../../../../services/outbox-repository';

type EventWebhook = {
    id: string;
    title: string;
    description: string;
    cover: string;
    startAt: string;
    venue: {
        name: string;
        address: string;
        latitude: number;
        longitude: number;
    },
    tickets: {
        id: string;
        name: string;
        quantity: number;
        price: number;
    }[]
}


const resultToWebHook = (result: any): EventWebhook => ({
    id: result.uuid,
    title: result.title,
    description: result.description,
    cover: result.cover.url,
    startAt: new Date(result.startAt).toISOString(),
    venue: {
        name: result.venue.name,
        address: result.venue.adress,
        latitude: result.venue.coordinate.lat,
        longitude: result.venue.coordinate.lng
    },
    tickets: result.tickets.map((ticket: any) => {
        return {
            id: ticket.uuid,
            name: ticket.name,
            quantity: ticket.quantity,
            price: ticket.price
        }
    })
})

const getOutboxRepository = () => strapi.get<OutboxRepository>(OutboxRepository.toString())

export default {
    async beforeCreate(event: any) {
        const { data } = event.params;

        if (!data.uuid) {
            data.uuid = `evt_${v7()}`;
        }
    },
    async afterCreate(event: any) {
        const fullResult = await strapi.db.query('api::event.event').findOne({
            where: { id: event.result.id },
            populate: { cover: true, venue: true, tickets: true },
        });
        const data = resultToWebHook(fullResult);
        await getOutboxRepository().create('event.created', data)
    },
    async afterUpdate(event: any) {
        const data = resultToWebHook(event.result);
        await getOutboxRepository().create('event.updated', data)
    },
    async afterDelete(event: any) {
        const id = event.result.uuid
        await getOutboxRepository().create('event.deleted', { id })
    },
};
