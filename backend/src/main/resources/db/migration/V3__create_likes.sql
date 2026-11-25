CREATE TABLE likes (
                       user_id  VARCHAR(255) NOT NULL,
                       event_id VARCHAR(255) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                       liked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                       -- Un utilisateur ne peut liker un événement qu'une fois (US-8).
                       -- Sert aussi d'index pour lister les likes d'un utilisateur (US-10).
                       PRIMARY KEY (user_id, event_id)
);

-- Pour calculer likedCount d'un événement.
CREATE INDEX idx_likes_event_id ON likes (event_id);
