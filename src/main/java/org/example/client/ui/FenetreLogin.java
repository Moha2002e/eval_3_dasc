package org.example.client.ui;

import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;
import org.example.shared.Protocol;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;

/**
 * Fenêtre de connexion de l'application client.
 * <p>
 * Gère l'authentification sécurisée du médecin auprès du serveur.
 * Le processus comprend :
 * 1. Connexion TCP initiale.
 * 2. Échange de sel (Salt) pour le hachage.
 * 3. Génération et échange sécurisé de la clé de session (AES).
 * 4. Validation des identifiants.
 * </p>
 */
public class FenetreLogin extends JFrame {

    // --- Constantes d'interface ---
    private static final String TITRE_FENETRE = "Connexion - Serveur Rapport Médical";
    private static final String TITRE_APP = "Serveur Rapport Médical Sécurisé";
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    // --- Composants UI ---
    private JTextField champLogin;
    private JPasswordField champMotDePasse;
    private JButton boutonConnecter;

    // --- Gestionnaires Logiques ---
    private GestionnaireConnexion gestionnaireConnexion;
    private GestionnaireCryptoClient gestionnaireCrypto;

    public FenetreLogin() {
        // Initialisation des gestionnaires logic
        gestionnaireConnexion = new GestionnaireConnexion();
        gestionnaireCrypto = new GestionnaireCryptoClient();

        initialiserInterface();
    }

    /**
     * Configure et affiche l'interface graphique de la fenêtre de login.
     */
    private void initialiserInterface() {
        setTitle(TITRE_FENETRE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300); // Taille ajustée sans les champs serveur
        setLocationRelativeTo(null); // Centrer à l'écran

        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // 1. Titre de l'application
        JLabel titre = new JLabel(TITRE_APP);
        titre.setFont(new Font("Arial", Font.BOLD, 18));
        titre.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelPrincipal.add(titre);
        panelPrincipal.add(Box.createRigidArea(new Dimension(0, 30)));

        // 2. Panel Identifiants (Login/Pass)
        JPanel panelIdentifiants = new JPanel(new GridLayout(2, 2, 10, 10));
        panelIdentifiants.setBorder(BorderFactory.createTitledBorder("Identifiants Médecin"));
        panelIdentifiants.add(new JLabel("Login:"));
        champLogin = new JTextField();
        panelIdentifiants.add(champLogin);
        panelIdentifiants.add(new JLabel("Mot de passe:"));
        champMotDePasse = new JPasswordField();
        panelIdentifiants.add(champMotDePasse);

        panelPrincipal.add(panelIdentifiants);
        panelPrincipal.add(Box.createRigidArea(new Dimension(0, 30)));

        // 3. Bouton de Connexion
        boutonConnecter = new JButton("Se Connecter");
        boutonConnecter.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonConnecter.setFont(new Font("Arial", Font.BOLD, 14));

        boutonConnecter.addActionListener(e -> lancerConnexion());
        getRootPane().setDefaultButton(boutonConnecter);

        panelPrincipal.add(boutonConnecter);

        add(panelPrincipal);
    }

    /**
     * Lance le processus de connexion dans un thread séparé pour ne pas figer l'UI.
     */
    private void lancerConnexion() {
        String login = champLogin.getText().trim();
        String motDePasse = new String(champMotDePasse.getPassword());

        // Validation basique des champs
        if (login.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        // Désactivation du bouton pour éviter les doubles clics
        setInterfaceActive(false);

        // Exécution en arrière-plan avec les paramètres en dur
        new Thread(() -> executerProtocoleConnexion(HOST, PORT, login, motDePasse)).start();
    }

    /**
     * Exécute la logique métier de la connexion (Réseau + Crypto).
     */
    private void executerProtocoleConnexion(String hote, int port, String login, String motDePasse) {
        try {
            // A. Connexion TCP
            if (!gestionnaireConnexion.connecter(hote, port)) {
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Impossible de se connecter au serveur (Connexion refusée).");
                    setInterfaceActive(true);
                });
                return;
            }

            // B. Envoi Login pour récupérer le Sel (Salt)
            gestionnaireConnexion.envoyerRequete(Protocol.CMD_LOGIN + "|" + login);
            String reponse = gestionnaireConnexion.recevoirReponse();

            if (reponse == null || reponse.startsWith(Protocol.RESP_ERROR)) {
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Login refusé ou compte inexistant.");
                    setInterfaceActive(true);
                });
                return;
            }

            // C. Traitement du Sel et Calcul du Digest
            String[] parties = reponse.split("\\|");
            if (!parties[0].equals(Protocol.RESP_SALT)) {
                throw new Exception("Protocole invalide : Attendu SALT, reçu " + reponse);
            }
            byte[] sel = Base64.getDecoder().decode(parties[1]);
            byte[] digest = gestionnaireCrypto.calculerDigestSale(login, motDePasse, sel);
            String digestBase64 = Base64.getEncoder().encodeToString(digest);

            // D. Génération et Chiffrement de la Clé de Session
            gestionnaireCrypto.genererCleSession();
            byte[] cleSessionChiffree = gestionnaireCrypto.chiffrerCleSession();
            String cleSessionBase64 = Base64.getEncoder().encodeToString(cleSessionChiffree);

            // E. Envoi des identifiants sécurisés (Digest + Clé Session)
            gestionnaireConnexion.envoyerRequete(Protocol.CMD_LOGIN + "|" + digestBase64 + "|" + cleSessionBase64);
            reponse = gestionnaireConnexion.recevoirReponse();

            if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                // F. Succès final
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connexion réussie!", "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    ouvrirFenetrePrincipale(login);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Authentification échouée (Mot de passe incorrect ?).");
                    setInterfaceActive(true);
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                afficherErreur("Erreur technique : " + ex.getMessage());
                setInterfaceActive(true);
            });
        }
    }

    private void ouvrirFenetrePrincipale(String login) {
        FenetrePrincipale fenetrePrincipale = new FenetrePrincipale(login, gestionnaireConnexion, gestionnaireCrypto);
        fenetrePrincipale.setVisible(true);
        this.dispose(); // Fermer la fenêtre de login
    }

    private void afficherErreur(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void setInterfaceActive(boolean active) {
        boutonConnecter.setEnabled(active);
        boutonConnecter.setText(active ? "Se Connecter" : "Connexion en cours...");
    }

    public static void main(String[] args) {
        // Appliquer le thème moderne FlatMacLightLaf si disponible
        try {
            com.formdev.flatlaf.themes.FlatMacLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Impossible de charger le thème FlatLaf, utilisation du défaut.");
        }

        SwingUtilities.invokeLater(() -> {
            FenetreLogin fenetre = new FenetreLogin();
            fenetre.setVisible(true);
        });
    }
}
