INSERT INTO T_ORDER(id, merchant_id, customer_id, expiry_datetime, status, total_amount, currency_code, mobile_payment_country_code, mobile_payment_area_code, mobile_payment_number, mobile_payment_provider)
  VALUES(100, 1, 1, '2030-10-11', 'PENDING', '12000', 'XAF', '237', null, '99595678', 'MTN');

INSERT INTO T_ORDER_LINE(id, order_fk, offer_type, offer_token, amount) VALUES(100, 100, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2370001,2370002,15138644923060', '6000');
INSERT INTO T_ORDER_LINE(id, order_fk, offer_type, offer_token, amount) VALUES(101, 100, 'CAR', '100,1902459600000,1902466800000,1000,6000,XAF,2370001,2370002,15138644923060', '6000');

INSERT INTO T_TRAVELLER(id, order_fk, first_name, last_name, sex) VALUES(100, 100, 'Ray', 'Sponsible', 'M')
