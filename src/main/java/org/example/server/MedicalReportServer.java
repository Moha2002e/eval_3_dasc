package org.example.server;

import org.example.server.bd.BdManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MedicalReportServer {
    private final int port;
    private final int taillePoolThreads;
    private final ExecutorService poolThreads;
    private final BdManager gestionnaireBd;
    private volatile boolean enCours;


    public MedicalReportServer(int port, int taillePoolThreads) {
        this.port = port;
        this.taillePoolThreads = taillePoolThreads;
        //on prépare un nb max de thread, pour pas que notre serveur plante
        this.poolThreads = Executors.newFixedThreadPool(taillePoolThreads);
        this.gestionnaireBd = new BdManager();
        this.enCours = false;
    }


    public void demarrer() {
        enCours = true;
        try {
            gestionnaireBd.connecter();
        } catch (SQLException e) {
            System.err.println("✗ Impossible de se connecter à la base de données: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try (ServerSocket socketServeur = new ServerSocket(port)) {

            while (enCours) {//on attend juste la connexion
                try {
                    //on attend qu'un client se connecte
                    Socket socketClient = socketServeur.accept();
                    //c'est ici le thread à la demande, on donne le traitement du client au pool de thread
                    poolThreads.submit(new ClientHandler(socketClient, gestionnaireBd));
                } catch (IOException e) {
                    if (enCours) {
                        System.err.println("Erreur lors de l'acceptation de la connexion client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            arreter();
        }
    }


    public void arreter() {
        enCours = false;
        //on libere le thread lorsque le serveur n'en a plus besoin
        poolThreads.shutdown();
        try {
            if (gestionnaireBd != null) {
                gestionnaireBd.deconnecter();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la déconnexion de la BD: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        BdManager chargeurConfig = new BdManager();
        int port = Integer.parseInt(chargeurConfig.getProperty("PORT_REPORT_SECURE"));
        int taillePoolThreads = Integer.parseInt(chargeurConfig.getProperty("THREAD_POOL_SIZE"));


        MedicalReportServer serveur = new MedicalReportServer(port, taillePoolThreads);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serveur.arreter();
        }));

        serveur.demarrer();
    }
}
