package org.example.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.PatientDAO;
import org.example.server.entity.Patient;
import org.example.rest.RestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class PatientsHandler extends ApiHandler {

    public PatientsHandler(BdManager bdManager) {
        super(bdManager);
    }

    @Override
    protected void gererPost(HttpExchange echange) throws IOException {
        String corps = lireCorps(echange);
        String typeContenu = echange.getRequestHeaders().getFirst("Content-Type");

        Map<String, String> donnees;
        if (typeContenu != null && typeContenu.contains("json")) {
            Map<String, Object> json = RestUtils.parserJson(corps, Map.class);
            donnees = new HashMap<>();
            if (json != null) {
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    Object val = entry.getValue();
                    if (val != null) {
                        donnees.put(entry.getKey(), val.toString());
                    } else {
                        donnees.put(entry.getKey(), null);
                    }
                }
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

        // On vérifie si c'est un nouveau patient
        boolean nouveauPatient = "true".equals(nouveauPatientStr);

        Connection connexion = obtenirConnexion();
        PatientDAO dao = new PatientDAO(connexion);

        int idPatient;
        if (nouveauPatient) {
            // Création d'un nouveau patient
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
