package org.example.rest.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.example.server.bd.BdManager;
import org.example.server.dao.DoctorDAO;
import org.example.server.entity.Doctor;
import org.example.server.searchvm.DoctorSearchVM;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;


public class DoctorsHandler extends ApiHandler {


    public DoctorsHandler(BdManager bdManager) {
        super(bdManager);
    }


    @Override
    protected void gererGet(HttpExchange echange) throws IOException {
        Map<String, String> parametres = obtenirParametresRequete(echange);
        DoctorSearchVM vm = new DoctorSearchVM();

        if (parametres.containsKey("name")) {
            vm.setLastName(parametres.get("name"));
        }
        if (parametres.containsKey("specialty")) {
            vm.setSpecialityName(parametres.get("specialty"));
        }

        Connection connexion = obtenirConnexion();
        DoctorDAO daoMedecin = new DoctorDAO(connexion);
        ArrayList<Doctor> medecins = daoMedecin.load(vm);
        envoyerJson(echange, 200, medecins);
    }
}

