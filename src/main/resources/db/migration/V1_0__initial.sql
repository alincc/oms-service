CREATE TABLE T_ORDER(
  id                       INT     NOT NULL AUTO_INCREMENT,
  merchant_id              INT     NOT NULL,
  customer_id              INT     NOT NULL,
  payment_id               INT,

  order_datetime           DATETIME       NOT NULL DEFAULT NOW(),
  expiry_datetime          DATETIME       NOT NULL,
  status                   VARCHAR(10)    NOT NULL,
  total_amount             DECIMAL(10, 2) NOT NULL,
  currency_code            VARCHAR(3)     NOT NULL,

  mobile_payment_area_code    VARCHAR(3),
  mobile_payment_country_code VARCHAR(3),
  mobile_payment_number       VARCHAR(20),
  mobile_payment_provider     VARCHAR(10),

  PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE T_ORDER_LINE(
  id                 INT     NOT NULL AUTO_INCREMENT,
  order_fk           INT     NOT NULL REFERENCES T_ORDER(id),
  booking_id         INT,

  offer_type         VARCHAR(10)    NOT NULL,
  offer_token        VARCHAR(255)   NOT NULL,
  description        VARCHAR(100),
  amount             DECIMAL(10, 2) NOT NULL,

  PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE T_TRAVELLER(
  id                 INT     NOT NULL AUTO_INCREMENT,
  order_fk           INT     NOT NULL REFERENCES T_ORDER(id),

  first_name         VARCHAR(50),
  last_name          VARCHAR(50),
  sex                CHAR(1),

  PRIMARY KEY (id)
) ENGINE = InnoDB;

