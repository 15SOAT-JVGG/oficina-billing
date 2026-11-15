-- Orçamento solicitado pela abertura da OS e journals transacionais da Saga (#109).

CREATE TABLE estimate (
    estimate_id       uuid          NOT NULL,
    work_order_id     uuid          NOT NULL,
    opening_event_id  uuid          NOT NULL,
    saga_id           uuid          NOT NULL,
    correlation_id    uuid          NOT NULL,
    total_amount      numeric(12,2) NOT NULL,
    created_at        timestamptz   NOT NULL,
    CONSTRAINT pk_estimate PRIMARY KEY (estimate_id),
    CONSTRAINT uk_estimate_opening_event UNIQUE (opening_event_id),
    CONSTRAINT ck_estimate_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX ix_estimate_work_order ON estimate (work_order_id);

CREATE TABLE estimate_item (
    estimate_item_id uuid          NOT NULL,
    estimate_id      uuid          NOT NULL,
    item_type        varchar(10)   NOT NULL,
    catalog_item_id  uuid          NOT NULL,
    name             varchar(100)  NOT NULL,
    quantity         integer       NOT NULL,
    unit_price       numeric(10,2) NOT NULL,
    CONSTRAINT pk_estimate_item PRIMARY KEY (estimate_item_id),
    CONSTRAINT fk_estimate_item_estimate FOREIGN KEY (estimate_id)
        REFERENCES estimate (estimate_id) ON DELETE CASCADE,
    CONSTRAINT ck_estimate_item_type CHECK (item_type IN ('SERVICE', 'PART')),
    CONSTRAINT ck_estimate_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_estimate_item_price CHECK (unit_price > 0)
);

CREATE INDEX ix_estimate_item_estimate ON estimate_item (estimate_id);

CREATE TABLE billing_event_inbox (
    event_id     uuid         NOT NULL,
    work_order_id uuid        NOT NULL,
    event_type   varchar(100) NOT NULL,
    received_at  timestamptz  NOT NULL,
    CONSTRAINT pk_billing_event_inbox PRIMARY KEY (event_id)
);

CREATE TABLE billing_event_outbox (
    sequence_id  bigserial    NOT NULL,
    event_id     uuid         NOT NULL,
    work_order_id uuid        NOT NULL,
    event_type   varchar(100) NOT NULL,
    payload      jsonb        NOT NULL,
    traceparent  varchar(255),
    created_at   timestamptz  NOT NULL,
    published_at timestamptz,
    CONSTRAINT pk_billing_event_outbox PRIMARY KEY (sequence_id),
    CONSTRAINT uk_billing_event_outbox_event UNIQUE (event_id)
);

CREATE INDEX ix_billing_event_outbox_pending
    ON billing_event_outbox (sequence_id) WHERE published_at IS NULL;
