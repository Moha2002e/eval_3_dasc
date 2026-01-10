package org.example.client.crypto;

import org.example.server.crypto.GestionnaireCrypto;
import org.example.server.crypto.GenerateurCles;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;


public class GestionnaireCryptoClient {

    private PrivateKey clePriveeClient;
    private PublicKey clePubliqueServeur;
    private SecretKey cleSession;

    public GestionnaireCryptoClient() {
        chargerCles();
    }


    private void chargerCles() {
        try {
            clePriveeClient = GenerateurCles.chargerClePrivee("src/main/resources/keys/client_prive.key");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la clé privée: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            clePubliqueServeur = GenerateurCles.chargerClePublique("src/main/resources/keys/serveur_public.key");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la clé publique: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void genererCleSession() throws Exception {
        cleSession = GestionnaireCrypto.genererCleSession();
    }


    public byte[] chiffrerCleSession() throws Exception {
        if (cleSession == null) {
            throw new Exception("La clé de session n'est pas initialisée");
        }

        if (clePubliqueServeur == null) {
            throw new Exception("La clé publique du serveur n'est pas chargée");
        }

        byte[] donneesCle = cleSession.getEncoded();
        return GestionnaireCrypto.chiffrerRSA(donneesCle, clePubliqueServeur);
    }


    public byte[] calculerDigestSale(String login, String motDePasse, byte[] sel) throws Exception {
        if (login == null) {
            throw new Exception("Le login est nul");
        }

        if (motDePasse == null) {
            throw new Exception("Le mot de passe est nul");
        }

        if (sel == null) {
            throw new Exception("Le sel est nul");
        }

        return GestionnaireCrypto.calculerDigestSale(login, motDePasse, sel);
    }


    public byte[] chiffrer(String donnees) throws Exception {
        if (donnees == null) {
            throw new Exception("Les données à chiffrer sont nulles");
        }

        if (cleSession == null) {
            throw new Exception("La clé de session n'est pas initialisée");
        }

        byte[] donneesBytes = donnees.getBytes();
        return GestionnaireCrypto.chiffrerAES(donneesBytes, cleSession);
    }


    public String dechiffrer(byte[] donnees) throws Exception {
        if (donnees == null) {
            throw new Exception("Les données à déchiffrer sont nulles");
        }

        if (cleSession == null) {
            throw new Exception("La clé de session n'est pas initialisée");
        }

        byte[] donneesDechiffrees = GestionnaireCrypto.dechiffrerAES(donnees, cleSession);
        return new String(donneesDechiffrees);
    }


    public byte[] signer(byte[] donnees) throws Exception {
        if (donnees == null) {
            throw new Exception("Les données à signer sont nulles");
        }

        if (clePriveeClient == null) {
            throw new Exception("La clé privée du client n'est pas chargée");
        }

        return GestionnaireCrypto.signer(donnees, clePriveeClient);
    }


    public boolean verifierHMAC(byte[] donnees, byte[] hmac) throws Exception {
        if (donnees == null) {
            throw new Exception("Les données à vérifier sont nulles");
        }

        if (hmac == null) {
            throw new Exception("Le HMAC à vérifier est nul");
        }

        if (cleSession == null) {
            throw new Exception("La clé de session n'est pas initialisée");
        }

        return GestionnaireCrypto.verifierHMAC(donnees, hmac, cleSession);
    }

    public SecretKey getCleSession() {
        if (cleSession == null) {
            return null;
        } else {
            return cleSession;
        }
    }
}
