CREATE TABLE T_FEES(
  id              INT           NOT NULL AUTO_INCREMENT,

  site_id         INT           NOT NULL,
  name            VARCHAR(100)  NOT NULL,
  percent         DECIMAL(3,2)  NOT NULL,
  amount          DECIMAL(10,2) NOT NULL,

  PRIMARY KEY (id)
) ENGINE = InnoDB;

ALTER TABLE T_ORDER_LINE ADD COLUMN fees_fk INT NULL REFERENCES T_FEES(id);
ALTER TABLE T_ORDER_LINE MODIFY merchant_id INT NULL;

ALTER TABLE T_ORDER ADD COLUMN sub_total_amount DECIMAL(10, 2);
ALTER TABLE T_ORDER ADD COLUMN total_fees DECIMAL(10, 2);

INSERT INTO T_FEES(id, site_id, name, amount, percent) VALUES(1, 1, 'BookingFees', 300, 0);
