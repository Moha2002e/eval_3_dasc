package org.example.server.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.DoctorDAO;
import org.example.server.entity.Doctor;
import org.example.server.searchvm.DoctorSearchVM;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

/**
 * Handler pour l'endpoint /api/doctors
 * 
 * Routes:
 * - GET /api/doctors
 * - GET /api/doctors?name=Maboul
 * - GET /api/doctors?specialty=Neurologie
 * - GET /api/doctors?name=Maboul&specialty=Neurologie
 * 
 * Retourne: Liste des médecins trouvés selon les critères en JSON
 */
public class DoctorsHandler extends ApiHandler {

    /**
     * Constructeur du handler.
     * 
     * @param bdManager Le gestionnaire de base de données
     */
    public DoctorsHandler(BdManager bdManager) {
        super(bdManager);
    }

    /**
     * Gère les requêtes GET pour rechercher des médecins.
     * Extrait les paramètres de recherche (name, specialty) de l'URL
     * et retourne la liste des médecins correspondants.
     * 
     * @param echange L'objet HttpExchange contenant la requête
     * @throws IOException En cas d'erreur d'I/O
     */
    @Override
    protected void gererGet(HttpExchange echange) throws IOException {
        Map<String, String> parametres = obtenirParametresRequete(echange);
        DoctorSearchVM vm = new DoctorSearchVM();
        
        if (parametres.containsKey("name")) {
            vm.setLastName(parametres.get("name"));
        }
        if (parametres.containsKey("specialty")) {
            vm.setSpecialityName(parametres.get("specialty"));
        }

        Connection connexion = obtenirConnexion();
        DoctorDAO daoMedecin = new DoctorDAO(connexion);
        ArrayList<Doctor> medecins = daoMedecin.load(vm);
        envoyerJson(echange, 200, medecins);
    }
}

