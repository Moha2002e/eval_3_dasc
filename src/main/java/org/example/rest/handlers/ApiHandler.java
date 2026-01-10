package org.example.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.server.bd.BdManager;
import org.example.rest.RestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;


public abstract class ApiHandler implements HttpHandler {

    protected BdManager bdManager;


    public ApiHandler(BdManager bdManager) {
        this.bdManager = bdManager;
    }


    @Override
    public void handle(HttpExchange echange) throws IOException {
        try {
            String methode = echange.getRequestMethod();
            switch (methode) {
                case "GET":
                    gererGet(echange);
                    break;
                case "POST":
                    gererPost(echange);
                    break;
                case "PUT":
                    gererPut(echange);
                    break;
                case "DELETE":
                    gererDelete(echange);
                    break;
                default:
                    RestUtils.envoyerErreur(echange, 405, "Méthode non supportée");
                    break;
            }
        } catch (Exception e) {
            RestUtils.envoyerErreur(echange, 500, "Erreur: " + e.getMessage());
        }
    }


    protected void gererGet(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "GET non supporté");
    }


    protected void gererPost(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "POST non supporté");
    }


    protected void gererPut(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "PUT non supporté");
    }


    protected void gererDelete(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "DELETE non supporté");
    }


    protected Connection obtenirConnexion() {
        return bdManager.getConnection();
    }


    protected Map<String, String> obtenirParametresRequete(HttpExchange echange) {
        String requete = echange.getRequestURI().getQuery();
        return RestUtils.parserRequete(requete);
    }


    protected String lireCorps(HttpExchange echange) throws IOException {
        return RestUtils.lireCorps(echange);
    }


    protected void envoyerJson(HttpExchange echange, int codeStatut, Object donnees) throws IOException {
        RestUtils.envoyerJson(echange, codeStatut, donnees);
    }


    protected void envoyerErreur(HttpExchange echange, int codeStatut, String message) throws IOException {
        RestUtils.envoyerErreur(echange, codeStatut, message);
    }
}

