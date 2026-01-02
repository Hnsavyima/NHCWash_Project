package com.nhcwash.backend.models.enumerations;

public enum PaymentMethod {
    ONLINE_CARD, // Stripe / PayPal
    CASH, // Espèces (au comptoir)
    POS_TERMINAL // Bancontact / Carte sur place
}