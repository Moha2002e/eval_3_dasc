package org.example.client.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;
import org.example.shared.Protocol;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Panel permettant de modifier un rapport existant.
 * <p>
 * Combine la lecture (récupération de la liste) et l'écriture (envoi de
 * modifications).
 * Les modifications sont également chiffrées avant d'être envoyées.
 * </p>
 */
public class PanelModificationRapport extends JPanel {

    private final GestionnaireConnexion gestionnaireConnexion;
    private final GestionnaireCryptoClient gestionnaireCrypto;

    // --- Composants UI ---
    private JComboBox<ReportItem> comboRapports;
    private JTextArea zoneTexteRapport;
    private JButton boutonModifier;
    private JButton boutonCharger;

    public PanelModificationRapport(GestionnaireConnexion connexion, GestionnaireCryptoClient crypto) {
        this.gestionnaireConnexion = connexion;
        this.gestionnaireCrypto = crypto;

        initialiserInterface();
    }

    private void initialiserInterface() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Sélection du Rapport
        JPanel panelId = creerPanelSelection();
        add(panelId, BorderLayout.NORTH);

        // 2. Zone d'édition
        JPanel panelTexte = new JPanel(new BorderLayout());
        panelTexte.setBorder(BorderFactory.createTitledBorder("Modification du Contenu"));

        zoneTexteRapport = new JTextArea();
        zoneTexteRapport.setLineWrap(true);
        zoneTexteRapport.setWrapStyleWord(true);
        zoneTexteRapport.setFont(new Font("Arial", Font.PLAIN, 12));

        panelTexte.add(new JScrollPane(zoneTexteRapport), BorderLayout.CENTER);
        add(panelTexte, BorderLayout.CENTER);

        // 3. Boutons d'action
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boutonModifier = new JButton("✏️ Sauvegarder les modifications");
        boutonModifier.setFont(new Font("Arial", Font.BOLD, 14));
        boutonModifier.addActionListener(e -> traiterModification());
        panelBoutons.add(boutonModifier);

        add(panelBoutons, BorderLayout.SOUTH);

