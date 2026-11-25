package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private String id;

    @Column(name = "event_id")
    private String eventId;

    private String name;

    private Integer quantity;

    private Integer price;

    protected TicketEntity() {
    }

    public TicketEntity(String id, String eventId, String name, Integer quantity, Integer price) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public Integer getQuantity() { return quantity; }
    public Integer getPrice() { return price; }
}