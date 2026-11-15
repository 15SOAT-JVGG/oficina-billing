package br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Estimate")
@Table(name = "estimate")
@Getter
@Setter
public class EstimateJpaEntity {

    @Id
    @Column(name = "estimate_id", nullable = false)
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "opening_event_id", nullable = false, unique = true)
    private UUID openingEventId;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<EstimateItemJpaEntity> items = new ArrayList<>();
}
