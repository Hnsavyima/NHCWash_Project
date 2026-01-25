package com.nhcwash.backend.models.enumerations;

public enum SlotType {
    COLLECTION, // L'employé vient chercher le linge
    DELIVERY, // L'employé ramène le linge
    DROP_OFF // Le client dépose lui-même à l'atelier
    // il faudra peut-être ajouter un type "PICK_UP" si on veut différencier le "COLLECTION" où l'employé vient chercher le linge à domicile et le "DROP_OFF" où le client dépose lui-même le linge à l'atelier
}
