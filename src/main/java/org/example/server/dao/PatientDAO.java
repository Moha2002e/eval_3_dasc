package org.example.server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.example.server.entity.Patient;
import org.example.server.searchvm.PatientSearchVM;


public class PatientDAO {

    private Connection connexion;

    public PatientDAO(Connection connexion) {
        this.connexion = connexion;
    }


    public List<Patient> listerPatientsAvecConsultations() throws SQLException {
        String sql = "SELECT DISTINCT p.id, p.first_name, p.last_name, p.birth_date " +
                "FROM patient p " +
                "INNER JOIN consultations c ON p.id = c.patient_id " +
                "ORDER BY p.last_name, p.first_name";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement stmt = connexion.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                patients.add(new Patient(
                        rs.getInt("id"),
                        rs.getString("last_name"),
                        rs.getString("first_name"),
                        rs.getString("birth_date")));
            }
        }
        return patients;
    }


    public ArrayList<Patient> load() {
        return load(null);
    }


    public ArrayList<Patient> load(PatientSearchVM psearchvm) {
        ArrayList<Patient> patients = new ArrayList<>();
        try {


            String query = "SELECT DISTINCT p.* FROM patient p ";


            if (psearchvm != null && psearchvm.getDoctorId() != null) {
                query += "INNER JOIN consultations c ON p.id = c.patient_id ";
            }

            query += "WHERE 1=1 ";

            if (psearchvm != null) {
                if (psearchvm.getDoctorId() != null) {
                    query += "AND c.doctor_id = ? ";
                }
                if (psearchvm.getLastName() != null && !psearchvm.getLastName().isEmpty()) {
                    query += "AND p.last_name LIKE ? ";
                }
                if (psearchvm.getFirstName() != null && !psearchvm.getFirstName().isEmpty()) {
                    query += "AND p.first_name LIKE ? ";
                }
                if (psearchvm.getBirthDateFrom() != null && !psearchvm.getBirthDateFrom().isEmpty()) {
                    query += "AND p.birth_date >= ? ";
                }
                if (psearchvm.getBirthDateTo() != null && !psearchvm.getBirthDateTo().isEmpty()) {
                    query += "AND p.birth_date <= ? ";
                }
            }

            query += " ORDER BY p.last_name, p.first_name";

            PreparedStatement ps = connexion.prepareStatement(query);

            if (psearchvm != null) {
                int index = 1;
                if (psearchvm.getDoctorId() != null) {
                    ps.setInt(index++, psearchvm.getDoctorId());
                }
                if (psearchvm.getLastName() != null && !psearchvm.getLastName().isEmpty()) {
                    ps.setString(index++, "%" + psearchvm.getLastName() + "%");
                }
                if (psearchvm.getFirstName() != null && !psearchvm.getFirstName().isEmpty()) {
                    ps.setString(index++, "%" + psearchvm.getFirstName() + "%");
                }
                if (psearchvm.getBirthDateFrom() != null && !psearchvm.getBirthDateFrom().isEmpty()) {
                    ps.setString(index++, psearchvm.getBirthDateFrom());
                }
                if (psearchvm.getBirthDateTo() != null && !psearchvm.getBirthDateTo().isEmpty()) {
                    ps.setString(index++, psearchvm.getBirthDateTo());
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Patient patient = new Patient();
                patient.setId(rs.getInt("id"));
                patient.setLast_name(rs.getString("last_name"));
                patient.setFirst_name(rs.getString("first_name"));
                patient.setBirth_date(rs.getString("birth_date"));

                patients.add(patient);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return patients;
    }


    public int createOrUpdatePatient(Patient p) {
        try {

            String checkSql = "SELECT id FROM patient WHERE last_name = ? AND first_name = ? AND birth_date = ?";
            try (PreparedStatement checkStmt = connexion.prepareStatement(checkSql)) {
                checkStmt.setString(1, p.getLast_name());
                checkStmt.setString(2, p.getFirst_name());
                checkStmt.setString(3, p.getBirth_date());

                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {

                    int idPatient = rs.getInt("id");
                    return idPatient;
                }
            }


            String insertSql = "INSERT INTO patient (last_name, first_name, birth_date) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = connexion.prepareStatement(insertSql,
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, p.getLast_name());
                insertStmt.setString(2, p.getFirst_name());
                insertStmt.setString(3, p.getBirth_date());

                int affectedRows = insertStmt.executeUpdate();
                if (affectedRows > 0) {
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int idGenere = generatedKeys.getInt(1);
                        return idGenere;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public int create(Patient p) {
        try {
            String insertSql = "INSERT INTO patient (last_name, first_name, birth_date) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = connexion.prepareStatement(insertSql,
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, p.getLast_name());
                insertStmt.setString(2, p.getFirst_name());
                insertStmt.setString(3, p.getBirth_date());

                int affectedRows = insertStmt.executeUpdate();
                if (affectedRows > 0) {
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int idGenere = generatedKeys.getInt(1);
                        return idGenere;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public Integer findByDetails(String lastName, String firstName, String birthDate) {
        try {
            String checkSql = "SELECT id FROM patient WHERE last_name = ? AND first_name = ? AND birth_date = ?";
            try (PreparedStatement checkStmt = connexion.prepareStatement(checkSql)) {
                checkStmt.setString(1, lastName);
                checkStmt.setString(2, firstName);
                checkStmt.setString(3, birthDate);

                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int idPatient = rs.getInt("id");
                    return idPatient;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
