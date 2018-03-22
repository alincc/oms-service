--- findById
INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(100, 1, 1, '2030-10-11', 'CONFIRMED', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);


INSERT INTO T_TICKET(id, order_line_fk, merchant_id, product_id, origin_id, destination_id, sequence_number, first_name, last_name, print_datetime, expiry_datetime, departure_datetime)
  VALUES(100, 100, 1, 1001, 2370001, 2370002, 0, 'Ray', 'Sponsible', '2030-01-10', '2030-01-31', '2030-01-31');


-- findById - Not Confirmed
INSERT INTO T_ACCOUNT(site_id, reference_id, type, balance, currency_code) VALUES(11, 1102, 'MERCHANT', 10000, 'XAF');

INSERT INTO T_ORDER(id, site_id, customer_id, expiry_datetime, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(110, 11, 1, '2030-10-11', 'CANCELLED', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(110, 110, 1101, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, offer_type, offer_token, quantity, unit_price, total_price)
  VALUES(111, 110, 1102, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2,2370001,2370002,I,15138644923060', 2, 6000, 12000);

INSERT INTO T_TICKET(id, order_line_fk, merchant_id, product_id, origin_id, destination_id, sequence_number, first_name, last_name, print_datetime, expiry_datetime, departure_datetime)
  VALUES(111, 111, 1, 1001, 2370001, 2370002, 0, 'Ray', 'Sponsible', '2030-01-10', '2030-01-31', '2030-01-31');
