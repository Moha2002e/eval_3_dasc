package org.example.client.reseau;

import org.example.shared.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class GestionnaireConnexion {

    private Socket socket;
    private BufferedReader entree;
    private PrintWriter sortie;
    private boolean connecte;


    public boolean connecter(String hote, int port) {
        try {
            socket = new Socket(hote, port);
            entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sortie = new PrintWriter(socket.getOutputStream(), true);
            connecte = true;

            return true;
        } catch (Exception e) {
            System.err.println("✗ Erreur de connexion: " + e.getMessage());
            connecte = false;
            return false;
        }
    }


    public void envoyerRequete(String requete) {
        if (connecte && sortie != null) {
            sortie.println(requete);

        }
    }


    public String recevoirReponse() throws Exception {
        if (connecte && entree != null) {
            String reponse = entree.readLine();

            return reponse;
        }
        return null;
    }


    public void deconnecter() {
        try {
            if (connecte) {
                envoyerRequete(Protocol.CMD_LOGOUT);
                recevoirReponse();
            }
            if (entree != null)
                entree.close();
            if (sortie != null)
                sortie.close();
            if (socket != null)
                socket.close();
            connecte = false;

        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }


    public boolean estConnecte() {
        return connecte;
    }
}
