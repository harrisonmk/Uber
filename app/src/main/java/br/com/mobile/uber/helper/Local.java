package br.com.mobile.uber.helper;


import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {

    public static float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal) {

        //Local inicial
        Location localInical = new Location("Local inicial");
        localInical.setLatitude(latLngInicial.latitude);
        localInical.setLongitude(latLngInicial.longitude);

        //Local Final
        Location localFinal = new Location("Local final");
        localFinal.setLatitude(latLngFinal.latitude);
        localFinal.setLongitude(latLngFinal.longitude);

        //Calcula automaticamente a distancia inicial com a final
        //Calcula distancia - Resultado em Metros
        //dividir por 1000 para converter em KM
        float distancia = localInical.distanceTo(localFinal) / 1000;


        return distancia;

    }


    public static String formatarDistancia(float distancia) {

        String distanciaFormatada;

        if (distancia < 1) {
            distancia = distancia * 1000; //Em Metros
            distanciaFormatada = Math.round(distancia) + " M "; //Arredonda o valor

        } else {

            DecimalFormat decimal = new DecimalFormat("0.0");
            distanciaFormatada = decimal.format(distancia) + " KM ";

        }

        return distanciaFormatada;
    }

}
