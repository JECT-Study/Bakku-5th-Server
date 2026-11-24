CREATE INDEX idx_post_home_hub ON post (hub, id DESC) WHERE status = 'AVAILABLE';
