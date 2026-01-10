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

        String corps = lireCorps(echange);
        if (corps == null || corps.trim().isEmpty()) {
            envoyerErreur(echange, 400, "Body de la requête vide");
            return;
        }

        String typeContenu = echange.getRequestHeaders().getFirst("Content-Type");

        Map<String, String> donnees;
        if (typeContenu != null && typeContenu.contains("json")) {
            try {
                Map<String, Object> json = RestUtils.parserJson(corps, Map.class);
                if (json == null) {
                    envoyerErreur(echange, 400, "Format JSON invalide");
                    return;
                }
                donnees = new HashMap<>();
                for (Map.Entry<String, Object> entree : json.entrySet()) {
                    Object valeur = entree.getValue();
                    String cle = entree.getKey();
                    if (valeur == null) {
                        donnees.put(cle, null);
                    } else if (valeur instanceof Number) {

                        int valeurInt = ((Number) valeur).intValue();
                        donnees.put(cle, String.valueOf(valeurInt));
                        System.out.println("DEBUG: " + cle + " = " + valeur + " (type: " + valeur.getClass().getSimpleName() + ") -> " + valeurInt);
                    } else {
                        String valeurStr = valeur.toString();
                        donnees.put(cle, valeurStr);
                        System.out.println("DEBUG: " + cle + " = " + valeurStr + " (type: " + valeur.getClass().getSimpleName() + ")");
                    }
                }
            } catch (Exception e) {
                envoyerErreur(echange, 400, "Erreur de parsing JSON: " + e.getMessage());
                return;
            }
        } else {
            donnees = RestUtils.parserFormulaire(corps);
        }

        String idPatientStr = donnees.get("patientId");
        int longueurPatientId;
        if (idPatientStr != null) {
            longueurPatientId = idPatientStr.length();
        } else {
            longueurPatientId = 0;
        }
        System.out.println("DEBUG: patientId reçu = '" + idPatientStr + "' (longueur: " + longueurPatientId + ")");
        if (idPatientStr == null) {
            envoyerErreur(echange, 400, "Champ 'patientId' manquant");
            return;
        }
        int idPatient;
        try {


            idPatientStr = idPatientStr.trim();
            System.out.println("DEBUG: patientId après trim = '" + idPatientStr + "'");
            double patientIdDouble = Double.parseDouble(idPatientStr);
            idPatient = (int) patientIdDouble;
            System.out.println("DEBUG: patientId parsé = " + idPatient + " (double: " + patientIdDouble + ")");
            if (patientIdDouble != idPatient) {
                envoyerErreur(echange, 400, "Format invalide pour patientId: doit être un entier (reçu: " + patientIdDouble + ")");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("DEBUG: Exception lors du parsing: " + e.getMessage());
            envoyerErreur(echange, 400, "Format invalide pour patientId: '" + idPatientStr + "' (" + e.getMessage() + ")");
            return;
        }
        String raison = donnees.get("reason");

        Connection connexion = obtenirConnexion();
        ConsultationDAO dao = new ConsultationDAO(connexion);
        boolean succes = dao.bookConsultation(idConsultation, idPatient, raison);

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", succes);
        int codeReponse;
        if (succes) {
            codeReponse = 200;
        } else {
            codeReponse = 400;
        }
        envoyerJson(echange, codeReponse, reponse);
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

