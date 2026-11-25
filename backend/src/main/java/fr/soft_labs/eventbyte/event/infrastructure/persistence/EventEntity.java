package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String cover;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "venue_address")
    private String venueAddress;

    private Double latitude;

    private Double longitude;

    protected EventEntity() {
    }

    public EventEntity(String id, String title, String description, String cover,
                       String venueName, String venueAddress, Double latitude, Double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.cover = cover;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCover() { return cover; }
    public String getVenueName() { return venueName; }
    public String getVenueAddress() { return venueAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}