package com.nhcwash.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// 1. Charger le fichier .env
		// ignoreIfMissing() évite que l'app crash si le fichier n'est pas là (ex: en
		// prod via Docker)
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// 2. Copier chaque entrée du .env vers les propriétés système de Java
		// Cela permet à Spring de résoudre les ${VARIABLE} dans application.properties
		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		// 3. Lancer l'application
		SpringApplication.run(BackendApplication.class, args);
	}
}