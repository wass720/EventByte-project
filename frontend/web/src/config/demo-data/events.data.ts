import { Event } from "@/core/events/ports/events.gateway";

export const demoEvents: Event[] = [
    {
        id: "1",
        title: "Doctor Love - Halloween (The Monster Ball)",
        cover:
            "https://res.cloudinary.com/shotgun/image/upload/c_limit,w_828/fl_lossy/f_auto/q_auto/production/artworks/Flyer_HD_copy_y7c3xe.jpg",
        likedCount: 129,
        venue: {
            name: "Bourse",
            adresse: "Place de la Bourse, 75007 Paris",
            coordinates: { latitude: 48.8585, longitude: 2.3475 },
        },
        isSoldOut: false,
        startAt: new Date("2025-12-23T04:00:00Z").toISOString(),
        lowestPrice: 2399,
        description: "Description",
    },
    {
        id: "2",
        title: "The Shazzams, Tribute To Sunshine",
        cover:
            "https://res.cloudinary.com/shotgun/image/upload/c_limit,w_828/fl_lossy/f_auto/q_auto/production/artworks/FORMAT_FB_ixzk8u.jpg",
        likedCount: 74,
        venue: {
            name: "Bourse",
            adresse: "Place de la Bourse, 75007 Paris",
            coordinates: { latitude: 48.8585, longitude: 2.3475 },
        },
        isSoldOut: false,
        startAt: new Date("2026-01-01T18:30:00Z").toISOString(),
        lowestPrice: 1820,
        description: "Description",
    },
    {
        id: "3",
        title: "Teletech Paris",
        cover:
            "https://res.cloudinary.com/shotgun/image/upload/c_limit,w_828/fl_lossy/f_auto/q_auto/c_limit,f_auto,fl_lossy,q_auto,w_1920/v1753689660/production/artworks/Teletech-Paris-22.11_ezxhwv",
        likedCount: 234,
        venue: {
            name: "Parc Floral de Paris",
            adresse: "Route de la Pyramide, 75012 Paris, France",
            coordinates: { latitude: 43.2965, longitude: 5.3698 },
        },
        isSoldOut: false,
        startAt: new Date("2026-11-22T20:30:00Z").toISOString(),
        lowestPrice: 4900,
        description: "Description",
    },
    {
        id: "4",
        title: "Babylon Halloween - Bourse De Paris With Agents Of Time",
        cover:
            "https://res.cloudinary.com/shotgun/image/upload/c_limit,ar_16:9,w_1920/fl_lossy/f_auto/q_auto/production/artworks/SG_31OCT_BABYLON_HALLOWEEN_2_hq7oa2.png",
        likedCount: 10,
        venue: {
            name: "Bourse",
            adresse: "Place de la Bourse, 75007 Paris",
            coordinates: { latitude: 48.8566, longitude: 2.3522 },
        },
        isSoldOut: false,
        startAt: new Date("2026-01-01").toISOString(),
        lowestPrice: 1000,
        description: "Description",
    },
    {
        id: "5",
        title: "Jazz Night Under the Stars",
        cover:
            "https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f?w=800&auto=format&fit=crop",
        likedCount: 45,
        venue: {
            name: "Riverside Theater",
            adresse: "175 N Harbor Dr, Chicago, IL 60601",
            coordinates: { latitude: 41.8781, longitude: -87.6298 },
        },
        isSoldOut: false,
        startAt: new Date("2024-06-10T19:00:00Z").toISOString(),
        lowestPrice: 3500,
        description: "An evening of smooth jazz under the stars",
    },
];

export const demoLikedEvents = ["1"];
