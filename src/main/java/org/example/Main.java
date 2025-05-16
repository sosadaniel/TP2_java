package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/trabajo2";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            for (int code = 100; code <= 200; code++) {
                String formattedCode = String.format("%03d", code);
                String url = "https://restcountries.com/v3.1/alpha/" + formattedCode;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


                if (response.statusCode() != 200) {
                    System.out.println(response);
                    continue;
                }

                JSONArray jsonArray = new JSONArray(response.body());
                System.out.println("response: " + response + "json: "+  jsonArray);
                if (jsonArray.isEmpty()) continue;

                JSONObject country = jsonArray.getJSONObject(0);

                String codigoPais = country.optString("ccn3");
                String nombrePais = country.getJSONObject("name").optString("common", "");
                JSONArray capitalArray = country.optJSONArray("capital");
                String capitalPais = (capitalArray != null && !capitalArray.isEmpty()) ? capitalArray.getString(0) : "";
                String region = country.optString("region", "");
                String subregion = country.optString("subregion", "");
                long poblacion = country.optLong("population", 0);
                JSONArray latlng = country.optJSONArray("latlng");
                double latitud = (latlng != null && latlng.length() > 0) ? latlng.getDouble(0) : 0.0;
                double longitud = (latlng != null && latlng.length() > 1) ? latlng.getDouble(1) : 0.0;

                PreparedStatement selectStmt = conn.prepareStatement("SELECT COUNT(*) FROM paises WHERE codigoPais = ?");
                selectStmt.setString(1, codigoPais);
                ResultSet rs = selectStmt.executeQuery();
                rs.next();
                boolean existePais = rs.getInt(1) > 0;

                if (existePais) {
                    PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE paises SET nombrePais = ?, capitalPais = ?, region = ?, subregion = ?, poblacion = ?, latitud = ?, longitud = ? WHERE codigoPais = ?");
                    updateStmt.setString(1, nombrePais);
                    updateStmt.setString(2, capitalPais);
                    updateStmt.setString(3, region);
                    updateStmt.setString(4, subregion);
                    updateStmt.setLong(5, poblacion);
                    updateStmt.setDouble(6, latitud);
                    updateStmt.setDouble(7, longitud);
                    updateStmt.setString(8, codigoPais);
                    updateStmt.executeUpdate();
                } else {
                    PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO paises (codigoPais, nombrePais, capitalPais, region, subregion, poblacion, latitud, longitud) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                    insertStmt.setString(1, codigoPais);
                    insertStmt.setString(2, nombrePais);
                    insertStmt.setString(3, capitalPais);
                    insertStmt.setString(4, region);
                    insertStmt.setString(5, subregion);
                    insertStmt.setLong(6, poblacion);
                    insertStmt.setDouble(7, latitud);
                    insertStmt.setDouble(8, longitud);
                    insertStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}