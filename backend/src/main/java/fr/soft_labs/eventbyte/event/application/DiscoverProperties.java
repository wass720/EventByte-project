package fr.soft_labs.eventbyte.event.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "eventbyte.discover")
public record DiscoverProperties(@DefaultValue("50") double radiusKm) {
}