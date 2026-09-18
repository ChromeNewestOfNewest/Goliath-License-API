-- V2: Replace the expiration CHECK constraint to match JPA enum names (EnumType.STRING)
BEGIN;
ALTER TABLE licenses DROP CONSTRAINT IF EXISTS licenses_expiration_check;
ALTER TABLE licenses ADD CONSTRAINT licenses_expiration_check CHECK (
    expiration IN (
        'TEN_MINUTES',
        'ONE_HOUR',
        'ONE_DAY',
        'FIVE_DAYS',
        'TEN_DAYS',
        'ONE_MONTH',
        'ONE_YEAR',
        'NEVER'
    )
);
COMMIT;
