package org.example.client.ui;

import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;
import org.example.shared.Protocol;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Base64;

/**
 * Panel permettant la création et l'envoi sécurisé de nouveaux rapports
 * médicaux.
 * <p>
 * Fonctionnalités clés :
 * 1. Sélection d'un patient existant.
 * 2. Saisie de la date et du contenu.
 * 3. Chiffrement des données avant envoi (Confidentialité).
 * 4. Signature numérique des données (Authenticité et Non-répudiation).
 * </p>
 */
public class PanelAjoutRapport extends JPanel {

    private final GestionnaireConnexion gestionnaireConnexion;
    private final GestionnaireCryptoClient gestionnaireCrypto;

    // --- Composants UI ---
    private JComboBox<PatientItem> comboPatients;
    private JSpinner spinnerDate;
    private JTextArea zoneTexteRapport;
    private JButton boutonEnregistrer;
    private JButton boutonRafraichir;

    public PanelAjoutRapport(GestionnaireConnexion connexion, GestionnaireCryptoClient crypto) {
        this.gestionnaireConnexion = connexion;
        this.gestionnaireCrypto = crypto;

        initialiserInterface();
    }

    private void initialiserInterface() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Formulaire (Patient + Date)
        JPanel panelFormulaire = creerPanelFormulaire();
        add(panelFormulaire, BorderLayout.NORTH);

        // 2. Zone de Texte (Contenu)
        JPanel panelTexte = new JPanel(new BorderLayout());
        panelTexte.setBorder(BorderFactory.createTitledBorder("Contenu du Rapport Médical"));

        zoneTexteRapport = new JTextArea();
        zoneTexteRapport.setLineWrap(true);
        zoneTexteRapport.setWrapStyleWord(true);
        zoneTexteRapport.setFont(new Font("Arial", Font.PLAIN, 12));

        panelTexte.add(new JScrollPane(zoneTexteRapport), BorderLayout.CENTER);
        add(panelTexte, BorderLayout.CENTER);

        // 3. Bouton Action
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boutonEnregistrer = new JButton("📝 Enregistrer le Rapport");
        boutonEnregistrer.setFont(new Font("Arial", Font.BOLD, 14));
        boutonEnregistrer.addActionListener(e -> traiterEnregistrement());
        panelBoutons.add(boutonEnregistrer);

        add(panelBoutons, BorderLayout.SOUTH);

