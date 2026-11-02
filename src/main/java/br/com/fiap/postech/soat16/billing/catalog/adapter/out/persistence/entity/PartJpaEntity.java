package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;
import br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence.AuditableJpaEntity;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Part")
@Table(name = "part")
@Getter
@Setter
public class PartJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "part_id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", nullable = false, length = 10)
    private PartType partType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
