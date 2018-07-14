package com.ermacaz.imagebackup.data.remote;

public class ApiUtils {
    private ApiUtils() {}

    public static final String BASE_URL = "http:/7.7.7.6:3000/";

    public static APIService getAPIService() {

        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }
}
