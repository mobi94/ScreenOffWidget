package com.sergeystasyuk.screenoffwidget;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class ApplicationsListActivity extends AppCompatActivity {

    private ApplicationListAdapter adapter;
    private List<String> applicationsPackageNameList;
    private List<Boolean> checked;
    private int checkedAppsCount;
    private PackageManager packageManager;
    private ProgressDialog progressDialog;
    public SharedPreferences sharedpreferences;
    public static final String AppsChecked = "AppsChecked";
    public static final String AppsToHide = "AppsToHide";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applications_list_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.apps_list_title);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedpreferences = getSharedPreferences(LockScreenActivity.MyPREFERENCES, Context.MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.progress_dialog));

        packageManager = getPackageManager();
        applicationsPackageNameList = new ArrayList<>();
        checked = new ArrayList<>();

        checkedAppsCount = 0;

        MyTask myTask = new MyTask();
        myTask.execute();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.applications_list);
        adapter = new ApplicationListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_app_list_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.ic_apply:
                saveCheckedToSharedPrefs();
                finish();
                return true;
            case R.id.ic_check_all:
                for(int i=0; i < checked.size(); i++)
                    checked.set(i, true);
                checkedAppsCount = checked.size();
                adapter.notifyDataSetChanged();
                updateCheckedCount();
                return true;
            case R.id.ic_uncheck_all:
                for(int i=0; i < checked.size(); i++)
                    checked.set(i, false);
                checkedAppsCount = 0;
                adapter.notifyDataSetChanged();
                updateCheckedCount();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getCheckedFromSharedPrefs(){
        Gson gson = new Gson();
        String empty_list = gson.toJson(new ArrayList<Boolean>());
        List<Boolean> checkedOld = gson.fromJson(sharedpreferences.getString(AppsChecked, empty_list),
                new TypeToken<List<Boolean>>(){}.getType());
        empty_list = gson.toJson(new ArrayList<String>());
        List<String> applicationsPackageNameListOld = gson.fromJson(sharedpreferences.getString(AppsToHide, empty_list),
                new TypeToken<ArrayList<String>>(){}.getType());
        for(int i=0; i < applicationsPackageNameListOld.size(); i++){
            for(int j=0; j < applicationsPackageNameList.size(); j++) {
                if (applicationsPackageNameList.get(j).equals(applicationsPackageNameListOld.get(i))) {
                    checked.set(j, checkedOld.get(i));
                    if (checkedOld.get(i)) checkedAppsCount++;
                    break;
                }
            }
        }
    }

    public void saveCheckedToSharedPrefs(){
        Gson gson = new Gson();
        String jsonString = gson.toJson(applicationsPackageNameList);
        sharedpreferences.edit().putString(AppsToHide, jsonString).apply();
        gson = new Gson();
        jsonString = gson.toJson(checked);
        sharedpreferences.edit().putString(AppsChecked, jsonString).apply();
    }

    public void getApplicationsList(){

        // Initialize a new Intent which action is main
        Intent intent = new Intent(Intent.ACTION_MAIN,null);
        // Set the newly created intent category to launcher
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        // Set the intent flags
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK|
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        );
        // Generate a list of ResolveInfo object based on intent filter
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent,0);

        String packageName1, packageName2;
        // Loop through the ResolveInfo list
        for(int i=0; i < resolveInfoList.size(); i++){
            packageName1 = resolveInfoList.get(i).activityInfo.applicationInfo.packageName;
            if (i < resolveInfoList.size() - 1) {
                packageName2 = resolveInfoList.get(i+1).activityInfo.applicationInfo.packageName;
                if (!packageName1.equals(packageName2)){
                    checked.add(false);
                    applicationsPackageNameList.add(packageName1);
                }
            }
            else {
                checked.add(false);
                applicationsPackageNameList.add(packageName1);
            }
        }
        getCheckedFromSharedPrefs();
    }

    public void updateCheckedCount(){
        String title;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (checkedAppsCount > 1) title = checkedAppsCount + " " + getResources().getString(R.string.apps_list_activity_subtitle_2nd_part_plural);
            else if (checkedAppsCount == 1) title = checkedAppsCount + " " + getResources().getString(R.string.apps_list_activity_subtitle_2nd_part_singular);
            else title = getResources().getString(R.string.apps_list_activity_subtitle_1st_part);
            actionBar.setSubtitle(title);
        }
    }

    /*public boolean isSystemPackage(ResolveInfo resolveInfo){
        return ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }*/

    public class ApplicationListAdapter extends RecyclerView.Adapter<ApplicationListAdapter.ViewHolder> {

        ApplicationListAdapter() {
        }

        @Override
        public int getItemCount() {
            return applicationsPackageNameList.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater mInflater = LayoutInflater.from(parent.getContext());
            View view = mInflater.inflate(R.layout.application_list_item, parent, false);
            return new ViewHolder(view);
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            ImageView appIcon;
            TextView appTitle;
            CheckBox appSelect;
            //public CardView cardView;

            ViewHolder(View v) {
                super(v);
                appIcon = (ImageView) v.findViewById(R.id.app_icon);
                appTitle = (TextView) v.findViewById(R.id.app_title);
                appSelect = (CheckBox) v.findViewById(R.id.app_checkBox);
                //cardView = (CardView) v.findViewById(R.id.card_view);
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int pos) {
            final int position = holder.getAdapterPosition();
            final String currentPackageName = applicationsPackageNameList.get(position);
            holder.appIcon.setImageDrawable(getAppIconByPackageName(currentPackageName));
            holder.appTitle.setText(getApplicationLabelByPackageName(currentPackageName));
            holder.appSelect.setChecked(checked.get(position));
            holder.appSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checked.set(position, !checked.get(position));
                    if (checked.get(position)) {
                        checkedAppsCount++;
                        /*holder.cardView.setCardBackgroundColor(ContextCompat.
                                getColor(ApplicationsListActivity.this, R.color.colorPrimary));
                        holder.appTitle.setTextColor(Color.parseColor("#ffffff"));*/
                    }
                    else {
                        checkedAppsCount--;
                        /*holder.cardView.setCardBackgroundColor(ContextCompat.
                                getColor(ApplicationsListActivity.this, R.color.cardview_light_background));
                        holder.appTitle.setTextColor(Color.parseColor("#000000"));*/
                    }
                    updateCheckedCount();
                }
            });
            /*if (checked.get(position)) {
                holder.cardView.setCardBackgroundColor(ContextCompat.
                        getColor(ApplicationsListActivity.this, R.color.colorPrimary));
                holder.appTitle.setTextColor(Color.parseColor("#ffffff"));
            }
            else {
                holder.cardView.setCardBackgroundColor(ContextCompat.
                        getColor(ApplicationsListActivity.this, R.color.cardview_light_background));
                holder.appTitle.setTextColor(Color.parseColor("#000000"));
            }*/
        }

        Drawable getAppIconByPackageName(String packageName){
            Drawable icon;
            try{
                icon = packageManager.getApplicationIcon(packageName);
            }catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
                icon = getDrawable(R.mipmap.ic_help);
            }
            return icon;
        }

        String getApplicationLabelByPackageName(String packageName){
            ApplicationInfo applicationInfo;
            String label = getResources().getString(R.string.apps_list_activity_list_item_title_unknown);
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                if(applicationInfo!=null){
                    label = (String)packageManager.getApplicationLabel(applicationInfo);
                }

            }catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return label;
        }
    }

    class MyTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getApplicationsList();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateCheckedCount();
            adapter.notifyDataSetChanged();
            if (progressDialog.isShowing()) progressDialog.dismiss();
        }
    }

}
