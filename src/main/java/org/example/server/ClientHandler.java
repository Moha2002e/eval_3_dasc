package org.example.server;

import org.example.server.bd.BdManager;
import org.example.server.crypto.GenerateurCles;
import org.example.server.crypto.GestionnaireCrypto;
import org.example.server.dao.ReportDAO;
import org.example.server.entity.Patient;
import org.example.server.searchvm.PatientSearchVM;
import org.example.shared.Protocol;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;


public class ClientHandler implements Runnable {
    private final Socket socketClient;
    private final BdManager gestionnaireBd;
    private BufferedReader entree;
    private PrintWriter sortie;


    private Integer idMedecinCourant;
    private String loginCourant;
    private SecretKey cleSession;
    private byte[] selCourant;
    private boolean authentifie;


    private PrivateKey clePriveeServeur;
    private PublicKey clePubliqueClient;


    public ClientHandler(Socket socket, BdManager gestionnaireBd) {
        this.socketClient = socket;
        this.gestionnaireBd = gestionnaireBd;
        this.authentifie = false;


        try {
            this.clePriveeServeur = GenerateurCles.chargerClePrivee("src/main/resources/keys/serveur_prive.key");
            this.clePubliqueClient = GenerateurCles.chargerClePublique("src/main/resources/keys/client_public.key");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des clés: " + e.getMessage());
        }
    }


