package com.store.store.service;

import com.store.store.dto.order.OrderFilterDto;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service pour la gestion des commandes
 *
 * @author Kardigué
 * @version 4.0 - Production Ready avec pagination
 * @since 2025-11-20
 */
public interface IOrderService {

    /**
     * Crée une nouvelle commande
     * @param orderRequest Données de la commande
     */
    void createOrder(OrderRequestDto orderRequest);

    /**
     * Récupère toutes les commandes du client authentifié
     * @return Liste des commandes du client
     */
    List<OrderResponseDto> getCustomerOrders();

    /**
     * Récupère une commande par son ID
     * @param orderId ID de la commande
     * @return Détails de la commande
     */
    OrderResponseDto getOrderById(Long orderId);

    /**
     * Récupère toutes les commandes en attente (statut CREATED)
     * @return Liste des commandes en attente
     */
    List<OrderResponseDto> getAllPendingOrders();

    /**
     * Récupère toutes les commandes du système
     * @return Liste complète des commandes
     */
    List<OrderResponseDto> getAllOrders();

    /**
     * Recherche des commandes avec filtres et pagination (RECOMMANDÉ pour admin)
     * @param filters Filtres de recherche (statut, query, pagination)
     * @return Page de commandes filtrées
     */
    Page<OrderResponseDto> findOrdersWithFiltersPaginated(OrderFilterDto filters);

    Page<OrderResponseDto> findCustomerOrdersPaginated(Long customerId, OrderFilterDto filters);

    /**
     * Met à jour le statut d'une commande
     * @param orderId ID de la commande
     * @param orderStatus Nouveau statut (CREATED, CONFIRMED, CANCELLED, DELIVERED)
     */
    void updateOrderStatus(Long orderId, String orderStatus);
}