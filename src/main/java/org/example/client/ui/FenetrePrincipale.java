package org.example.client.ui;

import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;

import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre Principale de l'application.
 * <p>
 * Agit comme un conteneur principal (Shell) qui héberge les différents
 * onglets fonctionnels (Ajout, Modification, Listes).
 * Elle gère aussi la déconnexion.
 * </p>
 */
public class FenetrePrincipale extends JFrame {

    private final String loginMedecin;
    private final GestionnaireConnexion gestionnaireConnexion;
    private final GestionnaireCryptoClient gestionnaireCrypto;

    // --- Composants UI ---
    private JTabbedPane onglets;

    public FenetrePrincipale(String loginMedecin, GestionnaireConnexion connexion, GestionnaireCryptoClient crypto) {
        this.loginMedecin = loginMedecin;
        this.gestionnaireConnexion = connexion;
        this.gestionnaireCrypto = crypto;

        initialiserInterface();
    }

    private void initialiserInterface() {
        setTitle("Serveur Rapport Médical - Connecté en tant que : " + loginMedecin);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750); // Taille confortable
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout());

        // 1. En-tête (Header)
        JPanel panelTitre = creerHeader();
        panelPrincipal.add(panelTitre, BorderLayout.NORTH);

        // 2. Zone Centrale (Onglets)
        onglets = new JTabbedPane();
        onglets.setFont(new Font("Arial", Font.PLAIN, 14));

        /*
         * Instanciation des sous-panels.
         * Chaque panel reçoit les gestionnaires nécessaires pour communiquer avec le
         * serveur.
         */
        onglets.addTab("📝 Ajouter un Rapport", new PanelAjoutRapport(gestionnaireConnexion, gestionnaireCrypto));
        onglets.addTab("✏️ Modifier un Rapport",new PanelModificationRapport(gestionnaireConnexion, gestionnaireCrypto));
        onglets.addTab("📋 Liste des Rapports", new PanelListeRapports(gestionnaireConnexion, gestionnaireCrypto));
        onglets.addTab("👥 Liste des Patients", new PanelListePatients(gestionnaireConnexion, gestionnaireCrypto));

        panelPrincipal.add(onglets, BorderLayout.CENTER);

        // 3. Pied de page (Footer) avec Déconnexion
        JPanel panelBas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton boutonDeconnexion = new JButton("Déconnexion");
        boutonDeconnexion.addActionListener(e -> deconnecter());
        panelBas.add(boutonDeconnexion);
        panelPrincipal.add(panelBas, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    /**
     * Crée le bandeau supérieur avec le titre et le nom du médecin.
     */
    private JPanel creerHeader() {
        JPanel panelTitre = new JPanel(new BorderLayout());
        panelTitre.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelTitre.setBackground(new Color(41, 128, 185)); // Bleu agréable

        JLabel titre = new JLabel("Gestion des Rapports Médicaux");
        titre.setFont(new Font("Arial", Font.BOLD, 22));
        titre.setForeground(Color.WHITE);
        panelTitre.add(titre, BorderLayout.WEST);

        JLabel labelMedecin = new JLabel("Dr. " + loginMedecin);
        labelMedecin.setFont(new Font("Arial", Font.ITALIC, 16));
        labelMedecin.setForeground(Color.WHITE);
        panelTitre.add(labelMedecin, BorderLayout.EAST);

        return panelTitre;
    }

    /**
     * Gère la déconnexion propre de l'utilisateur.
     */
    private void deconnecter() {
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment vous déconnecter ?",
                "Déconnexion",
                JOptionPane.YES_NO_OPTION);

        if (choix == JOptionPane.YES_OPTION) {
            gestionnaireConnexion.deconnecter();

            // Retour à la fenêtre de login
            FenetreLogin fenetreLogin = new FenetreLogin();
            fenetreLogin.setVisible(true);
            this.dispose();
        }
    }
}
