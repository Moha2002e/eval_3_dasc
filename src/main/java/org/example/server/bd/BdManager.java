package org.example.server.bd;

import org.example.server.dao.ConsultationDAO;
import org.example.server.dao.DoctorDAO;
import org.example.server.dao.PatientDAO;
import org.example.server.dao.ReportDAO;
import org.example.server.entity.Consultation;
import org.example.server.entity.Doctor;
import org.example.server.entity.Patient;
import org.example.server.entity.Report;
import org.example.server.searchvm.ConsultationSearchVM;
import org.example.server.searchvm.DoctorSearchVM;
import org.example.server.searchvm.PatientSearchVM;
import org.example.server.searchvm.ReportSearchVM;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;


public class BdManager {
    private Properties properties;
    private Connection connexion;


    private DoctorDAO doctorDAO;
    private PatientDAO patientDAO;
    private ConsultationDAO consultationDAO;
    private ReportDAO reportDAO;

    public BdManager() {
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Erreur : le fichier config.properties n'a pas été trouvé");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            System.out.println("Erreur : " + ex.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }


    public void connecter() throws SQLException {
        String url = getProperty("DB_URL");
        String user = getProperty("DB_USER");
        String password = getProperty("DB_PASSWORD");
        connexion = DriverManager.getConnection(url, user, password);


        doctorDAO = new DoctorDAO(connexion);
        patientDAO = new PatientDAO(connexion);
        consultationDAO = new ConsultationDAO(connexion);
        reportDAO = new ReportDAO(connexion);
    }


    public void deconnecter() throws SQLException {
        if (connexion != null && !connexion.isClosed()) {
            connexion.close();
        }
    }


    public Connection getConnection() {
        return connexion;
    }




    public String getMotDePasseMedecin(String login) throws SQLException {
        return doctorDAO.getMotDePasseMedecin(login);
    }


    public boolean medecinExiste(String login) throws SQLException {
        return doctorDAO.medecinExiste(login);
    }


    public Integer getIdMedecin(String login) throws SQLException {
        return doctorDAO.getIdMedecin(login);
    }


    public List<Doctor> loadDoctors(DoctorSearchVM dsearchvm) throws SQLException {
        return doctorDAO.load(dsearchvm);
    }




    public boolean medecinAConsultationAvecPatient(int medecinId, int patientId) throws SQLException {
        return consultationDAO.medecinAConsultationAvecPatient(medecinId, patientId);
    }


    public List<Consultation> loadConsultations(ConsultationSearchVM csearchvm) throws SQLException {
        return consultationDAO.load(csearchvm);
    }




    public List<Patient> listerTousPatients() throws SQLException {
        return patientDAO.load();
    }


    public List<Patient> load(PatientSearchVM psearchvm) throws SQLException {
        return patientDAO.load(psearchvm);
    }




    public int ajouterRapport(int medecinId, int patientId, String date, String texteRapport) throws SQLException {
        return reportDAO.ajouterRapport(medecinId, patientId, date, texteRapport);
    }


    public boolean modifierRapport(int rapportId, String nouveauTexte, int medecinId) throws SQLException {
        return reportDAO.modifierRapport(rapportId, nouveauTexte, medecinId);
    }


    public List<ReportDAO.Rapport> listerRapportsMedecin(int medecinId) throws SQLException {
        return reportDAO.listerRapportsMedecin(medecinId);
    }


    public List<ReportDAO.Rapport> listerRapportsMedecinPatient(int medecinId, int patientId) throws SQLException {
        return reportDAO.listerRapportsMedecinPatient(medecinId, patientId);
    }


    public List<Report> loadReports(ReportSearchVM rsearchvm) throws SQLException {
        return reportDAO.load(rsearchvm);
    }




    @Deprecated
    public static class Rapport extends ReportDAO.Rapport {
        public Rapport(int id, int medecinId, int patientId, Date dateRapport, String texteRapport) {
            super(id, medecinId, patientId, dateRapport, texteRapport);
        }
    }

    public static void main(String[] args) {
        BdManager manager = new BdManager();
        try {
            manager.connecter();
            System.out.println("✓ Connexion réussie à la base de données.");
            manager.deconnecter();
            System.out.println("✓ Déconnexion réussie de la base de données.");
        } catch (SQLException e) {
            System.err.println("✗ Erreur de connexion: " + e.getMessage());
        }
    }
}