    @Override
    public void run() {
        try {

            entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            sortie = new PrintWriter(socketClient.getOutputStream(), true);

            String requete;
            while ((requete = entree.readLine()) != null) {
                traiterRequete(requete);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur client: " + e.getMessage());
        } finally {
            nettoyer();
        }
    }


    private void traiterRequete(String requete) {
        String[] parties = requete.split("\\|", -1);
        if (parties.length == 0) {
            envoyerErreur("Format de requête invalide");
            return;
        }

        String commande = parties[0];

        try {
            switch (commande) {
                case Protocol.CMD_LOGIN:
                    traiterLogin(parties);
                    break;
                case Protocol.CMD_ADD_REPORT:
                    if (verifierAuthentification()) {
                        traiterAjoutRapport(parties);
                    }
                    break;
                case Protocol.CMD_EDIT_REPORT:
                    if (verifierAuthentification()) {
                        traiterModificationRapport(parties);
                    }
                    break;
                case Protocol.CMD_LIST_REPORTS:
                    if (verifierAuthentification()) {
                        traiterListeRapports(parties);
                    }
                    break;
                case Protocol.CMD_LIST_PATIENTS:
                    traiterListePatients();
                    break;
                case Protocol.CMD_LOGOUT:
                    traiterLogout();
                    break;
                default:
                    envoyerErreur("Commande inconnue: " + commande);
            }
        } catch (Exception e) {
            envoyerErreur("Erreur lors du traitement: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void traiterLogin(String[] parties) throws Exception {
        if (parties.length < 2) {
            envoyerErreur("Format LOGIN invalide");
            return;
        }

        String login = parties[1];


        if (parties.length == 2) {
            if (!gestionnaireBd.medecinExiste(login)) {
                envoyerErreur("Médecin inexistant");
                return;
            }

            loginCourant = login;
            selCourant = GestionnaireCrypto.genererSel(Protocol.SALT_SIZE);
            String selBase64 = Base64.getEncoder().encodeToString(selCourant);
            sortie.println(Protocol.RESP_SALT + "|" + selBase64); // Envoyer le sel au client (c'est le fichier qui continue le protocole)

            return;
        }


        if (parties.length >= 3) {
            String digestRecuBase64 = parties[1];
            String cleSessionChiffreeBase64 = parties[2];


            String motDePasseHash = gestionnaireBd.getMotDePasseMedecin(loginCourant);
            if (motDePasseHash == null) {
                envoyerErreur("Authentification échouée");
                return;
            }


            byte[] digestAttendu = GestionnaireCrypto.calculerDigestSale(
                    loginCourant, motDePasseHash, selCourant);


            byte[] digestRecu = Base64.getDecoder().decode(digestRecuBase64);
            if (!java.security.MessageDigest.isEqual(digestAttendu, digestRecu)) {
                envoyerErreur("Authentification échouée");
                return;
            }


            byte[] cleSessionChiffree = Base64.getDecoder().decode(cleSessionChiffreeBase64);
            byte[] cleSessionBytes = GestionnaireCrypto.dechiffrerRSA(cleSessionChiffree, clePriveeServeur);
            //transforme en string, et transforme cle secrète qui est la clé de session
            cleSession = GestionnaireCrypto.base64VersCleSecrete(Base64.getEncoder().encodeToString(cleSessionBytes)); // elle permet de reconstituer la clé de session


            idMedecinCourant = gestionnaireBd.getIdMedecin(loginCourant);
            authentifie = true;

            String sessionId = "session_" + System.currentTimeMillis();
            sortie.println(Protocol.RESP_OK + "|" + sessionId);
            System.out.println("✓ Médecin authentifié: " + loginCourant + " (ID: " + idMedecinCourant + ")");
        }
    }



    private void traiterAjoutRapport(String[] parties) throws Exception {
        if (parties.length < 5) {
            envoyerErreur("Format ADD_REPORT invalide");
            return;
        }


        byte[] dateChiffree = Base64.getDecoder().decode(parties[1]);
        byte[] patientIdChiffre = Base64.getDecoder().decode(parties[2]);
        byte[] texteChiffre = Base64.getDecoder().decode(parties[3]);
        byte[] signature = Base64.getDecoder().decode(parties[4]);

        String dateStr = new String(GestionnaireCrypto.dechiffrerAES(dateChiffree, cleSession));
        String patientIdStr = new String(GestionnaireCrypto.dechiffrerAES(patientIdChiffre, cleSession));
        String texteRapport = new String(GestionnaireCrypto.dechiffrerAES(texteChiffre, cleSession));


        byte[] donneesAVerifier = (dateStr + patientIdStr + texteRapport).getBytes();
        if (!GestionnaireCrypto.verifierSignature(donneesAVerifier, signature, clePubliqueClient)) {
            envoyerErreur("Signature invalide");
            return;
        }

        int patientId = Integer.parseInt(patientIdStr);


        if (!gestionnaireBd.medecinAConsultationAvecPatient(idMedecinCourant, patientId)) {
            envoyerErreur("Aucune consultation trouvée avec ce patient");
            return;
        }


        int rapportId = gestionnaireBd.ajouterRapport(idMedecinCourant, patientId, dateStr, texteRapport);

        sortie.println(Protocol.RESP_OK + "|" + rapportId);
        System.out.println("✓ Rapport ajouté (ID: " + rapportId + ")");
    }



    private void traiterModificationRapport(String[] parties) throws Exception {
        if (parties.length < 3) {
            envoyerErreur("Format EDIT_REPORT invalide");
            return;
        }


        byte[] rapportIdChiffre = Base64.getDecoder().decode(parties[1]);
        byte[] texteChiffre = Base64.getDecoder().decode(parties[2]);

        String rapportIdStr = new String(GestionnaireCrypto.dechiffrerAES(rapportIdChiffre, cleSession));
        String nouveauTexte = new String(GestionnaireCrypto.dechiffrerAES(texteChiffre, cleSession));

        int rapportId = Integer.parseInt(rapportIdStr);

        boolean succes = gestionnaireBd.modifierRapport(rapportId, nouveauTexte, idMedecinCourant);

        if (succes) {
            sortie.println(Protocol.RESP_OK);

        } else {
            envoyerErreur("Rapport non trouvé ou non autorisé");
        }
    }



    private void traiterListeRapports(String[] parties) throws Exception {
        List<ReportDAO.Rapport> rapports;


        if (parties.length >= 2 && !parties[1].isEmpty()) {
            byte[] patientIdChiffre = Base64.getDecoder().decode(parties[1]);
            String patientIdStr = new String(GestionnaireCrypto.dechiffrerAES(patientIdChiffre, cleSession));
            int patientId = Integer.parseInt(patientIdStr);
            rapports = gestionnaireBd.listerRapportsMedecinPatient(idMedecinCourant, patientId);
        } else {
            rapports = gestionnaireBd.listerRapportsMedecin(idMedecinCourant);
        }


        Gson gson = new Gson();
        String json = gson.toJson(rapports);


        byte[] jsonChiffre = GestionnaireCrypto.chiffrerAES(json.getBytes(), cleSession);
        String jsonChiffreBase64 = Base64.getEncoder().encodeToString(jsonChiffre);


        byte[] hmac = GestionnaireCrypto.calculerHMAC(jsonChiffre, cleSession);
        String hmacBase64 = Base64.getEncoder().encodeToString(hmac);

        sortie.println(Protocol.RESP_OK + "|" + rapports.size() + "|" + jsonChiffreBase64 + "|" + hmacBase64);

    }




    private void traiterListePatients() throws Exception {

        PatientSearchVM searchVM = new PatientSearchVM();
        searchVM.setDoctorId(idMedecinCourant);
        List<Patient> patients = gestionnaireBd.load(searchVM);


        StringBuilder reponse = new StringBuilder(Protocol.RESP_OK);
        for (Patient patient : patients) {
            reponse.append("|");
            reponse.append(patient.getId()).append(",");
            reponse.append(patient.getFirst_name()).append(",");
            reponse.append(patient.getLast_name()).append(",");
            String dateNaissance;
            if (patient.getBirth_date() != null) {
                dateNaissance = patient.getBirth_date();
            } else {
                dateNaissance = "";
            }
            reponse.append(dateNaissance);
        }

        sortie.println(reponse.toString());

    }



    private void traiterLogout() {
        authentifie = false;
        idMedecinCourant = null;
        loginCourant = null;
        cleSession = null;
        selCourant = null;
        sortie.println(Protocol.RESP_OK);

    }


    private boolean verifierAuthentification() {
        if (!authentifie) {
            envoyerErreur("Non authentifié");
            return false;
        }
        return true;
    }


    private void envoyerErreur(String message) {
        sortie.println(Protocol.RESP_ERROR + "|" + message);
    }


    private void nettoyer() {
        try {
            if (entree != null)
                entree.close();
            if (sortie != null)
                sortie.close();
            if (socketClient != null)
                socketClient.close();





            if (socketClient != null && socketClient.getInetAddress() != null) {
                System.out.println("Connexion client fermée: " + socketClient.getInetAddress());
            } else {
                System.out.println("Connexion client fermée");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture: " + e.getMessage());
        }
    }
}
