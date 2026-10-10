-- Enforce one XP award per source activity, regardless of the recipient.
-- Legacy/manual events without a source reference remain valid and unaffected.
CREATE UNIQUE INDEX uq_xp_events_source_activity
    ON xp_events (source, ref_type, ref_id)
    WHERE ref_type IS NOT NULL AND ref_id IS NOT NULL;