        // Chargement initial
        chargerRapports();
    }

    private JPanel creerPanelSelection() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.2;
        panel.add(new JLabel("Sélectionner un Rapport :"), gbc);

        // ComboBox
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        comboRapports = new JComboBox<>();
        comboRapports.addActionListener(e -> afficherContenuSelectionne());
        panel.add(comboRapports, gbc);

        // Bouton Refresh
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        boutonCharger = new JButton("🔄");
        boutonCharger.setToolTipText("Recharger la liste des rapports depuis le serveur");
        boutonCharger.addActionListener(e -> chargerRapports());
        panel.add(boutonCharger, gbc);

        return panel;
    }

    /**
     * Charge la liste des rapports disponibles pour modification.
     * (Similaire à PanelListeRapports, doit déchiffrer les données pour les
     * afficher dans la liste déroulante)
     */
    private void chargerRapports() {
        boutonCharger.setEnabled(false);
        comboRapports.removeAllItems();
        zoneTexteRapport.setText("");

        new Thread(() -> {
            try {
                // Demande de tous les rapports
                gestionnaireConnexion.envoyerRequete(Protocol.CMD_LIST_REPORTS + "|");
                String reponse = gestionnaireConnexion.recevoirReponse();

                if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                    // Protocol: OK | Count | JSON_AES | HMAC
                    String[] parties = reponse.split("\\|");

                    if (parties.length >= 4) {
                        String jsonChiffreBase64 = parties[2];
                        String hmacBase64 = parties[3];

                        byte[] jsonChiffre = Base64.getDecoder().decode(jsonChiffreBase64);
                        byte[] hmac = Base64.getDecoder().decode(hmacBase64);

                        // Vérif HMAC
                        if (gestionnaireCrypto.verifierHMAC(jsonChiffre, hmac)) {
                            // Déchiffrement
                            String json = gestionnaireCrypto.dechiffrer(jsonChiffre);

                            // Parsing
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Map<String, Object>>>() {
                            }.getType();
                            List<Map<String, Object>> rapports = gson.fromJson(json, listType);

                            // Update UI
                            SwingUtilities.invokeLater(() -> {
                                for (Map<String, Object> rapport : rapports) {
                                    int id = ((Double) rapport.get("id")).intValue();
                                    String date = (String) rapport.get("dateRapport");
                                    String texte = (String) rapport.get("texteRapport");
                                    comboRapports.addItem(new ReportItem(id, date, texte));
                                }
                                boutonCharger.setEnabled(true);
                            });
                        }
                    }
                } else {
                    SwingUtilities.invokeLater(() -> boutonCharger.setEnabled(true));
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> boutonCharger.setEnabled(true));
            }
        }).start();
    }

    /**
     * Envoie la commande de modification au serveur.
     */
    private void traiterModification() {
        ReportItem rapportSelectionne = (ReportItem) comboRapports.getSelectedItem();
        if (rapportSelectionne == null) {
            afficherErreur("Veuillez sélectionner un rapport à modifier.");
            return;
        }

        String rapportId = String.valueOf(rapportSelectionne.getId());
        String nouveauTexte = zoneTexteRapport.getText().trim();

        if (nouveauTexte.isEmpty()) {
            afficherErreur("Le texte du rapport ne peut pas être vide.");
            return;
        }

        boutonModifier.setEnabled(false);
        boutonModifier.setText("Chiffrement & Envoi...");

        new Thread(() -> {
            try {
                // 1. Chiffrement
                byte[] rapportIdChiffre = gestionnaireCrypto.chiffrer(rapportId);
                byte[] texteChiffre = gestionnaireCrypto.chiffrer(nouveauTexte);

                String rapportIdBase64 = Base64.getEncoder().encodeToString(rapportIdChiffre);
                String texteBase64 = Base64.getEncoder().encodeToString(texteChiffre);

                // 2. Construction Requête
                // Protocol: EDIT_REPORT | ID_CRYPT | CONTENT_CRYPT
                String requete = Protocol.CMD_EDIT_REPORT + "|" + rapportIdBase64 + "|" + texteBase64;

                gestionnaireConnexion.envoyerRequete(requete);
                String reponse = gestionnaireConnexion.recevoirReponse();

                // 3. Feedback UI
                SwingUtilities.invokeLater(() -> {
                    if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                        JOptionPane.showMessageDialog(this, "Rapport modifié avec succès !", "Succès",
                                JOptionPane.INFORMATION_MESSAGE);
                        zoneTexteRapport.setText("");
                        chargerRapports(); // Recharger la liste pour voir les changements si nécessaire
                    } else {
                        String msg = (reponse != null && reponse.startsWith(Protocol.RESP_ERROR))
                                ? reponse.substring(Protocol.RESP_ERROR.length() + 1)
                                : "Erreur inconnue";
                        afficherErreur("Echec modification : " + msg);
                    }
                    boutonModifier.setEnabled(true);
                    boutonModifier.setText("✏️ Sauvegarder les modifications");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Erreur technique : " + ex.getMessage());
                    boutonModifier.setEnabled(true);
                    boutonModifier.setText("✏️ Sauvegarder les modifications");
                });
            }
        }).start();
    }

    private void afficherContenuSelectionne() {
        ReportItem item = (ReportItem) comboRapports.getSelectedItem();
        if (item != null) {
            zoneTexteRapport.setText(item.getTexte());
        } else {
            zoneTexteRapport.setText("");
        }
    }

    private void afficherErreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    // --- Inner Class ---
    private static class ReportItem {
        private final int id;
        private final String date;
        private final String texte;

        public ReportItem(int id, String date, String texte) {
            this.id = id;
            this.date = date;
            this.texte = texte;
        }

        public int getId() {
            return id;
        }

        public String getTexte() {
            return texte;
        }

        @Override
        public String toString() {
            return "Rapport #" + id + " (" + date + ")";
        }
    }
}
