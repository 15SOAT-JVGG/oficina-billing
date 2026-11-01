-- Catálogo de serviços e peças com preço (issue #107). O schema oficina_billing é criado pelo
-- Flyway (create-schemas); as tabelas nascem aqui e o Hibernate apenas valida.
-- Peças não têm saldo de estoque: isso é do Execution.

CREATE TABLE service_item (
    service_item_id            uuid          NOT NULL,
    name                       varchar(100)  NOT NULL,
    description                varchar(500),
    base_price                 numeric(10,2) NOT NULL,
    estimated_duration_minutes integer,
    active                     boolean       NOT NULL,
    created_at                 timestamptz   NOT NULL,
    updated_at                 timestamptz   NOT NULL,
    created_by                 varchar(100)  NOT NULL,
    updated_by                 varchar(100)  NOT NULL,
    version                    bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_service_item PRIMARY KEY (service_item_id)
);

CREATE TABLE part (
    part_id     uuid          NOT NULL,
    name        varchar(100)  NOT NULL,
    description varchar(500),
    unit_price  numeric(10,2) NOT NULL,
    unit        varchar(10)   NOT NULL,
    part_type   varchar(10)   NOT NULL,
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(100)  NOT NULL,
    updated_by  varchar(100)  NOT NULL,
    version     bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_part PRIMARY KEY (part_id),
    CONSTRAINT ck_part_type CHECK (part_type IN ('PART', 'SUPPLY'))
);
