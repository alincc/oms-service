--- checkout
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(100, 1, 1, 'NEW', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(101, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);


-- checkout + transaction
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(110, 11, 1, 'NEW', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(110, 110, 1101, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(111, 110, 1102, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);



INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code)
  VALUES(150, 1, 3, 'CONFIRMED', '6000', 'XAF');



INSERT INTO T_ORDER(id, site_id, customer_id, language_code, status, total_amount, currency_code, first_name, last_name, email, device_uid)
  VALUES(200, 1, 3, 'fr', 'CONFIRMED', '6000', 'XAF', 'Ray', 'Sponsible', 'ray@gmail.com', '1234-1234');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price, description)
  VALUES(200, 200, 2001, 5678, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000, 'hello');


-- Expired order
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, first_name, last_name, email)
  VALUES(900, 1, 3, 'NEW', '6000', 'XAF', 'Ray', 'Sponsible', 'ray@gmail.com');

-- Cancelled order
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, first_name, last_name, email)
  VALUES(901, 1, 3, 'NEW', '6000', 'XAF', 'Ray', 'Sponsible', 'ray@gmail.com');

-- Cancel
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, first_name, last_name, email)
  VALUES(300, 1, 3, 'CONFIRMED', '6000', 'XAF', 'Ray', 'Sponsible', 'ray@gmail.com');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(301, 300, 2001, 5678, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);
INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, booking_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(302, 300, 2002, 5679, 'CAR', '100,1902459600000,1902466800000,1000,4000,XAF,1,2370002,2370001,I,15138644923060', 1, 4000, 4000);

INSERT INTO T_TICKET(id, order_line_fk, merchant_id, product_id, origin_id, destination_id, sequence_number, first_name, last_name, print_datetime, expiry_datetime, departure_datetime, status)
  VALUES(301, 301, 2001, 1001, 2370001, 2370002, 0, 'Ray', 'Sponsible', '2030-01-10', '2030-01-31', '2030-01-31', 'NEW');

INSERT INTO T_TICKET(id, order_line_fk, merchant_id, product_id, origin_id, destination_id, sequence_number, first_name, last_name, print_datetime, expiry_datetime, departure_datetime, status)
  VALUES(302, 302, 2002, 1001, 2370001, 2370002, 0, 'Ray', 'Sponsible', '2030-01-10', '2030-01-31', '2030-01-31', 'NEW');
