DROP TABLE T_TICKET;

CREATE TABLE T_TICKET(
  id                INT           NOT NULL AUTO_INCREMENT,
  order_line_fk     INT           NOT NULL REFERENCES T_ORDER_LINE(id),
  sequence_number   INT,

  first_name        VARCHAR(50),
  last_name         VARCHAR(50),
  sex               CHAR(1),

  print_datetime    DATETIME,
  expiry_datetime   DATETIME,
  hash              VARCHAR(32)   NOT NULL,

  PRIMARY KEY (id)
) ENGINE = InnoDB;
