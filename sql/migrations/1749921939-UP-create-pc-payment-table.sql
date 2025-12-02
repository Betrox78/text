-- 1749921939 UP create-pc-payment-table
CREATE TABLE payment_complement_payment (
    payment_complement_id INT NOT NULL,
    payment_id INT NOT NULL,
    PRIMARY KEY (payment_complement_id , payment_id),
    CONSTRAINT fk_pcp_pc FOREIGN KEY (payment_complement_id)
        REFERENCES payment_complement (id),
    CONSTRAINT fk_pcp_p FOREIGN KEY (payment_id)
        REFERENCES payment (id)
);