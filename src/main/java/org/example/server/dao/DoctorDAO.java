package org.example.server.dao;

import java.sql.*;
import java.util.ArrayList;
import org.example.server.entity.Doctor;
import org.example.server.entity.Specialty;
import org.example.server.searchvm.DoctorSearchVM;

/**
 * DAO (Data Access Object) pour la gestion des entités {@link Doctor}.
 * Fournit des méthodes pour l'authentification et la recherche de médecins.
 */
public class DoctorDAO {

    private Connection connexion;

    public DoctorDAO(Connection connexion) {
        this.connexion = connexion;
    }

    /**
     * Vérifie si un médecin existe dans la base de données à partir de son login.
     * Le login est formé par la concaténation "prenom.nom".
     *
     * @param login Le login du médecin (format: prenom.nom)
     * @return true si le médecin existe, false sinon
     * @throws SQLException En cas d'erreur d'accès à la base de données
     */
    public boolean medecinExiste(String login) throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctor WHERE CONCAT(first_name, '.', last_name) = ?";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int nombre = rs.getInt(1);
                if (nombre > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Récupère le mot de passe haché d'un médecin à partir de son login.
     *
     * @param login Le login du médecin
     * @return Le mot de passe haché, ou null si le médecin n'est pas trouvé
     * @throws SQLException En cas d'erreur d'accès à la base de données
     */
    public String getMotDePasseMedecin(String login) throws SQLException {
        String sql = "SELECT password FROM doctor WHERE CONCAT(first_name, '.', last_name) = ?";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String motDePasse = rs.getString("password");
                return motDePasse;
            }
        }
        return null;
    }

    /**
     * Récupère l'identifiant unique d'un médecin à partir de son login.
     *
     * @param login Le login du médecin
     * @return L'ID du médecin, ou null si non trouvé
     * @throws SQLException En cas d'erreur d'accès à la base de données
     */
    public Integer getIdMedecin(String login) throws SQLException {
        String sql = "SELECT id FROM doctor WHERE CONCAT(first_name, '.', last_name) = ?";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int idMedecin = rs.getInt("id");
                return idMedecin;
            }
        }
        return null;
    }

    /**
     * Recherche des médecins en fonction de critères dynamiques.
     *
     * @param dsearchvm L'objet contenant les critères de recherche (nom, prénom,
     *                  spécialité)
     * @return Une liste de médecins correspondant aux critères
     */
    public ArrayList<Doctor> load(DoctorSearchVM dsearchvm) {
        ArrayList<Doctor> doctors = new ArrayList<>();
        try {
            String query = "SELECT d.* FROM doctor d " +
                    "LEFT JOIN specialties s ON d.specialite_id = s.id " +
                    "WHERE 1=1 ";

            if (dsearchvm != null) {
                if (dsearchvm.getLastName() != null && !dsearchvm.getLastName().isEmpty()) {
                    query += "AND d.last_name LIKE ? ";
                }
                if (dsearchvm.getFirstName() != null && !dsearchvm.getFirstName().isEmpty()) {
                    query += "AND d.first_name LIKE ? ";
                }
                if (dsearchvm.getSpecialityName() != null && !dsearchvm.getSpecialityName().isEmpty()) {
                    query += "AND s.name LIKE ? ";
                }
            }

            query += " ORDER BY d.last_name, d.first_name";

            PreparedStatement ps = connexion.prepareStatement(query);

            if (dsearchvm != null) {
                int index = 1;
                if (dsearchvm.getLastName() != null && !dsearchvm.getLastName().isEmpty()) {
                    ps.setString(index++, "%" + dsearchvm.getLastName() + "%");
                }
                if (dsearchvm.getFirstName() != null && !dsearchvm.getFirstName().isEmpty()) {
                    ps.setString(index++, "%" + dsearchvm.getFirstName() + "%");
                }
                if (dsearchvm.getSpecialityName() != null && !dsearchvm.getSpecialityName().isEmpty()) {
                    ps.setString(index++, "%" + dsearchvm.getSpecialityName() + "%");
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Doctor doctor = new Doctor();
                doctor.setId(rs.getInt("id"));
                doctor.setSpecialite_id(rs.getInt("specialite_id"));
                doctor.setLast_name(rs.getString("last_name"));
                doctor.setFirst_name(rs.getString("first_name"));
                // doctor.setPassword(rs.getString("password")); // Optional, depending on
                // security requirements

                doctors.add(doctor);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return doctors;
    }

    /**
     * Récupère toutes les spécialités disponibles dans la base de données.
     * 
     * @return Une liste de toutes les spécialités
     */
    public ArrayList<Specialty> getAllSpecialties() {
        ArrayList<Specialty> specialties = new ArrayList<>();
        try {
            String sql = "SELECT * FROM specialties ORDER BY name";
            PreparedStatement stmt = connexion.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                specialties.add(new Specialty(id, name));
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return specialties;
    }
}
