package org.example.rest;

import com.sun.net.httpserver.HttpServer;
import org.example.server.bd.BdManager;
import org.example.rest.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;


public class RestServer {

    private static final int PORT = 8080;
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
        } catch (IOException e) {

        } catch (SQLException e) {

        }
    }


    public void stop() {
        if (serveur != null) {
            serveur.stop(0);
        }
        try {
            bdManager.deconnecter();
        } catch (SQLException e) {

        }
    }


    public static void main(String[] args) {
        RestServer serveur = new RestServer();
        serveur.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serveur.stop();
        }));
    }
}
