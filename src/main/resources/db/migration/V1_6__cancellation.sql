ALTER TABLE T_ORDER DROP COLUMN expiry_datetime;

ALTER TABLE T_TICKET ADD COLUMN cancellation_datetime DATETIME;

ALTER TABLE T_TICKET ADD COLUMN status VARCHAR(10);
UPDATE T_TICKET SET status='NEW';
ALTER TABLE T_TICKET MODIFY COLUMN status VARCHAR(10) NOT NULL;