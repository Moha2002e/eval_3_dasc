package org.example.server.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class GestionnaireCrypto {

    private static final String ALGORITHME_SYMETRIQUE = "AES";
    private static final String ALGORITHME_ASYMETRIQUE = "RSA";
    private static final String ALGORITHME_HASH = "SHA-256";
    private static final String ALGORITHME_HMAC = "HmacSHA256";
    private static final String ALGORITHME_SIGNATURE = "SHA256withRSA";

    private static final int TAILLE_CLE_AES = 256;
    private static final int TAILLE_CLE_RSA = 2048;

    static {

        Security.addProvider(new BouncyCastleProvider()); // le provider c'est un ensemble d'algorithmes de chiffrement et de fonctions cryptographiques
    }


    public static SecretKey genererCleSession() throws NoSuchAlgorithmException {
        KeyGenerator generateur = KeyGenerator.getInstance(ALGORITHME_SYMETRIQUE);
        generateur.init(TAILLE_CLE_AES);
        return generateur.generateKey();
    }

    // Génère une paire de clés RSA (publique et privée)
    public static KeyPair genererPaireClesRSA() throws NoSuchAlgorithmException {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance(ALGORITHME_ASYMETRIQUE);
        generateur.initialize(TAILLE_CLE_RSA);
        return generateur.generateKeyPair();
    }

    // Chiffre les données avec AES elle sert a chiffrer les données de la session
    public static byte[] chiffrerAES(byte[] donnees, SecretKey cle) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHME_SYMETRIQUE);
        cipher.init(Cipher.ENCRYPT_MODE, cle);
        return cipher.doFinal(donnees); // retourne les données chiffrées (le dofinal effectue l'opération de chiffrement)
    }

    // on l'utilise pour déchiffrer les données de la session
    public static byte[] dechiffrerAES(byte[] donnees, SecretKey cle) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHME_SYMETRIQUE);
        cipher.init(Cipher.DECRYPT_MODE, cle);
        return cipher.doFinal(donnees);
    }


    public static byte[] chiffrerRSA(byte[] donnees, PublicKey clePublique) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHME_ASYMETRIQUE);
        cipher.init(Cipher.ENCRYPT_MODE, clePublique);
        return cipher.doFinal(donnees);
    }


    public static byte[] dechiffrerRSA(byte[] donnees, PrivateKey clePrivee) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHME_ASYMETRIQUE);
        cipher.init(Cipher.DECRYPT_MODE, clePrivee);
        return cipher.doFinal(donnees);
    }


    // on calcule le digest salé pour le mot de passe de l'utilisateur(le digest c'est une empreinte numérique)
    public static byte[] calculerDigestSale(String login, String motDePasse, byte[] sel)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHME_HASH);
        digest.update(login.getBytes());
        digest.update(motDePasse.getBytes());
        digest.update(sel);
        return digest.digest();
    }


    public static byte[] signer(byte[] donnees, PrivateKey clePrivee) throws Exception {
        Signature signature = Signature.getInstance(ALGORITHME_SIGNATURE);
        signature.initSign(clePrivee);
        signature.update(donnees);
        return signature.sign();
    }


    public static boolean verifierSignature(byte[] donnees, byte[] signatureBytes, PublicKey clePublique)
            throws Exception {
        Signature signature = Signature.getInstance(ALGORITHME_SIGNATURE);
        signature.initVerify(clePublique); // la cle c'est a qui on veut verifier la signature
        signature.update(donnees);
        return signature.verify(signatureBytes);
    }

    // je l'utilise pour calculer le HMAC des messages échangés entre le client et le serveur pour garantir l'intégrité et l'authenticité des messages
    public static byte[] calculerHMAC(byte[] donnees, SecretKey cle) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHME_HMAC);
        mac.init(cle);
        return mac.doFinal(donnees);
    }

    // je l'utilise pour vérifier le HMAC des messages échangés entre le client et le serveur
    public static boolean verifierHMAC(byte[] donnees, byte[] hmacRecu, SecretKey cle) throws Exception {
        byte[] hmacCalcule = calculerHMAC(donnees, cle);
        return MessageDigest.isEqual(hmacCalcule, hmacRecu);
    }

    // elle permet de stocker les clés sous forme de chaîne de caractères en base64(car c'est plus facile a stocker et a transmettre au niveau de la taille)
    public static String cleVersBase64(Key cle) {
        return Base64.getEncoder().encodeToString(cle.getEncoded());
    }

    // elle permet de reconstituer la clé publique a partir de la chaîne de caractères en base64
    public static PublicKey base64VersClePublique(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance(ALGORITHME_ASYMETRIQUE);
        return factory.generatePublic(spec);
    }

    // elle permet de reconstituer la clé privée a partir de la chaîne de caractères en base64
    public static PrivateKey base64VersClePrivee(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance(ALGORITHME_ASYMETRIQUE);
        return factory.generatePrivate(spec);
    }

    // elle permet de reconstituer la clé secrète symétrique a partir de la chaîne de caractères en base64
    public static SecretKey base64VersCleSecrete(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new SecretKeySpec(bytes, ALGORITHME_SYMETRIQUE);
    }

    // un sel c'est une valeur aléatoire qu'on ajoute au mot de passe avant de le hacher pour renforcer la sécurité
    public static byte[] genererSel(int taille) {
        SecureRandom random = new SecureRandom();
        byte[] sel = new byte[taille];
        random.nextBytes(sel);
        return sel;
    }
}
