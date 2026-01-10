package org.example.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.DoctorDAO;
import org.example.server.entity.Specialty;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;


public class SpecialtiesHandler extends ApiHandler {


    public SpecialtiesHandler(BdManager bdManager) {
        super(bdManager);
    }


    @Override
    protected void gererGet(HttpExchange echange) throws IOException {
        Connection connexion = obtenirConnexion();
        DoctorDAO daoMedecin = new DoctorDAO(connexion);
        ArrayList<Specialty> specialites = daoMedecin.getAllSpecialties();
        envoyerJson(echange, 200, specialites);
    }
}

