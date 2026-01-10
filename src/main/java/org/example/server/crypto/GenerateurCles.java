package org.example.server.crypto;

import java.io.*;
import java.security.*;


public class GenerateurCles {

    private static final String DOSSIER_CLES = "src/main/resources/keys/";

    public static void main(String[] args) {
        try {
            new File(DOSSIER_CLES).mkdirs();


            KeyPair paireServeur = GestionnaireCrypto.genererPaireClesRSA();
            KeyPair paireClient = GestionnaireCrypto.genererPaireClesRSA();


            sauvegarderCle(paireServeur.getPublic(), DOSSIER_CLES + "serveur_public.key");
            sauvegarderCle(paireServeur.getPrivate(), DOSSIER_CLES + "serveur_prive.key");
            sauvegarderCle(paireClient.getPublic(), DOSSIER_CLES + "client_public.key");
            sauvegarderCle(paireClient.getPrivate(), DOSSIER_CLES + "client_prive.key");


        } catch (Exception e) {
            System.err.println("✗ Erreur génération clés: " + e.getMessage());
        }
    }


    private static void sauvegarderCle(Key cle, String cheminFichier) throws IOException {
        String base64 = GestionnaireCrypto.cleVersBase64(cle);
        try (FileWriter writer = new FileWriter(cheminFichier)) {
            writer.write(base64);
        }
    }


    public static PublicKey chargerClePublique(String cheminFichier) throws Exception {
        String base64 = lireFichier(cheminFichier);
        return GestionnaireCrypto.base64VersClePublique(base64);
    }


    public static PrivateKey chargerClePrivee(String cheminFichier) throws Exception {
        String base64 = lireFichier(cheminFichier);
        return GestionnaireCrypto.base64VersClePrivee(base64);
    }


    private static String lireFichier(String cheminFichier) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(cheminFichier))) {
            StringBuilder contenu = new StringBuilder();
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                contenu.append(ligne);
            }
            return contenu.toString();
        }
    }
}
