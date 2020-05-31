package network;


import authorization.AuthService;
import authorization.AuthServiceImpl;
import authorization.AuthToken;
import filters.Filter;
import mappers.EntitiesMapper;
import mappers.JsonEntitiesMapper;
import user.UserInfo;
import vacancies.Catalogue;
import vacancies.VacanciesInfo;
import vacancies.Vacancy;

import javax.security.auth.login.FailedLoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NetworkServiceImpl implements NetworkService {
    private final String APP_KEY = "v3.r.132136870.4ee38e902a0d001e916d40c50ebc65a0462696ec.cd36a27c87d8e690a1884ec256b337763d26d188";
    private final String APP_ID = "1463";
    private final String AUTH_PATH = "https://api.superjob.ru/2.0/oauth2/password/";
    private final String CATALOGUES_PATH = "https://api.superjob.ru/2.0/catalogues";
    private final String VACANCIES_PATH = "https://api.superjob.ru/2.0/vacancies/?";
    private final String CURRENT_USER_PATH = "https://api.superjob.ru/2.0/user/current";

    private EntitiesMapper entityMapper = new JsonEntitiesMapper();
    private AuthService authService = AuthServiceImpl.getInstance();

    public AuthToken getAccessToken(String login, String password) throws FailedLoginException {
        URL url;
        AuthToken authToken;
        try {
            String path = AUTH_PATH
                    + "?login=" + login
                    + "&password=" + password
                    + "&client_id=" + APP_ID
                    + "&client_secret=" + APP_KEY;

            url = new URL(path);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 200) {

                String jsonData = readData(con);
                authToken = entityMapper.extractToken(jsonData);

                return authToken;

            } else {
                System.out.println("Error occurred");
                throw new FailedLoginException();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readData(HttpURLConnection connection) {
        StringBuilder result = new StringBuilder();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Objects.requireNonNull(result).toString();
    }

    @Override
    public List<Catalogue> getCataloguesList() {
        List<Catalogue> catalogues;
        try {
            URL url = new URL(CATALOGUES_PATH);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (con.getResponseCode() == 200) {

                String jsonData = readData(con);
                catalogues = entityMapper.mapCatalogues(jsonData);

                return catalogues;
            } else {
                System.out.println("Error occurred");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Vacancy> getVacanciesList(Map<Filter, String> searchParameters) {
        List<Vacancy> vacancies;
        try {
            StringBuilder path = new StringBuilder(VACANCIES_PATH);
            for (Filter parameter : searchParameters.keySet()) {
                path.append(parameter.getName());
                path.append("=");
                path.append(searchParameters.get(parameter));
                path.append("&");
            }

            HttpURLConnection con = requestGet(path.toString(), null);

            if (con != null && con.getResponseCode() == 200) {
                String jsonData = readData(con);

                VacanciesInfo vacanciesInfo = entityMapper.mapVacanciesInfo(jsonData);
                vacancies = new JsonEntitiesMapper().mapVacancies(vacanciesInfo.getObjects());

                return vacancies;

            } else {
                System.out.println("Error occurred");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpURLConnection requestGet(String path, String authToken) {
        URL url;
        try {
            url = new URL(path);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            con.setRequestProperty("X-Api-App-Id", APP_KEY);

            if (authToken != null) {
                con.setRequestProperty("Authorization", authToken);
            }


            System.out.println("Response code for path " + path.substring(0, path.lastIndexOf('/')) + " " + con.getResponseCode());
            return con;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UserInfo loadUser(long chatId) {
        AuthToken token = authService.getToken(chatId);
        String strToken = token.getTokenType() + " " + token.getAccessToken();

        HttpURLConnection connection = requestGet(CURRENT_USER_PATH, strToken);
        UserInfo userInfo = null;
        try {
            if (connection != null && connection.getResponseCode() == 200) {
                String jsonUser = readData(connection);
                userInfo = entityMapper.mapUserInfo(jsonUser);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userInfo;
    }
}
