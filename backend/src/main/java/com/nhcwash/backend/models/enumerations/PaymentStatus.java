package com.nhcwash.backend.models.enumerations;

public enum PaymentStatus {
    UNPAID, // En attente de règlement
    PAID, // Règlement effectué
    REFUNDED // Remboursé en cas de litige
}