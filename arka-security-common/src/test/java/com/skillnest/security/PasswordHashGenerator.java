package com.skillnest.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utilitario para generar hashes BCrypt correctos para las contraseñas
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

        // Generar hash para admin123
        String password = "admin123";
        String hash = passwordEncoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println();

        // Verificar que el hash es correcto
        boolean matches = passwordEncoder.matches(password, hash);
        System.out.println("Hash matches password: " + matches);

        // Generar hashes para otros usuarios de prueba
        System.out.println("\n--- Otros usuarios ---");
        String[] passwords = {"admin123", "admin123", "admin123", "admin123"};
        String[] users = {"admin", "gestor", "operador", "usuario"};

        for (int i = 0; i < users.length; i++) {
            String userHash = passwordEncoder.encode(passwords[i]);
            System.out.println(users[i] + " -> " + userHash);
        }
    }
}
