package org.example.server.rest;

import com.sun.net.httpserver.HttpServer;
import org.example.server.bd.BdManager;
import org.example.server.rest.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;

/**
 * Serveur REST pour l'API de gestion médicale.
 * Écoute sur le port 8080 et expose les endpoints :
 * - /api/specialties : Liste des spécialités
 * - /api/doctors : Recherche de médecins
 * - /api/patients : Gestion des patients
 * - /api/consultations : Gestion des consultations
 */
public class RestServer {

    private static final int PORT = 8080;
    private HttpServer serveur;
    private BdManager bdManager;

    /**
     * Constructeur.
     */
    public RestServer() {
        bdManager = new BdManager();
    }

    /**
     * Démarre le serveur REST.
     * Crée les routes et associe chaque route à son handler.
     */
    public void start() {
        try {
            bdManager.connecter();
            serveur = HttpServer.create(new InetSocketAddress(PORT), 0);

            serveur.createContext("/api/specialties", new SpecialtiesHandler(bdManager));
            serveur.createContext("/api/doctors", new DoctorsHandler(bdManager));
            serveur.createContext("/api/patients", new PatientsHandler(bdManager));
            serveur.createContext("/api/consultations", new ConsultationsHandler(bdManager));

            serveur.start();
        } catch (IOException e) {
            // Erreur de démarrage du serveur
        } catch (SQLException e) {
            // Erreur de connexion à la base de données
        }
    }

    /**
     * Arrête le serveur.
     */
    public void stop() {
        if (serveur != null) {
            serveur.stop(0);
        }
        try {
            bdManager.deconnecter();
        } catch (SQLException e) {
            // Erreur de déconnexion de la base de données
        }
    }

    /**
     * Point d'entrée du programme.
     * 
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        RestServer serveur = new RestServer();
        serveur.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serveur.stop();
        }));
    }
}
