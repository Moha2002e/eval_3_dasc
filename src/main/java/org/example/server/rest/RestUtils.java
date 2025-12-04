package org.example.server.rest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaires pour le serveur REST.
 * Méthodes pour parser les requêtes et envoyer les réponses.
 */
public class RestUtils {

    private static final Gson gson = new Gson();

    /**
     * Parse la query string de l'URL.
     * Exemple: ?name=Dupont&specialty=Cardio
     * 
     * @param requete La query string à parser
     * @return Une Map avec les paramètres
     */
    public static Map<String, String> parserRequete(String requete) {
        Map<String, String> parametres = new HashMap<>();
        if (requete == null || requete.isEmpty()) {
            return parametres;
        }

        String[] paires = requete.split("&");
        for (String paire : paires) {
            String[] cleValeur = paire.split("=", 2);
            if (cleValeur.length == 2) {
                try {
                    String cle = URLDecoder.decode(cleValeur[0], StandardCharsets.UTF_8);
                    String valeur = URLDecoder.decode(cleValeur[1], StandardCharsets.UTF_8);
                    parametres.put(cle, valeur);
                } catch (Exception e) {
                    // Ignorer les erreurs
                }
            }
        }
        return parametres;
    }

    /**
     * Lit le corps de la requête HTTP.
     * 
     * @param echange L'objet HttpExchange
     * @return Le contenu du body
     * @throws IOException En cas d'erreur
     */
    public static String lireCorps(HttpExchange echange) throws IOException {
        BufferedReader lecteur = new BufferedReader(
            new InputStreamReader(echange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder corps = new StringBuilder();
        String ligne;
        while ((ligne = lecteur.readLine()) != null) {
            corps.append(ligne);
        }
        return corps.toString();
    }

    /**
     * Parse le body en format form-urlencoded.
     * Exemple: nom=Dupont&prenom=Jean
     * 
     * @param corps Le body à parser
     * @return Une Map avec les données
     */
    public static Map<String, String> parserFormulaire(String corps) {
        return parserRequete(corps);
    }

    /**
     * Parse le body en format JSON.
     * 
     * @param corps Le body JSON à parser
     * @param classe La classe cible
     * @return L'objet parsé
     */
    public static <T> T parserJson(String corps, Class<T> classe) {
        return gson.fromJson(corps, classe);
    }

    /**
     * Envoie une réponse JSON au client.
     * 
     * @param echange L'objet HttpExchange
     * @param code Le code HTTP (200, 404, etc.)
     * @param donnees Les données à envoyer
     * @throws IOException En cas d'erreur
     */
    public static void envoyerJson(HttpExchange echange, int code, Object donnees) throws IOException {
        String json = gson.toJson(donnees);
        echange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] octets = json.getBytes(StandardCharsets.UTF_8);
        echange.sendResponseHeaders(code, octets.length);
        OutputStream fluxSortie = echange.getResponseBody();
        fluxSortie.write(octets);
        fluxSortie.close();
    }

    /**
     * Envoie une erreur JSON au client.
     * 
     * @param echange L'objet HttpExchange
     * @param code Le code d'erreur HTTP
     * @param message Le message d'erreur
     * @throws IOException En cas d'erreur
     */
    public static void envoyerErreur(HttpExchange echange, int code, String message) throws IOException {
        Map<String, String> erreur = new HashMap<>();
        erreur.put("error", message);
        envoyerJson(echange, code, erreur);
    }
}
