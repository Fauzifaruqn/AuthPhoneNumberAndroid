package com.example.applikasihawker.Utils;

import com.example.applikasihawker.Retrofit.IDHawkerAPI;
import com.example.applikasihawker.Retrofit.RetrofitClient;

public class Common {
    private static final String BASE_URL = "http://10.0.2.2/hawker/";

    public static IDHawkerAPI getAPI()
    {
        return RetrofitClient.getClient(BASE_URL).create(IDHawkerAPI.class);
    }
}
