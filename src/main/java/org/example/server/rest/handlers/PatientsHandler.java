package org.example.server.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.PatientDAO;
import org.example.server.entity.Patient;
import org.example.server.rest.RestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler pour l'endpoint /api/patients
 * 
 * Route: POST /api/patients
 * Body: JSON ou form-urlencoded avec lastName, firstName, birthDate, newPatient
 * 
 * Retourne: Succès + ID patient en JSON
 */
public class PatientsHandler extends ApiHandler {

    /**
     * Constructeur du handler.
     * 
     * @param bdManager Le gestionnaire de base de données
     */
    public PatientsHandler(BdManager bdManager) {
        super(bdManager); // Appel au constructeur parent
    }

    /**
     * Gère les requêtes POST pour créer ou vérifier l'existence d'un patient.
     * Si newPatient est true, crée un nouveau patient.
     * Sinon, vérifie si le patient existe déjà.
     * 
     * @param echange L'objet HttpExchange contenant la requête
     * @throws IOException En cas d'erreur d'I/O
     */
    @Override
    protected void gererPost(HttpExchange echange) throws IOException {
        String corps = lireCorps(echange);
        String typeContenu = echange.getRequestHeaders().getFirst("Content-Type");
        
        Map<String, String> donnees;
        if (typeContenu != null && typeContenu.contains("json")) {
            Map<String, Object> json = RestUtils.parserJson(corps, Map.class);
            donnees = new HashMap<>();
            for (Map.Entry<String, Object> entree : json.entrySet()) {
                donnees.put(entree.getKey(), entree.getValue().toString());
            }
        } else {
            donnees = RestUtils.parserFormulaire(corps);
        }

        String nom = donnees.get("lastName");
        String prenom = donnees.get("firstName");
        String dateNaissance = donnees.get("birthDate");
        String nouveauPatientStr = donnees.get("newPatient");

        if (nom == null || prenom == null || dateNaissance == null || nouveauPatientStr == null) {
            envoyerErreur(echange, 400, "Champs manquants: lastName, firstName, birthDate, newPatient requis");
            return;
        }

        boolean nouveauPatient = "true".equals(nouveauPatientStr);

        Connection connexion = obtenirConnexion();
        PatientDAO dao = new PatientDAO(connexion);
        
        int idPatient;
        if (nouveauPatient) {
            Patient patient = new Patient();
            patient.setLast_name(nom);
            patient.setFirst_name(prenom);
            patient.setBirth_date(dateNaissance);
            idPatient = dao.create(patient);
            if (idPatient == -1) {
                envoyerErreur(echange, 500, "Erreur lors de la création du patient");
                return;
            }
        } else {
            Integer idTrouve = dao.findByDetails(nom, prenom, dateNaissance);
            if (idTrouve == null) {
                envoyerErreur(echange, 404, "Patient non trouvé");
                return;
            }
            idPatient = idTrouve;
        }

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("patientId", idPatient);
        envoyerJson(echange, 200, reponse);
    }
}

