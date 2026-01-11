package org.example.rest;

import com.sun.net.httpserver.HttpServer;
import org.example.server.bd.BdManager;
import org.example.rest.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;


public class RestServer {

    private static final int PORT = 9090;
    private HttpServer serveur;
    private BdManager bdManager;


    public RestServer() {
        bdManager = new BdManager();
    }


    public void start() {
        try {
            bdManager.connecter();
            serveur = HttpServer.create(new InetSocketAddress(PORT), 0);

            serveur.createContext("/api/specialties", new SpecialtiesHandler(bdManager));
            serveur.createContext("/api/doctors", new DoctorsHandler(bdManager));
            serveur.createContext("/api/patients", new PatientsHandler(bdManager));
            serveur.createContext("/api/consultations", new ConsultationsHandler(bdManager));

            serveur.start();
            System.out.println("✓ REST server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("✗ I/O error while starting RestServer: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("✗ Database error while starting RestServer: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void stop() {
        if (serveur != null) {
            serveur.stop(0);
        }
        try {
            bdManager.deconnecter();
        } catch (SQLException e) {
            System.err.println("✗ Error while disconnecting from DB: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        RestServer serveur = new RestServer();
        serveur.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serveur.stop();
        }));

        // Si le serveur a bien démarré, on bloque le thread principal pour garder
        // le processus actif (HttpServer démarre des threads en arrière-plan mais
        // il est utile d'empêcher la fin immédiate de main si start() a réussi).
        if (serveur.serveur != null) {
            System.out.println("Serveur REST en cours d'exécution. Ctrl+C pour arrêter.");
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            System.err.println("Serveur REST n'a pas démarré correctement. Voir les logs pour les détails.");
        }
    }
}
