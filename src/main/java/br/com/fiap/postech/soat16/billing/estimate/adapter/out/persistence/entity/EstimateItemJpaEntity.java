package br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItemType;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "EstimateItem")
@Table(name = "estimate_item")
@Getter
@Setter
public class EstimateItemJpaEntity {

    @Id
    @Column(name = "estimate_item_id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estimate_id", nullable = false)
    private EstimateJpaEntity estimate;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private EstimateItemType itemType;

    @Column(name = "catalog_item_id", nullable = false)
    private UUID catalogItemId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
