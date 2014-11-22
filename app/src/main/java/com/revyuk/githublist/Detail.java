package com.revyuk.githublist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class Detail extends Activity {

    Context context;
    String login, url, avatarurl, reposurl;
    TextView loginText;
    TextView fullnameText, locationText, createdText, followers, following, email;
    ImageView avatar;
    ImageButton fav;
    Handler handler;
    TextView loadingrepo;
    ListView listRepo;
    ArrayList<ListRepos> arrayListRepos;
    My_Adapter my_adapter;
    DBPreferences dbPreferences;
    SQLiteDatabase db;
    boolean editedFlag = false;

    class ListRepos {
        private String repoName;
        private String repoDescr;
        private String repoLang;

        ListRepos(String new_repoName, String new_repoDescr, String new_repoLang) {
            repoName = new_repoName;
            repoDescr = new_repoDescr;
            repoLang = new_repoLang;
        }
        public String getRepoName() {
            return repoName;
        }
        public String getRepoDescr() {
            return repoDescr;
        }
        public String getRepoLang() {
            return repoLang;
        }
    }

    class My_Adapter extends ArrayAdapter<ListRepos> {

        public My_Adapter(Context context, int resource, ArrayList<ListRepos> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.reposlistitem, parent, false);
            }
            TextView repoName = (TextView) convertView.findViewById(R.id.tv_repoName);
            TextView repoDescr = (TextView) convertView.findViewById(R.id.tv_repoDescr);
            TextView repoLang = (TextView) convertView.findViewById(R.id.tv_repolang);
            repoName.setText(getItem(position).getRepoName());
            repoDescr.setText(getItem(position).getRepoDescr());
            repoLang.setText(getItem(position).getRepoLang());
            return convertView;
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("XXX", "obBackpressed"+editedFlag);
        Intent intent = getIntent();
        if(editedFlag) {
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
        finish();
        //super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        context = getApplicationContext();
        loadingrepo = (TextView) findViewById(R.id.loadingRepo);
        listRepo = (ListView) findViewById(R.id.listrepositories);
        listRepo.setVisibility(View.GONE);

        loginText = (TextView) findViewById(R.id.login1);
        fullnameText = (TextView) findViewById(R.id.fullname);
        locationText = (TextView) findViewById(R.id.location);
        createdText = (TextView) findViewById(R.id.created);
        avatar = (ImageView) findViewById(R.id.avatar);
        followers = (TextView) findViewById(R.id.followers);
        following = (TextView) findViewById(R.id.following);
        email = (TextView) findViewById(R.id.email);
        fav = (ImageButton) findViewById(R.id.fav_on_off);
        fav.setVisibility(View.GONE);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if(msg.obj != null) {
                            JSONObject json = (JSONObject) msg.obj;
                            if(!json.isNull("login")) {
                                try {
                                    loginText.setText(json.getString("login"));
                                    if(!json.isNull("repos_url")) { reposurl = json.getString("repos_url"); }
                                    if(!json.isNull("name")) { fullnameText.setText(json.getString("name")); }
                                    if(!json.isNull("location")) { locationText.setText(json.getString("location")); }
                                    if(!json.isNull("created_at")) { createdText.setText(json.getString("created_at")); }
                                    if(!json.isNull("followers")) { followers.setText("Followers: "+json.getString("followers")); }
                                    if(!json.isNull("following")) { following.setText("Following: "+json.getString("following")); }
                                    if(!json.isNull("email")) { email.setText(json.getString("email")); }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    case 5:
                        avatar.setImageBitmap((Bitmap)msg.obj);
                        LoadRepos loadRepos = new LoadRepos();
                        loadRepos.run();
                        break;
                    case 6:
                        try {
                            JSONArray jArray = (JSONArray) msg.obj;
                            arrayListRepos = new ArrayList<ListRepos>();
                            for(int i=0; i<jArray.length();i++) {
                                arrayListRepos.add(0, new ListRepos(jArray.getJSONObject(i).getString("name"),
                                        jArray.getJSONObject(i).getString("description"),
                                        jArray.getJSONObject(i).getString("language")));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        my_adapter = new My_Adapter(context, R.layout.reposlistitem, arrayListRepos);
                        listRepo.setAdapter(my_adapter);
                        loadingrepo.setVisibility(View.GONE);
                        listRepo.setVisibility(View.VISIBLE);
                        listRepo.setSelection(listRepo.getCount()-1);
                        break;
                    case 7:
                        break;
                    case 999:
                        finish();
                        break;
                }
                return false;
            }
        });
        Intent intent = getIntent();
        login = intent.getStringExtra("login");
        url = intent.getStringExtra("url");
        dbPreferences = new DBPreferences(context);
        db = dbPreferences.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from "+DBPreferences.FAVORITES_TABLE_NAME+" where "
                +DBPreferences.FAVORITES_LOGIN_COLUMN+" = \""+login+"\"", null);
        Log.d("XXX", "Query:" + "select * from " + DBPreferences.FAVORITES_TABLE_NAME + " where "
                + DBPreferences.FAVORITES_LOGIN_COLUMN + " = \"" + login + "\"");
        if(cursor.getCount()==0) {
            fav.setImageResource(android.R.drawable.btn_star_big_off);
        } else {
            fav.setImageResource(android.R.drawable.btn_star_big_on);
        }
        fav.setVisibility(View.VISIBLE);

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editedFlag = true;
                int delete = db.delete(DBPreferences.FAVORITES_TABLE_NAME,
                        DBPreferences.FAVORITES_LOGIN_COLUMN+" = \""+login+"\"", null);
                Log.d("XXX", "delete:"+delete);
                if(delete==0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBPreferences.FAVORITES_LOGIN_COLUMN, login);
                    cv.put(DBPreferences.FAVORITES_USER_URL_COLUMN, url);
                    cv.put(DBPreferences.FAVORITES_AVATAR_URL_COLUMN, avatarurl);
                    long insert = db.insert(DBPreferences.FAVORITES_TABLE_NAME, null, cv);
                    Log.d("XXX", "insert:"+insert);
                    if(insert > 0) fav.setImageResource(android.R.drawable.btn_star_big_on);
                } else {
                    fav.setImageResource(android.R.drawable.btn_star_big_off);
                }
            }
        });
        loadData();
    }

    class LoadRepos extends Thread {
        @Override
        public void run() {
            Object obj;
            JSONObject jsonObj;
            JSONArray jsonArray;
            if(reposurl.length()>0) {
                JSONLoader repos = new JSONLoader(context);
                repos.execute(JSONLoader.LIST_REPO_LOAD, reposurl);
                try {
                    obj = repos.get();
                    if(obj instanceof JSONArray) {
                        Message msg = handler.obtainMessage(6, obj);
                        handler.sendMessage(msg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void loadData() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Object object=null;
                JSONObject jsonObject;
                Message msg;

                JSONLoader user = new JSONLoader(context);
                user.execute(JSONLoader.LIST_SINGLE_USER, url);
                try {
                    object = user.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if(object instanceof JSONObject) {
                    try {
                        jsonObject = (JSONObject) object;
                        Log.d("XXX", "json "+jsonObject.toString());
                        if(!jsonObject.isNull("login")) {
                            msg = handler.obtainMessage(1, jsonObject);
                            handler.sendMessage(msg);
                            InputStream inputStream = null;
                            try {
                                if(!jsonObject.isNull("avatar_url")) {
                                    avatarurl = jsonObject.getString("avatar_url");
                                    inputStream = new URL(jsonObject.getString("avatar_url")).openStream();
                                    msg = handler.obtainMessage(5, Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), 150, 150, false));
                                    handler.sendMessage(msg);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d("XXX", e.getMessage());
                            }
                        } else {
                            Log.d("XXX", "NO1");
                            handler.sendEmptyMessage(999);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("XXX", e.getMessage());
                    }
                }  else { Log.d("XXX", "NO2"); }
            }
        });
        thread.start();
    }
}
