// src/main/java/org/example/springboot/dto/email/EmailResponseDTO.java
package org.example.springboot.dto.email;

import lombok.Data;

@Data
public class EmailResponseDTO {
    private boolean success;
    private String message;
    private String email;
    private String type;
}