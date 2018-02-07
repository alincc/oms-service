--- checkout
INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(100, 1, 1, '2030-10-11', 'NEW', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(101, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);


-- checkout + transaction
INSERT INTO T_ACCOUNT(site_id, reference_id, type, balance, currency_code) VALUES(11, 1102, 'MERCHANT', 10000, 'XAF');

INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(110, 11, 1, '2030-10-11', 'NEW', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(110, 110, 1101, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(111, 110, 1102, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);



INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id)
  VALUES(150, 1, 3, '2030-10-11', 'CONFIRMED', '6000', 'XAF', 'ONLINE', 123);



INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id, first_name, last_name, email, device_uid)
  VALUES(200, 1, 3, '2030-10-11', 'CONFIRMED', '6000', 'XAF', 'ONLINE', 123, 'Ray', 'Sponsible', 'ray@gmail.com', '1234-1234');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price, description)
  VALUES(200, 200, 2001, 5678, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000, 'hello');


-- Expired order
INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id, first_name, last_name, email)
  VALUES(900, 1, 3, '2010-10-11', 'NEW', '6000', 'XAF', 'ONLINE', 123, 'Ray', 'Sponsible', 'ray@gmail.com');

-- Cancelled order
INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id, first_name, last_name, email)
  VALUES(901, 1, 3, '2030-10-11', 'CANCELLED', '6000', 'XAF', 'ONLINE', 123, 'Ray', 'Sponsible', 'ray@gmail.com');

-- Email
INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id, first_name, last_name, email)
  VALUES(300, 1, 3, '2030-10-11', 'CONFIRMED', '6000', 'XAF', 'ONLINE', 123, 'Ray', 'Sponsible', 'ray@gmail.com');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(301, 300, 2001, 5678, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);
INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(302, 300, 2002, 5678, 'CAR', '100,1902459600000,1902466800000,1000,4000,XAF,1,2370002,2370001,I,15138644923060', 1, 4000, 4000);
