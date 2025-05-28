package com.program.bookie.models;

public enum ResponseType {
    SUCCESS,    // Operacja zakończona sukcesem
    ERROR,      // Wystąpił błąd
    INFO,       // Informacja (np. ostrzeżenie)
    DENIED      // Brak uprawnień
}