package org.example.client.ui;

import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;
import org.example.shared.Protocol;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel d'affichage de la liste des patients.
 * <p>
 * Récupère la liste des patients depuis le serveur et l'affiche dans un
 * tableau.
 * Cette vue est en lecture seule (pas de crypto complexe ici, c'est de l'info
 * publique ou non chiffrée selon ce protocole spécifique pour les noms).
 * </p>
 */
public class PanelListePatients extends JPanel {

    private final GestionnaireConnexion gestionnaireConnexion;
    // Crypto disponible si besoin d'extensions futures (ex: déchiffrer les noms)
    private final GestionnaireCryptoClient gestionnaireCrypto;

    // --- Composants UI ---
    private JTable tablePatients;
    private DefaultTableModel modeleTable;
    private JButton boutonCharger;

    public PanelListePatients(GestionnaireConnexion connexion, GestionnaireCryptoClient crypto) {
        this.gestionnaireConnexion = connexion;
        this.gestionnaireCrypto = crypto;

        initialiserInterface();
    }

    private void initialiserInterface() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Barre d'outils supérieure
        JPanel panelHaut = new JPanel(new BorderLayout());

        JLabel titre = new JLabel("Annuaire des Patients");
        titre.setFont(new Font("Arial", Font.BOLD, 18));
        panelHaut.add(titre, BorderLayout.WEST);

        boutonCharger = new JButton("🔄 Actualiser la liste");
        boutonCharger.addActionListener(e -> chargerPatients());
        panelHaut.add(boutonCharger, BorderLayout.EAST);

        add(panelHaut, BorderLayout.NORTH);

        // 2. Tableau des données
        String[] colonnes = { "ID", "Prénom", "Nom", "Date de Naissance" };
        modeleTable = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table en lecture seule
            }
        };

        tablePatients = new JTable(modeleTable);
        tablePatients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePatients.setFont(new Font("Arial", Font.PLAIN, 12));
        tablePatients.setRowHeight(25);
        tablePatients.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Configuration des largeurs de colonnes
        tablePatients.getColumnModel().getColumn(0).setPreferredWidth(50); // ID petit
        tablePatients.getColumnModel().getColumn(1).setPreferredWidth(150);
        tablePatients.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablePatients.getColumnModel().getColumn(3).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(tablePatients);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Pied de page
        JPanel panelBas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel labelInfo = new JLabel("ℹ️ Les données sont récupérées en temps réel depuis le serveur.");
        labelInfo.setForeground(Color.GRAY);
        panelBas.add(labelInfo);
        add(panelBas, BorderLayout.SOUTH);
    }

    private void chargerPatients() {
        boutonCharger.setEnabled(false);
        boutonCharger.setText("Chargement...");

        new Thread(() -> {
            try {
                // Envoi commande
                gestionnaireConnexion.envoyerRequete(Protocol.CMD_LIST_PATIENTS);
                String reponse = gestionnaireConnexion.recevoirReponse();

                // Traitement UI
                SwingUtilities.invokeLater(() -> {
                    if (reponse == null) {
                        afficherErreur("Aucune réponse du serveur.");
                    } else if (reponse.startsWith(Protocol.RESP_OK)) {
                        miseAJourTableau(reponse);
                    } else {
                        String msg = reponse.startsWith(Protocol.RESP_ERROR)
                                ? reponse.substring(Protocol.RESP_ERROR.length() + 1)
                                : reponse;
                        afficherErreur("Erreur serveur : " + msg);
                    }
                    // Reset bouton
                    boutonCharger.setEnabled(true);
                    boutonCharger.setText("🔄 Actualiser la liste");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Erreur technique : " + ex.getMessage());
                    boutonCharger.setEnabled(true);
                    boutonCharger.setText("🔄 Actualiser la liste");
                });
            }
        }).start();
    }

    private void miseAJourTableau(String reponse) {
        modeleTable.setRowCount(0); // Vider la table

        String[] parties = reponse.split("\\|");

        // Format reponse : OK | ID,Pre,Nom,Date | ID,Pre,Nom,Date ...
        int compteur = 0;
        for (int i = 1; i < parties.length; i++) {
            String[] patient = parties[i].split(",");
            if (patient.length >= 4) {
                modeleTable.addRow(new Object[] {
                        patient[0], // ID
                        patient[1], // Prénom
                        patient[2], // Nom
                        patient[3] // Date Naissance
                });
                compteur++;
            }
        }

        JOptionPane.showMessageDialog(this, compteur + " patient(s) chargé(s).", "Succès",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherErreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
