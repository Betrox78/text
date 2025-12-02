-- 1743509488 DOWN add-payment-complement-cols-to-invoice
ALTER TABLE `invoice`
  DROP COLUMN `available_amount_for_complement`,
  DROP COLUMN `cfdi_payment_method`,
  DROP COLUMN `cfdi_payment_form`;