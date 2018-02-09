-- confirmed
INSERT INTO T_ACCOUNT(site_id, reference_id, type, balance, currency_code) VALUES(1, 1002, 'MERCHANT', 10000, 'XAF');

INSERT INTO T_ORDER(id, site_id, customer_id, email, expiry_datetime, status, total_amount, currency_code)
  VALUES(100, 1, 1, 'ray.sponsible@gmail.com', '2030-10-11', 'CONFIRMED', '12000', 'XAF');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1001, 1000, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,O,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(101, 100, 1002, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);
