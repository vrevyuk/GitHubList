package com.revyuk.githublist;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Vitaly Revyuk on 13.11.2014.
 */
public class JSONLoader extends AsyncTask<String, Void, Object> {
    final static String LIST_ALL_USERS = "1";
    final static String LIST_SEARCH_USERS = "2";
    final static String LIST_SINGLE_USER = "3";
    final static String LIST_REPO_LOAD = "4";
    final static String LIST_SINGLE_USER_WITHOUT_URL = "5";


    String errorMsg = "";
    int returnCode;
    Context context;


    public JSONLoader() {
        Log.d("XXX", "loader created");
    }

    public JSONLoader(Context context) {
        this.context = context;
    }

    @Override
    protected Object doInBackground(String... params) {
        ArrayList<GitHubUser> userArrayList = new ArrayList<GitHubUser>();
        HttpURLConnection http = null;
        URL url;
        int search_type = 0;
        String method = "GET";
        String patch = "users";
        String http_args = "";
        DataOutputStream outputStream;
        InputStream inputStream = null;
        String str = "";
        JSONObject jsonObject;
        JSONArray jsonArray;
        Object object = null;
        try {
            switch (Integer.valueOf(params[0])) {
                case 1: //LIST_ALL_USERS:
                    http_args = params[1]!=null?"?since="+params[1]:"";
                    url = new URL("https://api.github.com/users"+http_args);
                    break;
                case 2: //LIST_SEARCH_USERS:
                    http_args = params[1]!=null?"?q="+params[1]:"?";
                    http_args = http_args+(params[2]!=null?"&page="+params[2]:"");
                    url = new URL("https://api.github.com/search/users"+http_args);
                    break;
                case 3: //LIST_SINGLE_USER:
                    url = new URL(params[1]);
                    break;
                case 4: //LIST_REPO_LOAD:
                    url = new URL(params[1]);
                    break;
                case 5:
                    Log.d("XXX", "1>"+params[0]+" 2>"+params[1]);
                    url = new URL("https://api.github.com/users/"+params[1]);
                    Log.d("XXX", "5 url:"+url.toString());
                    break;
                default:
                    url = new URL("https://api.github.com/users");
                    break;
            }
            Log.d("XXX", "url:"+url.toString());
            http = (HttpsURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setRequestProperty("Accept-Charset", "UTF-8");
            http.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            http.setUseCaches(false);
            http.setDoInput(true);
            if(method.equals("POST")) {
                http.setRequestProperty("Content-Length", "" + Integer.toString(http_args.getBytes().length));
                http.setDoOutput(true);
                outputStream = new DataOutputStream(http.getOutputStream());
                outputStream.writeBytes(http_args);
                outputStream.flush();
                outputStream.close();
            }
            returnCode = http.getResponseCode();
            Log.d("XXX", "Return code:"+returnCode+" messages:"+http.getResponseMessage()+" for URL:"+url);
            inputStream = http.getInputStream();
            str = convertStreamToString(inputStream);
        } catch(IOException e) {
            inputStream = http.getErrorStream();
            str = convertStreamToString(inputStream);
            errorMsg = str;
            e.printStackTrace();
        }

        try {
            Log.d("XXX", "str:"+str);
            inputStream.close();
            object = new JSONTokener(str).nextValue();
        } catch(IOException e) {
            e.printStackTrace(); errorMsg = e.getMessage();
        } catch (JSONException e) {
            e.printStackTrace(); errorMsg = e.getMessage();
        }
        if(http!=null) { http.disconnect(); }
        return object;
    }

    @Override
    protected void onPostExecute(Object object) {
        if(errorMsg.length()>0) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context==null?MainActivity.context:context);
            alertDialogBuilder.setTitle("Error");
            alertDialogBuilder.setMessage("(" + returnCode + ")" + errorMsg);
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        super.onPostExecute(object);
    }

    String convertStreamToString(InputStream inputStreams) {
        Scanner s = new Scanner(inputStreams).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
