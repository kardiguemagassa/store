package com.store.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"customer", "orderItems"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_customer_id", columnList = "customer_id"),
                @Index(name = "idx_order_status", columnList = "order_status"),
                @Index(name = "idx_payment_status", columnList = "payment_status")
        }
)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long orderId;

    @NotNull(message = "Le client est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @NotNull(message = "Le prix total est obligatoire")
    @Positive(message = "Le prix total doit être positif")
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @NotBlank(message = "L'ID du paiement Stripe est obligatoire")
    @Size(max = 250, message = "L'ID du paiement ne peut pas dépasser 250 caractères")
    @Column(name = "payment_intent_id", nullable = false, length = 250)
    private String paymentIntentId;

    @NotBlank(message = "Le statut du paiement est obligatoire")
    @Size(max = 50)
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus;

    @NotBlank(message = "Le statut de la commande est obligatoire")
    @Size(max = 50)
    @Column(name = "order_status", nullable = false, length = 50)
    private String orderStatus;

    // MÉTHODES HELPER

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    public BigDecimal calculateTotal() {
        return orderItems.stream()
                .map(item -> item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}