package org.example.server.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.DoctorDAO;
import org.example.server.entity.Specialty;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;

/**
 * Handler pour l'endpoint /api/specialties
 * 
 * Route: GET /api/specialties
 * Retourne: Liste de toutes les spécialités en JSON
 */
public class SpecialtiesHandler extends ApiHandler {

    /**
     * Constructeur du handler.
     * 
     * @param bdManager Le gestionnaire de base de données
     */
    public SpecialtiesHandler(BdManager bdManager) {
        super(bdManager);
    }

    /**
     * Gère les requêtes GET pour récupérer toutes les spécialités.
     * 
     * @param echange L'objet HttpExchange contenant la requête
     * @throws IOException En cas d'erreur d'I/O
     */
    @Override
    protected void gererGet(HttpExchange echange) throws IOException {
        Connection connexion = obtenirConnexion();
        DoctorDAO daoMedecin = new DoctorDAO(connexion);
        ArrayList<Specialty> specialites = daoMedecin.getAllSpecialties();
        envoyerJson(echange, 200, specialites);
    }
}

