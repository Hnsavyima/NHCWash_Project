package com.nhcwash.backend.models.enumerations;

public enum OrderStatus {
    PENDING, // Commande créée, en attente de dépôt/collecte
    PAID, // Paiement enregistré (facture émise)
    RECEIVED, // Linge reçu à l'atelier
    PROCESSING, // Lavage/Repassage en cours
    READY, // Linge propre, prêt pour retrait/livraison
    DELIVERED, // Remis au client
    CANCELLED // Commande annulée
}
