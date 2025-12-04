package org.example.server.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.server.bd.BdManager;
import org.example.server.rest.RestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

/**
 * Classe de base pour tous les handlers REST.
 * Gère le switch sur les méthodes HTTP et fournit des méthodes utilitaires.
 */
public abstract class ApiHandler implements HttpHandler {

    protected BdManager bdManager;

    /**
     * Constructeur.
     * 
     * @param bdManager Le gestionnaire de base de données
     */
    public ApiHandler(BdManager bdManager) {
        this.bdManager = bdManager;
    }

    /**
     * Point d'entrée appelé par HttpServer.
     * Fait un switch sur la méthode HTTP et appelle la bonne méthode.
     * 
     * @param echange L'objet HttpExchange
     * @throws IOException En cas d'erreur
     */
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

    /**
     * Méthode pour les requêtes GET.
     * À redéfinir dans les handlers qui supportent GET.
     * 
     * @param echange L'objet HttpExchange
     * @throws IOException En cas d'erreur
     */
    protected void gererGet(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "GET non supporté");
    }

    /**
     * Méthode pour les requêtes POST.
     * À redéfinir dans les handlers qui supportent POST.
     * 
     * @param echange L'objet HttpExchange
     * @throws IOException En cas d'erreur
     */
    protected void gererPost(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "POST non supporté");
    }

    /**
     * Méthode pour les requêtes PUT.
     * À redéfinir dans les handlers qui supportent PUT.
     * 
     * @param echange L'objet HttpExchange
     * @throws IOException En cas d'erreur
     */
    protected void gererPut(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "PUT non supporté");
    }

    /**
     * Méthode pour les requêtes DELETE.
     * À redéfinir dans les handlers qui supportent DELETE.
     * 
     * @param echange L'objet HttpExchange
     * @throws IOException En cas d'erreur
     */
    protected void gererDelete(HttpExchange echange) throws IOException {
        RestUtils.envoyerErreur(echange, 405, "DELETE non supporté");
    }

    /**
     * Récupère la connexion à la base de données.
     * 
     * @return La connexion JDBC
     */
    protected Connection obtenirConnexion() {
        return bdManager.getConnection();
    }

    /**
     * Parse les paramètres de l'URL (query string).
     * Exemple: ?name=Dupont&specialty=Cardio
     * 
     * @param echange L'objet HttpExchange
     * @return Une Map avec les paramètres
     */
    protected Map<String, String> obtenirParametresRequete(HttpExchange echange) {
        String requete = echange.getRequestURI().getQuery();
        return RestUtils.parserRequete(requete);
    }

    /**
     * Lit le corps de la requête HTTP.
     * 
     * @param echange L'objet HttpExchange
     * @return Le contenu du body
     * @throws IOException En cas d'erreur
     */
    protected String lireCorps(HttpExchange echange) throws IOException {
        return RestUtils.lireCorps(echange);
    }

    /**
     * Envoie une réponse JSON.
     * 
     * @param echange L'objet HttpExchange
     * @param codeStatut Le code HTTP (200, 404, etc.)
     * @param donnees Les données à envoyer en JSON
     * @throws IOException En cas d'erreur
     */
    protected void envoyerJson(HttpExchange echange, int codeStatut, Object donnees) throws IOException {
        RestUtils.envoyerJson(echange, codeStatut, donnees);
    }

    /**
     * Envoie une erreur JSON.
     * 
     * @param echange L'objet HttpExchange
     * @param codeStatut Le code d'erreur HTTP
     * @param message Le message d'erreur
     * @throws IOException En cas d'erreur
     */
    protected void envoyerErreur(HttpExchange echange, int codeStatut, String message) throws IOException {
        RestUtils.envoyerErreur(echange, codeStatut, message);
    }
}

