package org.example.client.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.client.crypto.GestionnaireCryptoClient;
import org.example.client.reseau.GestionnaireConnexion;
import org.example.shared.Protocol;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Panel visualisant les rapports médicaux.
 * <p>
 * Ce composant illustre la RÉCEPTION sécurisée de données :
 * 1. Téléchargement d'un bloc de données chiffrées (AES) accompagné d'un HMAC.
 * 2. Vérification de l'intégrité via HMAC (Empêche toute modification pendant
 * le transfert).
 * 3. Déchiffrement des données (Confidentialité).
 * 4. Désérialisation JSON et affichage.
 * </p>
 */
public class PanelListeRapports extends JPanel {

    private final GestionnaireConnexion gestionnaireConnexion;
    private final GestionnaireCryptoClient gestionnaireCrypto;

    // --- Composants UI ---
    private JComboBox<PatientItem> comboPatients;
    private JButton boutonCharger;
    private JButton boutonTous;
    private JTable tableRapports;
    private DefaultTableModel modeleTable;
    private JTextArea zoneTexteRapport;

    // Cache des données déchiffrées
    private List<Map<String, Object>> listeRapportsComplets;

    public PanelListeRapports(GestionnaireConnexion connexion, GestionnaireCryptoClient crypto) {
        this.gestionnaireConnexion = connexion;
        this.gestionnaireCrypto = crypto;

        initialiserInterface();
    }

    private void initialiserInterface() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Filtres et Actions
        JPanel panelFiltres = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltres.add(new JLabel("Filtrer par Patient:"));

        comboPatients = new JComboBox<>();
        comboPatients.addItem(new PatientItem(-1, "Tous", "les patients")); // Option par défaut
        panelFiltres.add(comboPatients);

        boutonCharger = new JButton("🔍 Rechercher");
        boutonCharger.addActionListener(e -> actionRechercher());
        panelFiltres.add(boutonCharger);

        boutonTous = new JButton("📋 Voir Tout");
        boutonTous.addActionListener(e -> chargerRapports("")); // Chaine vide = tout
        panelFiltres.add(boutonTous);

        add(panelFiltres, BorderLayout.NORTH);

        // Chargement initial de la liste des patients pour le filtre
        chargerListePatientsPourFiltre();

        // 2. Zone Principale (SplitPane: Tableau en haut, Détail en bas)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6); // 60% pour la table

        // 2a. Tableau
        splitPane.setTopComponent(creerPanelTable());

        // 2b. Zone de lecture seule
        splitPane.setBottomComponent(creerPanelLecture());

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel creerPanelTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Liste des Rapports"));

