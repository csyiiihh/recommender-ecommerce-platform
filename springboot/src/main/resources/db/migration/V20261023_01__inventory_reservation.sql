-- Run manually against the intended database before enabling the feature.
-- No existing stock or orders are changed by this migration.
CREATE TABLE IF NOT EXISTS inventory_reservation (
    order_id BIGINT NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    state TINYINT NOT NULL COMMENT '1 held, 2 consumed, 3 released',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reservation_state (state, order_id),
    CONSTRAINT chk_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_reservation_state CHECK (state IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
