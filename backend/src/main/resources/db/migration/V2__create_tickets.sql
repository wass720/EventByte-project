CREATE TABLE tickets (
                         id       VARCHAR(255) PRIMARY KEY,
                         event_id VARCHAR(255) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                         name     VARCHAR(255) NOT NULL,
                         quantity INTEGER      NOT NULL,
                         price    INTEGER      NOT NULL
);