        String[] colonnes = { "ID", "Patient ID", "Date", "Aperçu" };
        modeleTable = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableRapports = new JTable(modeleTable);
        tableRapports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Au clic, on affiche le détail en bas
        tableRapports.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                afficherDetailRapport();
            }
        });

        panel.add(new JScrollPane(tableRapports), BorderLayout.CENTER);
        return panel;
    }

    private JPanel creerPanelLecture() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Contenu Complet du Rapport (Déchiffré)"));

        zoneTexteRapport = new JTextArea();
        zoneTexteRapport.setLineWrap(true);
        zoneTexteRapport.setWrapStyleWord(true);
        zoneTexteRapport.setFont(new Font("Arial", Font.PLAIN, 12));
        zoneTexteRapport.setEditable(false); // Lecture seule

        panel.add(new JScrollPane(zoneTexteRapport), BorderLayout.CENTER);
        return panel;
    }

    private void actionRechercher() {
        PatientItem selected = (PatientItem) comboPatients.getSelectedItem();
        if (selected != null && selected.getId() != -1) {
            chargerRapports(String.valueOf(selected.getId()));
        } else {
            chargerRapports("");
        }
    }

    /**
     * Charge et DÉCHIFFRE les rapports.
     */
    private void chargerRapports(String patientId) {
        setBoutonsActifs(false);

        new Thread(() -> {
            try {
                String requete;
                if (patientId.isEmpty()) {
                    requete = Protocol.CMD_LIST_REPORTS + "|";
                } else {
                    // Même l'ID dans la requête est chiffré pour la confidentialité de la recherche
                    byte[] patientIdChiffre = gestionnaireCrypto.chiffrer(patientId);
                    String patientIdBase64 = Base64.getEncoder().encodeToString(patientIdChiffre);
                    requete = Protocol.CMD_LIST_REPORTS + "|" + patientIdBase64;
                }

                gestionnaireConnexion.envoyerRequete(requete);
                String reponse = gestionnaireConnexion.recevoirReponse();

                if (reponse.startsWith(Protocol.RESP_OK)) { // le startwith permet de vérifier que la réponse commence par OK
                    traiterReponseRapports(reponse);
                } else {
                    String msg = reponse.startsWith(Protocol.RESP_ERROR)
                            ? reponse.substring(Protocol.RESP_ERROR.length() + 1)
                            : "Erreur inconnue";
                    SwingUtilities.invokeLater(() -> afficherErreurAndUnlock(msg));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> afficherErreurAndUnlock("Erreur: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Traite la réponse cryptée du serveur.
     * C'est ici que se passe la vérification HMAC et le déchiffrement.
     */
    private void traiterReponseRapports(String reponse) throws Exception {
        String[] parties = reponse.split("\\|");

        // Format: OK | Count | JSON_AES_Base64 | HMAC_Base64
        int count = Integer.parseInt(parties[1]);
        String jsonChiffreBase64 = parties[2];
        String hmacBase64 = parties[3];

        byte[] jsonChiffre = Base64.getDecoder().decode(jsonChiffreBase64);
        byte[] hmacRecu = Base64.getDecoder().decode(hmacBase64);

        // 1. VERIFICATION INTEGRITE (HMAC)
        // On vérifie que le bloc chiffré n'a pas été altéré
        if (!gestionnaireCrypto.verifierHMAC(jsonChiffre, hmacRecu)) {
            throw new SecurityException(
                    "ALERTE SECURITE : HMAC invalide ! Les données ont peut-être été altérées en transit.");
        }

        // 2. DECHIFFREMENT (AES)
        String jsonClair = gestionnaireCrypto.dechiffrer(jsonChiffre);

        // 3. PARSING JSON
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> rapports = gson.fromJson(jsonClair, listType);

        this.listeRapportsComplets = rapports;

        // 4. MISE A JOUR UI
        SwingUtilities.invokeLater(() -> {
            modeleTable.setRowCount(0);
            for (Map<String, Object> rapport : rapports) {
                // Le JSON contient des Nombres sous forme de Double par défaut avec Gson
                // générique
                int id = ((Double) rapport.get("id")).intValue();
                int patId = ((Double) rapport.get("patientId")).intValue();
                String date = (String) rapport.get("dateRapport");
                String texte = (String) rapport.get("texteRapport");

                String apercu = (texte.length() > 50) ? texte.substring(0, 50) + "..." : texte;
                modeleTable.addRow(new Object[] { id, patId, date, apercu });
            }
            JOptionPane.showMessageDialog(this, count + " rapport(s) déchiffré(s) avec succès.", "Données Sécurisées",
                    JOptionPane.INFORMATION_MESSAGE);
            setBoutonsActifs(true);
        });
    }

    private void afficherDetailRapport() {
        int ligneSelectionnee = tableRapports.getSelectedRow();
        if (ligneSelectionnee >= 0 && listeRapportsComplets != null) {
            int idSelectionne = (int) modeleTable.getValueAt(ligneSelectionnee, 0);

            // Recherche du texte complet dans notre liste en mémoire
            for (Map<String, Object> rapport : listeRapportsComplets) {
                int id = ((Double) rapport.get("id")).intValue();
                if (id == idSelectionne) {
                    zoneTexteRapport.setText((String) rapport.get("texteRapport"));
                    return; // Trouvé
                }
            }
        }
        zoneTexteRapport.setText("");
    }

    private void chargerListePatientsPourFiltre() {
        new Thread(() -> {
            try {
                gestionnaireConnexion.envoyerRequete(Protocol.CMD_LIST_PATIENTS);
                String reponse = gestionnaireConnexion.recevoirReponse();

                SwingUtilities.invokeLater(() -> {
                    if (reponse != null && reponse.startsWith(Protocol.RESP_OK)) {
                        String[] parties = reponse.split("\\|");
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
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setBoutonsActifs(boolean actif) {
        boutonCharger.setEnabled(actif);
        boutonTous.setEnabled(actif);
    }

    private void afficherErreurAndUnlock(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
        setBoutonsActifs(true);
    }

    // --- Inner Classes ---
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
            return (id == -1) ? prenom + " " + nom : id + " - " + prenom + " " + nom;
        }
    }
}
