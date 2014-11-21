package com.revyuk.githublist;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity {

    final static int USERLIST_INDEX_UP = 1;
    final static int USERLIST_INDEX_DOWN = 2;
    final static int USERLIST_INDEX_REFRESH = 0;
    final static String ARRAY_INDEX_NAME = "arrayIndex";
    int arrayIndex;
    static int searchPage = 1;
    public static String searchWord = "";
    boolean searchFlag = false;


    static Context context;
    Handler handler;
    Bundle bundle;
    android.support.v7.app.ActionBar actionBar;
    ProgressBar pb, pbLoadAvatar;
    DrawerLayout drawer;
    ListView leftList;
    ListView mainList;
    View header, footer;
    ArrayList<GitHubUser> mainArrayList = new ArrayList<GitHubUser>();
    ArrayList<GitHubUser> leftArrayList = new ArrayList<GitHubUser>();
    MainArrayAdapter mainArrayAdapter;
    LeftArrayAdapter leftArrayAdapter;
    DBPreferences dbPreferences;
    SQLiteDatabase db;
    UpdateAvatars updateAvatarsThread;
    int startY, moveY, headerHeight, footerHeight;
    final static int PULL_REFRESH_OFFSET = 300;
    boolean pullRefreshFlag = true;

    private class MainArrayAdapter extends ArrayAdapter<GitHubUser> {

        public MainArrayAdapter(Context context, int resource, List<GitHubUser> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.userlistitem, parent, false);
            }
            TextView login = (TextView) convertView.findViewById(R.id.login_name);
            login.setText(getItem(position).getUserLogin());
            ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar_image);
            avatar.setImageBitmap(getItem(position).getUserBitmap());
            if(db.isOpen()) {
                ImageView favicon = (ImageView) convertView.findViewById(R.id.favorites_icon);
                Cursor cursor = db.rawQuery("select * from "+DBPreferences.FAVORITES_TABLE_NAME+" where "+
                        DBPreferences.FAVORITES_LOGIN_COLUMN+"=\""+getItem(position).getUserLogin()+"\"", null);
                if(cursor.moveToFirst()) {
                    favicon.setImageResource(android.R.drawable.btn_star_big_on);
                } else {
                    favicon.setImageBitmap(null);
                }
            }
            return convertView;
        }
    }

    private class LeftArrayAdapter extends ArrayAdapter<GitHubUser> {

        public LeftArrayAdapter(Context context, int resource, List<GitHubUser> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.userlistitem, parent, false);
            }
            ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar_image);
            if(leftArrayList.get(position).getUserBitmap() != null) {
                avatar.setImageBitmap(leftArrayAdapter.getItem(position).getUserBitmap());
            }
            TextView login = (TextView) convertView.findViewById(R.id.login_name);
            login.setText(leftArrayList.get(position).getUserLogin());
            login.setTextColor(Color.WHITE);
            return convertView;
        }
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(Gravity.LEFT)) {
            drawer.closeDrawers();
        } else {
            if(searchFlag) {
                if(updateAvatarsThread.getStatus() == AsyncTask.Status.RUNNING) updateAvatarsThread.cancel(true);
                searchWord = "";
                searchFlag = false;
                actionBar.setSubtitle("");
                loadAllUsers(USERLIST_INDEX_REFRESH);
            } else {
                super.onBackPressed();
            }
        }
    }

    class MyOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(!pullRefreshFlag) return false;

            //TextView headerText = (TextView) findViewById(R.id.headerText);
            //TextView footerText = (TextView) findViewById(R.id.footerText);
            ProgressBar upProgressBar1 = (ProgressBar) findViewById(R.id.pullUpProgressBar1);
            ProgressBar upProgressBar2 = (ProgressBar) findViewById(R.id.pullUpProgressBar2);
            ProgressBar downProgressBar1 = (ProgressBar) findViewById(R.id.pullDownProgressBar1);
            ProgressBar downProgressBar2 = (ProgressBar) findViewById(R.id.pullDownProgressBar2);
            upProgressBar1.setMax(PULL_REFRESH_OFFSET);
            upProgressBar2.setMax(PULL_REFRESH_OFFSET);
            downProgressBar1.setMax(PULL_REFRESH_OFFSET);
            downProgressBar2.setMax(PULL_REFRESH_OFFSET);

            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                startY = (int) event.getY();
            }
            if(event.getAction() == MotionEvent.ACTION_MOVE) {
                moveY = (int) event.getY();
                if(mainList.getFirstVisiblePosition()==0 && (startY < moveY)) {
                    // pull up
                    if((moveY - startY) > PULL_REFRESH_OFFSET) {
                        header.setPadding(0, 0, 0, 0);
                        upProgressBar1.setProgress(0);
                        upProgressBar2.setProgress(0);
                    } else {
                        upProgressBar1.setProgress(moveY - startY);
                        upProgressBar2.setProgress(moveY - startY);
                        header.setPadding(0, (moveY - startY), 0, 0);
                    }
                }
                if(mainList.getLastVisiblePosition()+1==mainList.getCount() && startY > moveY) {
                    // pull down
                    if((startY - moveY) > PULL_REFRESH_OFFSET) {
                        downProgressBar1.setProgress(0);
                        downProgressBar2.setProgress(0);
                        footer.setPadding(0, 0, 0, 0);
                    } else {
                        footer.setPadding(0, 0, 0, headerHeight+(startY - moveY));
                        downProgressBar1.setProgress(startY - moveY);
                        downProgressBar2.setProgress(startY - moveY);
                    }
                }
            }
            if(event.getAction() == MotionEvent.ACTION_UP) {
                moveY = (int) event.getY();
                if(mainList.getFirstVisiblePosition()==0 && ((moveY - startY) > PULL_REFRESH_OFFSET)) {
                    // pull up commit
                    pullRefreshFlag = false;
                    upProgressBar1.setProgress(0); upProgressBar2.setProgress(0);
                    downProgressBar1.setProgress(0); downProgressBar2.setProgress(0);
                    if(searchFlag) {
                        if(updateAvatarsThread != null) updateAvatarsThread.cancel(true);
                        searchPage++;
                        searchUsers();
                        header.setPadding(0,headerHeight,0,0);
                        pullRefreshFlag = true;
                    } else {
                        //asyncTaskCancelFlag = true;
                        if(updateAvatarsThread != null) updateAvatarsThread.cancel(true);
                        loadAllUsers(USERLIST_INDEX_UP);
                        header.setPadding(0, headerHeight, 0, 0);
                        //mainList.setSelection(my_adapter.getCount() - 1);
                        pullRefreshFlag = true;
                    }
                    mainList.requestLayout();
                }else if(mainList.getLastVisiblePosition()+1==mainList.getCount() && ((startY - moveY) > PULL_REFRESH_OFFSET)) {
                    // pull down commit
                    pullRefreshFlag = false;
                    if(searchFlag) {
                        if(updateAvatarsThread != null) updateAvatarsThread.cancel(true);
                        searchPage--;
                        searchUsers();
                        header.setPadding(0,headerHeight,0,0);
                        pullRefreshFlag = true;
                    } else {
                        if(updateAvatarsThread != null) updateAvatarsThread.cancel(true);
                        loadAllUsers(USERLIST_INDEX_DOWN);
                        footer.setPadding(0, 0, 0, headerHeight);
                        mainList.refreshDrawableState();
                        mainList.setSelection(mainArrayAdapter.getCount()-1);
                        pullRefreshFlag = true;
                    }
                    mainList.requestLayout();
                } else {
                    upProgressBar1.setProgress(0); upProgressBar2.setProgress(0);
                    downProgressBar1.setProgress(0); downProgressBar2.setProgress(0);
                    header.setPadding(0,headerHeight,0,0);
                    footer.setPadding(0,0,0,footerHeight);
                }
                startY=0;
            }
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
            Cursor cursor = db.rawQuery("select * from "+DBPreferences.FAVORITES_TABLE_NAME, null);
            Log.d("XXX", "cnt fav:"+cursor.getCount());
            leftArrayList.clear();
            if(cursor.moveToFirst()) {
                do {
                    leftArrayList.add(new GitHubUser(cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_LOGIN_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_AVATAR_URL_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_USER_URL_COLUMN)), null));
                    leftArrayAdapter.notifyDataSetChanged();
                } while (cursor.moveToNext());
                updateAvatarsThread = new UpdateAvatars();
                updateAvatarsThread.execute();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Cursor cursor;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        context = this;
        actionBar = getSupportActionBar();

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) {       }
            @Override
            public void onDrawerOpened(View view) {
                getSupportActionBar().setTitle("Favorites");
                //actionBar.setDisplayHomeAsUpEnabled(true);
            }

            @Override
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle("Github list");
                //actionBar.setDisplayHomeAsUpEnabled(false);
            }
            @Override
            public void onDrawerStateChanged(int i) {       }
        });

        pb = (ProgressBar) findViewById(R.id.content_loading);
        pbLoadAvatar = (ProgressBar) findViewById(R.id.progressBarLoadingAvatar);
        pbLoadAvatar.setProgress(0);
        pbLoadAvatar.setVisibility(View.GONE);
        bundle = new Bundle();

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        pbLoadAvatar.setVisibility(View.VISIBLE);
                        mainArrayAdapter.notifyDataSetChanged();
                        leftArrayAdapter.notifyDataSetChanged();
                        pbLoadAvatar.incrementProgressBy(1);
                        break;
                    case 2:
                        pb.setVisibility(View.GONE);
                        mainList.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        int i = bundle.getInt("deleteFavorite", -1);
                        if(i>=0) {
                            db.execSQL("delete from "+DBPreferences.FAVORITES_TABLE_NAME+" where "+
                                    DBPreferences.FAVORITES_LOGIN_COLUMN+"= \""+leftArrayList.get(i).getUserLogin()+"\"");
                            leftArrayList.remove(i);
                            bundle.remove("deleteFavorite");
                            leftArrayAdapter.notifyDataSetChanged();
                            mainArrayAdapter.notifyDataSetChanged();
                        }
                        break;
                    case 4:
                        break;
                }
                return false;
            }
        });

        leftList = (ListView) findViewById(R.id.left_list);
        View searchHeader = getLayoutInflater().inflate(R.layout.search, leftList, false);
        final TextView searchTextView = (TextView) searchHeader.findViewById(R.id.searchTextView);
        searchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("XXX", "Searching ....." + searchTextView.getText());
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                drawer.closeDrawers();
                if(updateAvatarsThread.getStatus() == AsyncTask.Status.RUNNING) updateAvatarsThread.cancel(true);
                searchWord = searchTextView.getText().toString();
                searchUsers();
                searchTextView.setText("");
                return false;
            }
        });
        leftList.addHeaderView(searchHeader, null, false);
        leftList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(context, Detail.class);
                intent.putExtra("login", leftArrayList.get(position - 1).getUserLogin());
                intent.putExtra("url", leftArrayList.get(position - 1).getUserUrl());
                startActivityForResult(intent, 1);
            }
        });
        leftList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("XXX", "click for del " + (position - 1));
                AlertDialog.Builder ab = new AlertDialog.Builder(context);
                ab.setTitle("Remove favorites record");
                ab.setMessage("Delete this ?");
                ab.setCancelable(true);
                bundle.putInt("deleteFavorite", position - 1);
                ab.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                ab.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.sendEmptyMessage(3);
                    }
                });
                ab.show();
                return false;
            }
        });
        mainList = (ListView) findViewById(R.id.main_list);
        header = getLayoutInflater().inflate(R.layout.header, mainList, false);
        footer = getLayoutInflater().inflate(R.layout.footer, mainList, false);
        header.setBackgroundColor(Color.DKGRAY);
        footer.setBackgroundColor(Color.DKGRAY);
        mainList.addHeaderView(header, null, false);
        mainList.addFooterView(footer, null, false);

        header.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        footer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        headerHeight = header.getMeasuredHeight()*-1;
        footerHeight = footer.getMeasuredHeight()*-1;
        header.setPadding(0, headerHeight, 0, 0);
        footer.setPadding(0, 0, 0, headerHeight);

        mainList.setOnTouchListener(new MyOnTouchListener());
        mainList.setVisibility(View.GONE);
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(context, Detail.class);
                intent.putExtra("login", mainArrayList.get(position-1).getUserLogin());
                intent.putExtra("url", mainArrayList.get(position-1).getUserUrl());
                startActivityForResult(intent, 1);
            }
        });
        mainArrayAdapter = new MainArrayAdapter(context, R.id.main_list, mainArrayList);
        leftArrayAdapter = new LeftArrayAdapter(context, R.id.left_list, leftArrayList);
        mainList.setAdapter(mainArrayAdapter);
        leftList.setAdapter(leftArrayAdapter);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                dbPreferences = new DBPreferences(context);
                db = dbPreferences.getWritableDatabase();
                Cursor cursor = db.rawQuery("select * from "+ DBPreferences.PREFERENCES_TABLE_NAME+" where "
                        +DBPreferences.PREFERENCES_KEY_COLUMN+" = \""+ARRAY_INDEX_NAME+"\"", null);
                Log.d("XXX", "row:"+cursor.getCount());
                if(cursor.getCount()>0) {
                    cursor.moveToFirst();
                    arrayIndex = Integer.valueOf(cursor.getString(cursor.getColumnIndex(DBPreferences.PREFERENCES_VALUE_COLUMN)));
                    Log.d("XXX", "select");
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put(DBPreferences.PREFERENCES_KEY_COLUMN, ARRAY_INDEX_NAME);
                    cv.put(DBPreferences.PREFERENCES_VALUE_COLUMN, "0");
                    db.insert(DBPreferences.PREFERENCES_TABLE_NAME, null, cv);
                    arrayIndex = 0;
                    Log.d("XXX", "insert");
                }

                cursor = db.rawQuery("select * from "+DBPreferences.FAVORITES_TABLE_NAME, null);
                Log.d("XXX", "cnt fav:"+cursor.getCount());
                if(cursor.moveToFirst()) {
                    do {
                        leftArrayList.add(new GitHubUser(cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_LOGIN_COLUMN)),
                                cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_AVATAR_URL_COLUMN)),
                                cursor.getString(cursor.getColumnIndex(DBPreferences.FAVORITES_USER_URL_COLUMN)), null));
                        leftArrayAdapter.notifyDataSetChanged();
                    } while (cursor.moveToNext());
                }

                loadAllUsers(USERLIST_INDEX_REFRESH);
            }
        });
        thread.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContentValues cv = new ContentValues();
        cv.put(DBPreferences.PREFERENCES_KEY_COLUMN, ARRAY_INDEX_NAME);
        cv.put(DBPreferences.PREFERENCES_VALUE_COLUMN, String.valueOf(arrayIndex));
        int update = db.update(DBPreferences.PREFERENCES_TABLE_NAME, cv, DBPreferences.PREFERENCES_KEY_COLUMN+"= ?", new String[] {ARRAY_INDEX_NAME});
        Log.d("XXX", "DB updated :"+update);
        db.close();
        dbPreferences.close();
    }

    void loadAllUsers(int index) {
        JSONArray jsonArray;
        JSONObject jsonObject = null;

        switch (index) {
            case USERLIST_INDEX_UP:{
                arrayIndex = arrayIndex + 100;
                break;
            }
            case USERLIST_INDEX_DOWN: {
                arrayIndex = arrayIndex - 100;
                break;
            }
            case USERLIST_INDEX_REFRESH:
                break;
        }
        Log.d("XXX", "Global index:" + arrayIndex);
        if(arrayIndex <0) { arrayIndex = 0; return; }
        JSONLoader ull = new JSONLoader();
        ull.execute(JSONLoader.LIST_ALL_USERS, String.valueOf(arrayIndex));
        try {
            if(ull.get() instanceof JSONArray) {
                jsonArray = (JSONArray) ull.get();
                if(jsonArray.length()>0) {
                    mainArrayList.clear();
                    for(int x=0; x < jsonArray.length(); x++) {
                        jsonObject = jsonArray.getJSONObject(x);
                        mainArrayList.add(0, new GitHubUser(jsonObject.getString("login"),
                                jsonObject.getString("avatar_url"),
                                jsonObject.getString("url"), null));
                    }
                    updateAvatarsThread = new UpdateAvatars();
                    updateAvatarsThread.execute();

                } else {
                    showMessage("Nothing ... :(");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handler.sendEmptyMessage(2);
    }

    public void searchUsers() {
        JSONArray jsonArray;
        JSONObject jsonObject;
        if(searchPage<=0) searchPage = 1;
        JSONLoader search = new JSONLoader();
        search.execute(JSONLoader.LIST_SEARCH_USERS, searchWord, String.valueOf(searchPage));
        try {
            if(search.get() instanceof JSONObject) {
                jsonObject = (JSONObject) search.get();
                if(!jsonObject.isNull("total_count")) {
                    jsonArray = jsonObject.getJSONArray("items");
                    if(jsonArray.length()>0) {
                        searchFlag = true;
                        int total_count = Integer.valueOf(jsonObject.getString("total_count"));
                        int current_count = jsonArray.length();
                        if(actionBar != null) {
                            actionBar.setSubtitle("Found "+total_count+" items. "+
                                    searchPage+"page of "+(total_count%30==0?total_count/30:total_count/30+1));
                        }
                        mainArrayAdapter.clear();
                        pb.setMax(jsonArray.length());
                        pb.setProgress(0);
                        for(int x=0; x < jsonArray.length(); x++) {
                            jsonObject = jsonArray.getJSONObject(x);
                            mainArrayAdapter.insert(new GitHubUser(jsonObject.getString("login"), jsonObject.getString("avatar_url"), jsonObject.getString("url"), null), 0);
                        }
                        updateAvatarsThread = new UpdateAvatars();
                        updateAvatarsThread.execute();
                        leftList.setSelection(mainArrayAdapter.getCount() - 1);
                    } else {
                        showMessage("Nothing not found ... :(");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public class UpdateAvatars extends AsyncTask<Integer, Void, Void> {
        int pos;

        private Bitmap loadImage(int index, ArrayList<GitHubUser> arrayList) {
            InputStream inputStream = null;
            try {
                inputStream = new URL(arrayList.get(index).getAvatarUrl()).openStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("XXX", e.getMessage());
            }
            return Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), 70, 70, false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbLoadAvatar.setVisibility(View.VISIBLE);
            pbLoadAvatar.setMax(mainArrayList.size() + leftArrayList.size());
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pbLoadAvatar.setProgress(0);
            pbLoadAvatar.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pbLoadAvatar.setProgress(0);
            pbLoadAvatar.setVisibility(View.GONE);
        }

        @Override
        public Void doInBackground(Integer... params) {
            try {
                if (params.length == 0) {
                    for (pos=0; pos<leftArrayList.size();pos++) {
                        if (isCancelled()) break;
                        if(leftArrayList.get(pos).getUserBitmap() == null) {
                            leftArrayList.get(pos).setUserBitmap(loadImage(pos, leftArrayList));
                        }
                        publishProgress();
                        Thread.sleep(1);
                    }
                    for (pos = mainArrayList.size() - 1; pos >= 0; pos--) {
                        if (isCancelled()) break;
                        if(mainArrayList.get(pos).getUserBitmap() == null) {
                            mainArrayList.get(pos).setUserBitmap(loadImage(pos, mainArrayList));
                        }
                        publishProgress();
                        Thread.sleep(1);
                    }
                } else {
                    mainArrayList.get(pos).setUserBitmap(loadImage(params[0], mainArrayList));
                    publishProgress();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d("XXX", "Interrupted blat`");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            handler.sendEmptyMessage(1);
        }
    }

    void showMessage(String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.context);
        alertDialogBuilder.setMessage(msg);
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
}
