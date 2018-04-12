ALTER TABLE T_TRANSACTION MODIFY COLUMN type VARCHAR(15) NOT NULL;

ALTER TABLE T_TRANSACTION ADD COLUMN correlation_id VARCHAR(36) NOT NULL;
CREATE INDEX T_TRANSACTION__correlation_id ON T_TRANSACTION (correlation_id);



ALTER TABLE T_ORDER_LINE ADD COLUMN transaction_fk INT NULL REFERENCES T_TRANSACTION(id);


CREATE TABLE T_CANCELLATION(
  id                 INT            NOT NULL AUTO_INCREMENT,
  order_fk           INT            NOT NULL REFERENCES T_ORDER(id),
  transaction_fk     INT            NULL REFERENCES T_TRANSACTION(id),

  booking_id         INT            NOT NULL,
  cancellation_datetime DATETIME,

  UNIQUE(booking_id),
  PRIMARY KEY (id)
) ENGINE = InnoDB;
