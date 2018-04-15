ALTER TABLE T_ORDER_LINE DROP COLUMN transaction_fk;

ALTER TABLE T_CANCELLATION DROP COLUMN transaction_fk;

ALTER TABLE T_ORDER DROP COLUMN payment_id;
ALTER TABLE T_ORDER DROP COLUMN payment_method;

DROP TABLE T_TRANSACTION;
CREATE TABLE T_TRANSACTION(
  id               INT           NOT NULL AUTO_INCREMENT,
  order_fk         INT           NOT NULL REFERENCES T_ORDER(id),
  cancellation_fk  INT               NULL REFERENCES T_ORDER(id),

  type             VARCHAR(10)   NOT NULL,
  gateway_tid      VARCHAR(36)   NOT NULL,

  amount           DECIMAL(10,2),
  currency_code    VARCHAR(3),
  transaction_datetime DATETIME  NOT NULL,
  payment_method   VARCHAR(20),

  PRIMARY KEY (id)
) ENGINE = InnoDB;

DROP TABLE T_ACCOUNT;

