-- confirmed
INSERT INTO T_ORDER(id, site_id, customer_id, email, expiry_datetime, status, total_amount, currency_code)
  VALUES(100, 1, 1, 'ray.sponsible@gmail.com', '2030-10-11', 'CONFIRMED', '12000', 'XAF');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1000, 1000, 'BUS', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,O,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(101, 100, 1001, 1001, 'BUS', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);

-- new
INSERT INTO T_ORDER(id, site_id, customer_id, email, expiry_datetime, status, total_amount, currency_code)
  VALUES(101, 1, 1, 'ray.sponsible@gmail.com', '2030-10-11', 'NEW', '12000', 'XAF');

-- cancelled
INSERT INTO T_ORDER(id, site_id, customer_id, email, expiry_datetime, status, total_amount, currency_code)
  VALUES(103, 1, 1, 'ray.sponsible@gmail.com', '2030-10-11', 'CANCELLED', '12000', 'XAF');
