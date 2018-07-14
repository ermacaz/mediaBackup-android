package com.ermacaz.imagebackup.data.remote;
import com.ermacaz.imagebackup.data.model.ImageUpload;
import com.ermacaz.imagebackup.data.model.UserAuthentication;
import com.ermacaz.imagebackup.data.model.UserAuthenticationResponse;

import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @POST("/documents.json")
    Call<ResponseBody> saveDocument(@Body ImageUpload body);

    @Headers("Content-Type: application/json")
    @POST("/login.json")
    Call<UserAuthenticationResponse> authenticate(@Body UserAuthentication body);
}
