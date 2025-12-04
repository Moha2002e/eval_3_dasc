package org.example.server.dao;

import org.example.server.entity.Specialty;
import java.sql.*;
import java.util.ArrayList;

public class SpecialtyDAO {

    private Connection connexion;

    public SpecialtyDAO(Connection connexion) {
        this.connexion = connexion;
    }

    // Méthode simple pour récupérer toutes les spécialités
    // Méthode de recherche avec critères (SearchVM)
    public ArrayList<Specialty> load(org.example.server.searchvm.SpecialtySearchVM vm) {
        ArrayList<Specialty> liste = new ArrayList<>();

        try {
            String sql = "SELECT * FROM specialties WHERE 1=1";

            // Ajout des filtres si le VM n'est pas null
            if (vm != null) {
                if (vm.getName() != null && !vm.getName().isEmpty()) {
                    sql += " AND name LIKE ?";
                }
            }

            PreparedStatement ps = connexion.prepareStatement(sql);

            // Remplissage des paramètres
            if (vm != null) {
                int index = 1;
                if (vm.getName() != null && !vm.getName().isEmpty()) {
                    ps.setString(index++, "%" + vm.getName() + "%");
                }
            }

            ResultSet resultat = ps.executeQuery();

            while (resultat.next()) {
                int id = resultat.getInt("id");
                String nom = resultat.getString("name");
                liste.add(new Specialty(id, nom));
            }

            resultat.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return liste;
    }
}
