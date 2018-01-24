INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code)
  VALUES(100, 1, 1, '2030-10-11', 'PENDING', '12000', 'XAF');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 100, 1000, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(101, 100, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,15138644923060', 2, 6000, 12000);



INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code)
  VALUES(103, 1, 1, '2010-10-11', 'PENDING', '12000', 'XAF');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(1031, 103, 103, 1031, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(1032, 103, 103,1032, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,15138644923060', 1, 6000, 6000);


INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code)
  VALUES(104, 1, 1, '2010-10-11', 'NEW', '12000', 'XAF');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(1041, 104, 104, 1041, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,15138644923060', 1, 6000, 6000);


INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, payment_method, payment_id, first_name, last_name, email)
  VALUES(200, 1, 3, '2030-10-11', 'CONFIRMED', '6000', 'XAF', 'ONLINE', 123, 'Ray', 'Sponsible', 'ray@gmail.com');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price, description)
  VALUES(200, 200, 200, 5678, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,15138644923060', 1, 6000, 6000, 'hello');
