ALTER TABLE post_tag ADD COLUMN post_count INTEGER NOT NULL DEFAULT 0;

UPDATE post_tag t
SET post_count = c.cnt
FROM (SELECT l.tag_id, count(*) AS cnt
      FROM post_tag_link l
      JOIN post p ON p.id = l.post_id
      WHERE p.status <> 'DELETED'
      GROUP BY l.tag_id) c
WHERE c.tag_id = t.id;

ALTER TABLE post_tag ADD CONSTRAINT ck_post_tag_post_count CHECK (post_count >= 0);
