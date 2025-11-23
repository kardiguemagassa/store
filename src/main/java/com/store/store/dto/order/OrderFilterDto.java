package com.store.store.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filtres pour la recherche de commandes")
public class OrderFilterDto {

    @Schema(description = "Numéro de page (0-based)", example = "0")
    private Integer page;

    @Schema(description = "Taille de la page", example = "10")
    private Integer size;

    @Schema(description = "Statut de la commande", example = "CREATED",
            allowableValues = {"CREATED", "CONFIRMED", "CANCELLED", "DELIVERED"})
    private String status;

    @Schema(description = "Recherche par email client ou ID commande", example = "client@example.com")
    private String query;

    @Schema(description = "Tri par champ", example = "createdAt")
    private String sortBy;

    @Schema(description = "Direction du tri", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortDirection;

    // NOUVEAU: Pour la pagination client
    @Schema(description = "ID du client (filtrer par client spécifique)", example = "123")
    private Long customerId;
}