--- findById
INSERT INTO T_ORDER(id, site_id, customer_id, status, total_amount, currency_code, email, first_name, last_name)
  VALUES(100, 1, 1, 'CONFIRMED', '12000', 'XAF', 'ray.sponsible@gmail.com', 'Ray', 'Sponsible');

INSERT INTO T_ORDER_LINE(id, order_fk, merchant_id, type, offer_token, quantity, unit_price, total_price)
  VALUES(100, 100, 1001, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,1,2370001,2370002,-,15138644923060', 1, 6000, 6000);


INSERT INTO T_TICKET(id, order_line_fk, merchant_id, product_id, origin_id, destination_id, sequence_number, first_name, last_name, print_datetime, expiry_datetime, departure_datetime, status)
  VALUES(100, 100, 1, 1001, 2370001, 2370002, 0, 'Ray', 'Sponsible', '2030-01-10', '2030-01-31', '2030-01-31', 'NEW');
