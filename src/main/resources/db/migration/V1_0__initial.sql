CREATE TABLE T_ORDER(
  id                       INT     NOT NULL AUTO_INCREMENT,
  payment_id               INT,

  order_datetime           DATETIME       NOT NULL DEFAULT NOW(),
  expiry_datetime          DATETIME       NOT NULL,
  status                   VARCHAR(10)    NOT NULL,
  total_amount             DECIMAL(10, 2) NOT NULL,
  currency_code            VARCHAR(3)     NOT NULL,
  payment_method           VARCHAR(20),

  customer_id              INT,
  first_name               VARCHAR(50),
  last_name                VARCHAR(50),
  email                    VARCHAR(100),

  INDEX T_ORDER_expiry_status(expiry_datetime, status),
  PRIMARY KEY (id)
) ENGINE = InnoDB;



CREATE TABLE T_ORDER_LINE(
  id                 INT     NOT NULL AUTO_INCREMENT,
  order_fk           INT     NOT NULL REFERENCES T_ORDER(id),
  booking_id         INT,

  offer_type         VARCHAR(10)    NOT NULL,
  quantity           INT,
  unit_price         DECIMAL(10, 2) NOT NULL,
  total_price        DECIMAL(10, 2) NOT NULL,
  offer_token        VARCHAR(255),
  description        VARCHAR(100),

  PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE T_TRAVELLER(
  id                 INT     NOT NULL AUTO_INCREMENT,
  order_fk           INT     NOT NULL REFERENCES T_ORDER(id),

  first_name         VARCHAR(50),
  last_name          VARCHAR(50),
  sex                CHAR(1),
  email              VARCHAR(255),

  PRIMARY KEY (id)
) ENGINE = InnoDB;

