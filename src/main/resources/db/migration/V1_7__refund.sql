CREATE TABLE T_CANCELLATION(
  id                 INT            NOT NULL AUTO_INCREMENT,
  order_fk           INT            NOT NULL REFERENCES T_ORDER(id),

  booking_id         INT            NOT NULL,
  cancellation_datetime DATETIME,
  refund_amount      DECIMAL(10, 2) NOT NULL,
  currency_code      VARCHAR(3)     NOT NULL,

  UNIQUE(booking_id),
  PRIMARY KEY (id)
) ENGINE = InnoDB;
