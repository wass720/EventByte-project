CREATE TABLE events (
                        id          VARCHAR(255) PRIMARY KEY,
                        title       VARCHAR(255) NOT NULL,
                        description TEXT,
                        cover       VARCHAR(1024),
                        venue_name  VARCHAR(255),
                        venue_address VARCHAR(255),
                        latitude    DOUBLE PRECISION,
                        longitude   DOUBLE PRECISION
);