package com.example.sqsmicro.records;

import java.util.Map;

public record MessageDto(
        Map<String, String> metadata, // Timestamp, TraceId, Sender, etc.
        String encryptedPayload,      // El contenido cifrado en Base64
        String keyId   // Opcional: Para saber qué clave se usó
) {}