        // Chargement initial des patients
        chargerPatients();
    }

    private JPanel creerPanelFormulaire() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ligne 1 : Sélection Patient
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        panel.add(new JLabel("Patient:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        comboPatients = new JComboBox<>();
        panel.add(comboPatients, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.1;
        boutonRafraichir = new JButton("🔄");
        boutonRafraichir.setToolTipText("Rafraîchir la liste des patients");
        boutonRafraichir.addActionListener(e -> chargerPatients());
        panel.add(boutonRafraichir, gbc);

        // Ligne 2 : Date
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        panel.add(new JLabel("Date:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.9;
        gbc.gridwidth = 2; // Prend le reste de la largeur

        SpinnerDateModel model = new SpinnerDateModel();
        spinnerDate = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinnerDate, "yyyy-MM-dd");
        spinnerDate.setEditor(editor);
        spinnerDate.setValue(java.sql.Date.valueOf(LocalDate.now())); // Date du jour par défaut

        panel.add(spinnerDate, gbc);

        return panel;
    }

    /**
     * Orchestre le processus d'enregistrement : Validation -> Chiffrement ->
     * Signature -> Envoi.
     */
    private void traiterEnregistrement() {
        // A. Validation des entrées
        PatientItem patientSelectionne = (PatientItem) comboPatients.getSelectedItem();
        if (patientSelectionne == null) {
            afficherErreur("Veuillez sélectionner un patient.");
            return;
        }

        String patientId = String.valueOf(patientSelectionne.getId());
        java.util.Date dateValue = (java.util.Date) spinnerDate.getValue();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(dateValue);
        String texteRapport = zoneTexteRapport.getText().trim();

        if (texteRapport.isEmpty()) {
            afficherErreur("Le contenu du rapport ne peut pas être vide.");
            return;
        }

        // B. Verrouillage UI
        boutonEnregistrer.setEnabled(false);
        boutonEnregistrer.setText("Sécurisation & Envoi...");

        // C. Traitement asynchrone
        new Thread(() -> {
            try {
                // 1. Chiffrement (AES) des données sensibles
                // On chiffres les champs individuellement pour que le serveur puisse les
                // traiter/stocker séparément si besoin
                // Note: Dans une vraie app, on chiffrerait peut-être tout le JSON, mais ici on
                // suit le protocole établi.
                byte[] dateChiffree = gestionnaireCrypto.chiffrer(date);
                byte[] patientIdChiffre = gestionnaireCrypto.chiffrer(patientId);
                byte[] texteChiffre = gestionnaireCrypto.chiffrer(texteRapport);

                // 2. Signature (RSA)
                // On signe la concaténation des données claires pour prouver que c'est bien
                // nous qui avons émis ces infos.
                // Le serveur pourra vérifier la signature avec notre clé publique.
                byte[] donneesASigner = (date + patientId + texteRapport).getBytes();
                byte[] signature = gestionnaireCrypto.signer(donneesASigner);

                // 3. Encodage Base64 pour le transport
                String dateBase64 = Base64.getEncoder().encodeToString(dateChiffree);
                String patientIdBase64 = Base64.getEncoder().encodeToString(patientIdChiffre);
                String texteBase64 = Base64.getEncoder().encodeToString(texteChiffre);
                String signatureBase64 = Base64.getEncoder().encodeToString(signature);

                // 4. Construction de la requête protocolaire
                // Format: ADD_REPORT | DATE_CRYPT | PID_CRYPT | CONTENT_CRYPT | SIGNATURE
                String requete = Protocol.CMD_ADD_REPORT + "|" + dateBase64 + "|" +
                        patientIdBase64 + "|" + texteBase64 + "|" + signatureBase64;

                gestionnaireConnexion.envoyerRequete(requete);
                String reponse = gestionnaireConnexion.recevoirReponse();

                // D. Traitement réponse
                SwingUtilities.invokeLater(() -> {
                    if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                        String[] parties = reponse.split("\\|");
                        String rapportId = (parties.length > 1) ? parties[1] : "?";

                        JOptionPane.showMessageDialog(this,
                                "Rapport enregistré et sécurisé avec succès!\nID Reference: " + rapportId,
                                "Succès", JOptionPane.INFORMATION_MESSAGE);

                        resetFormulaire();
                    } else {
                        String msg = (reponse != null && reponse.startsWith(Protocol.RESP_ERROR))
                                ? reponse.substring(Protocol.RESP_ERROR.length() + 1)
                                : "Réponse inconnue";
                        afficherErreur("Erreur serveur : " + msg);
                    }
                    boutonEnregistrer.setEnabled(true);
                    boutonEnregistrer.setText("📝 Enregistrer le Rapport");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    afficherErreur("Erreur technique : " + ex.getMessage());
                    boutonEnregistrer.setEnabled(true);
                    boutonEnregistrer.setText("📝 Enregistrer le Rapport");
                });
            }
        }).start();
    }

    private void chargerPatients() {
        boutonRafraichir.setEnabled(false);
        comboPatients.removeAllItems();

        new Thread(() -> {
            try {
                // Utilisation constante Protocol
                gestionnaireConnexion.envoyerRequete(Protocol.CMD_LIST_PATIENTS);
                String reponse = gestionnaireConnexion.recevoirReponse();

                SwingUtilities.invokeLater(() -> {
                    if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                        String[] parties = reponse.split("\\|");
                        // Format: OK | ID,Prenom,Nom | ID,Prenom,Nom ...
                        for (int i = 1; i < parties.length; i++) {
                            String[] infos = parties[i].split(",");
                            if (infos.length >= 3) {
                                try {
                                    int id = Integer.parseInt(infos[0]);
                                    comboPatients.addItem(new PatientItem(id, infos[1], infos[2]));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    } else {
                        System.err.println("Erreur chargement patients: " + reponse);
                    }
                    boutonRafraichir.setEnabled(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> boutonRafraichir.setEnabled(true));
            }
        }).start();
    }

    private void resetFormulaire() {
        spinnerDate.setValue(java.sql.Date.valueOf(LocalDate.now()));
        zoneTexteRapport.setText("");
    }

    private void afficherErreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    // --- Inner Class pour l'affichage dans le ComboBox ---
    private static class PatientItem {
        private final int id;
        private final String prenom;
        private final String nom;

        public PatientItem(int id, String prenom, String nom) {
            this.id = id;
            this.prenom = prenom;
            this.nom = nom;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return id + " - " + prenom + " " + nom;
        }
    }
}
