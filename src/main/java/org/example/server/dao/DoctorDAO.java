package org.example.server.dao;

import java.sql.*;
import java.util.ArrayList;
import org.example.server.entity.Doctor;
import org.example.server.entity.Specialty;
import org.example.server.searchvm.DoctorSearchVM;


public class DoctorDAO {

    private Connection connexion;

    public DoctorDAO(Connection connexion) {
        this.connexion = connexion;
    }


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



                doctors.add(doctor);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return doctors;
    }


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
