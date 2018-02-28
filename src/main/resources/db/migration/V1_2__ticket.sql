CREATE TABLE T_TICKET(
  id                INT           NOT NULL AUTO_INCREMENT,
  order_fk          INT           NOT NULL REFERENCES T_ORDER(id),
  booking_id        INT           NOT NULL,
  merchant_id       INT           NOT NULL,
  offer_token       VARCHAR(255)  NOT NULL,
  hash              VARCHAR(32)   NOT NULL,
  print_datetime    DATETIME,
  expiry_datetime   DATETIME,

  PRIMARY KEY (id)
) ENGINE = InnoDB;
