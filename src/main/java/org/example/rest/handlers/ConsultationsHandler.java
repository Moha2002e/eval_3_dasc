package org.example.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.ConsultationDAO;
import org.example.server.entity.Consultation;
import org.example.rest.RestUtils;
import org.example.server.searchvm.ConsultationSearchVM;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConsultationsHandler extends ApiHandler {

    public ConsultationsHandler(BdManager bdManager) {
        super(bdManager);
    }

    @Override
    protected void gererGet(HttpExchange echange) throws IOException {
        Map<String, String> parametres = obtenirParametresRequete(echange);
        ConsultationSearchVM vm = new ConsultationSearchVM();

        if (parametres.containsKey("date")) {
            vm.setDateFrom(parametres.get("date"));
            vm.setDateTo(parametres.get("date"));
        }
        if (parametres.containsKey("doctor")) {
            vm.setDoctorName(parametres.get("doctor"));
        }
        if (parametres.containsKey("specialty")) {
            vm.setSpecialityName(parametres.get("specialty"));
        }

        Connection connexion = obtenirConnexion();
        ConsultationDAO dao = new ConsultationDAO(connexion);
        ArrayList<Consultation> consultations = dao.load(vm);

        String idPatientStr = parametres.get("patientId");
        if (idPatientStr != null) {
            try {
                int idPatient = Integer.parseInt(idPatientStr);
                ArrayList<Consultation> consultationsFiltrees = new ArrayList<>();
                for (Consultation c : consultations) {
                    // Si l'id patient correspond, on garde la consultation
                    if (c.getPatient_id() != null && c.getPatient_id() == idPatient) {
                        consultationsFiltrees.add(c);
                    }
                }
                consultations = consultationsFiltrees;
            } catch (NumberFormatException e) {
                envoyerErreur(echange, 400, "Format invalide pour patientId");
                return;
            }
        } else {
            // Si pas de patientId, on ne garde que les consultations libres (patientId ==
            // null)
            ArrayList<Consultation> consultationsFiltrees = new ArrayList<>();
            for (Consultation c : consultations) {
                if (c.getPatient_id() == null) {
                    consultationsFiltrees.add(c);
                }
            }
            consultations = consultationsFiltrees;
        }

        envoyerJson(echange, 200, consultations);
    }

    @Override
    protected void gererPut(HttpExchange echange) throws IOException {
        // Etape 1 : Récupérer l'ID de la consultation dans l'URL
        Map<String, String> parametres = obtenirParametresRequete(echange);
        String idStr = parametres.get("id");

        // On vérifie que l'ID est bien présent
        if (idStr == null) {
            envoyerErreur(echange, 400, "Paramètre 'id' manquant");
            return;
        }

        // On convertit l'ID en entier
        int idConsultation;
        try {
            idConsultation = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            envoyerErreur(echange, 400, "Format invalide pour id");
            return;
        }

        // Etape 2 : Lire le corps de la requête (les données envoyées)
        String corps = lireCorps(echange);
        if (corps == null || corps.isEmpty()) {
            envoyerErreur(echange, 400, "Body de la requête vide");
            return;
        }

        // Etape 3 : Transformer le corps (JSON ou Formulaire) en une liste de données
        // simple (Map)
        Map<String, String> donnees = new HashMap<>(); // On va stocker les infos ici
        String typeContenu = echange.getRequestHeaders().getFirst("Content-Type");

        if (typeContenu != null && typeContenu.contains("json")) {
            // C'est du JSON, on le traite
            try {
                Map<String, Object> json = RestUtils.parserJson(corps, Map.class);
                if (json == null) {
                    envoyerErreur(echange, 400, "Format JSON invalide");
                    return;
                }
                // On parcourt chaque élément du JSON pour le mettre dans notre Map 'donnees'
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    String cle = entry.getKey();
                    Object valeur = entry.getValue();

                    if (valeur == null) {
                        donnees.put(cle, null);
                    } else if (valeur instanceof Number) {
                        // Si c'est un nombre (ex: 123), on le met en texte
                        int valeurEntiere = ((Number) valeur).intValue();
                        donnees.put(cle, String.valueOf(valeurEntiere));
                    } else {
                        // Sinon c'est du texte, on le prend tel quel
                        donnees.put(cle, valeur.toString());
                    }
                }
            } catch (Exception e) {
                envoyerErreur(echange, 400, "Erreur lors de la lecture du JSON");
                return;
            }
        } else {
            // Ce n'est pas du JSON (c'est probablement un formulaire standard), on utilise
            // notre utilitaire
            donnees = RestUtils.parserFormulaire(corps);
        }

        // Etape 4 : Récupérer les informations précises (patientId et reason)
        String idPatientStr = donnees.get("patientId");
        String raison = donnees.get("reason");

        if (idPatientStr == null) {
            envoyerErreur(echange, 400, "Champ 'patientId' manquant");
            return;
        }

        int idPatient;
        try {
            idPatient = Integer.parseInt(idPatientStr.trim());
        } catch (NumberFormatException e) {
            envoyerErreur(echange, 400, "Le patientId doit être un nombre entier");
            return;
        }

        // Etape 5 : Enregistrer la réservation dans la base de données
        Connection connexion = obtenirConnexion();
        ConsultationDAO dao = new ConsultationDAO(connexion);
        boolean succes = dao.bookConsultation(idConsultation, idPatient, raison);

        // Etape 6 : Envoyer la réponse au client
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", succes);

        if (succes) {
            envoyerJson(echange, 200, reponse);
        } else {
            envoyerJson(echange, 400, reponse);
        }

    }

    @Override
    protected void gererDelete(HttpExchange echange) throws IOException {
        Map<String, String> parametres = obtenirParametresRequete(echange);
        String idStr = parametres.get("id");
        if (idStr == null) {
            envoyerErreur(echange, 400, "Paramètre 'id' manquant");
            return;
        }
        int idConsultation;
        try {
            idConsultation = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            envoyerErreur(echange, 400, "Format invalide pour id");
            return;
        }

        Connection connexion = obtenirConnexion();
        ConsultationDAO dao = new ConsultationDAO(connexion);
        boolean succes = dao.cancelConsultation(idConsultation);

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", succes);
        int codeReponse;
        if (succes) {
            codeReponse = 200;
        } else {
            codeReponse = 404;
        }
        envoyerJson(echange, codeReponse, reponse);
    }
}
