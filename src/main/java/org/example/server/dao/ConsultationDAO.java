package org.example.server.dao;

import java.sql.*;
import java.util.ArrayList;
import org.example.server.entity.Consultation;
import org.example.server.searchvm.ConsultationSearchVM;


public class ConsultationDAO {

    private Connection connexion;

    public ConsultationDAO(Connection connexion) {
        this.connexion = connexion;
    }


    public boolean medecinAConsultationAvecPatient(int medecinId, int patientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM consultations WHERE doctor_id = ? AND patient_id = ?";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setInt(1, medecinId);
            stmt.setInt(2, patientId);
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


    public ArrayList<Consultation> load(ConsultationSearchVM csearchvm) {
        ArrayList<Consultation> consultations = new ArrayList<>();
        try {
            String query = "SELECT c.*, p.first_name AS p_first_name, p.last_name AS p_last_name, p.birth_date AS p_birth_date "
                    +
                    "FROM consultations c " +
                    "LEFT JOIN patient p ON c.patient_id = p.id " +
                    "LEFT JOIN doctor d ON c.doctor_id = d.id " +
                    "WHERE 1=1 ";

            if (csearchvm != null) {
                if (csearchvm.getPatientName() != null && !csearchvm.getPatientName().isEmpty()) {
                    query += "AND p.last_name LIKE ? ";
                }
                if (csearchvm.getDoctorName() != null && !csearchvm.getDoctorName().isEmpty()) {
                    query += "AND d.last_name LIKE ? ";
                }
                if (csearchvm.getDateFrom() != null && !csearchvm.getDateFrom().isEmpty()) {
                    query += "AND c.date >= ? ";
                }
                if (csearchvm.getDateTo() != null && !csearchvm.getDateTo().isEmpty()) {
                    query += "AND c.date <= ? ";
                }
                if (csearchvm.getReason() != null && !csearchvm.getReason().isEmpty()) {
                    query += "AND c.reason LIKE ? ";
                }
            }

            query += " ORDER BY c.date DESC";

            PreparedStatement ps = connexion.prepareStatement(query);

            if (csearchvm != null) {
                int index = 1;
                if (csearchvm.getPatientName() != null && !csearchvm.getPatientName().isEmpty()) {
                    ps.setString(index++, "%" + csearchvm.getPatientName() + "%");
                }
                if (csearchvm.getDoctorName() != null && !csearchvm.getDoctorName().isEmpty()) {
                    ps.setString(index++, "%" + csearchvm.getDoctorName() + "%");
                }
                if (csearchvm.getDateFrom() != null && !csearchvm.getDateFrom().isEmpty()) {
                    ps.setString(index++, csearchvm.getDateFrom());
                }
                if (csearchvm.getDateTo() != null && !csearchvm.getDateTo().isEmpty()) {
                    ps.setString(index++, csearchvm.getDateTo());
                }
                if (csearchvm.getReason() != null && !csearchvm.getReason().isEmpty()) {
                    ps.setString(index++, "%" + csearchvm.getReason() + "%");
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Consultation consultation = new Consultation();
                consultation.setId(rs.getInt("id"));
                consultation.setDoctor_id(rs.getInt("doctor_id"));


                int patientId = rs.getInt("patient_id");
                if (rs.wasNull()) {
                    consultation.setPatient_id(null);
                } else {
                    consultation.setPatient_id(patientId);
                }

                consultation.setDate(rs.getString("date"));
                consultation.setHour(rs.getString("hour"));
                consultation.setReason(rs.getString("reason"));

                consultation.setPatient_first_name(rs.getString("p_first_name"));
                consultation.setPatient_last_name(rs.getString("p_last_name"));
                consultation.setPatient_birth_date(rs.getString("p_birth_date"));

                consultations.add(consultation);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return consultations;
    }


    public boolean bookConsultation(int consultationId, int patientId, String reason) {

        String sql = "UPDATE consultations SET patient_id = ?, reason = ? WHERE id = ? AND patient_id IS NULL";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            stmt.setString(2, reason);
            stmt.setInt(3, consultationId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean cancelConsultation(int consultationId) {
        String sql = "UPDATE consultations SET patient_id = NULL, reason = NULL WHERE id = ?";
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            stmt.setInt(1, consultationId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
