/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aengbee.android.leanback.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import com.aengbee.android.leanback.R;
import com.aengbee.android.leanback.data.FetchVideoService;
import com.aengbee.android.leanback.data.VideoContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Activity that showcases different aspects of GuidedStepFragments.
 */
public class GuidedStepActivity extends Activity {

    private static final int CONTINUE = 0;
    private static final int BACK = 1;
    private static final int DELETE = 2;
    private static final int NEW = 3;
    private static final int OLD = 4;
    private static final int OPTION_CHECK_SET_ID = 10;

    private static JSONObject serverList;
    private static GuidedAction serverName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), android.R.id.content);
        }
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addCheckedAction(List<GuidedAction> actions, Context context,
                                         String title, String desc) {
        GuidedAction guidedAction = new GuidedAction.Builder()
                .title(title)
                .description(desc)
                .build();

        actions.add(guidedAction);
    }

    public static class FirstStepFragment extends GuidedStepFragment {
        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_First;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_first_title);
            String breadcrumb = getString(R.string.guidedstep_first_breadcrumb);
            String description = getString(R.string.guidedstep_first_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, CONTINUE,
                    getResources().getString(R.string.newDB),
                    getResources().getString(R.string.enterDB));
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            String serverJson = sharedPreferences.getString("serverList","null");

            if(serverJson!="null" && !serverJson.equals("{}")) {
                try {
                    serverList = new JSONObject(serverJson);

                    for (int i = 0; i < serverList.names().length(); i++) {
                        addCheckedAction(actions, getActivity(), serverList.names().getString(i), serverList.getString(serverList.names().getString(i)));
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            addAction(actions, BACK,
                    getResources().getString(R.string.guidedstep_cancel),
                    "");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            if (action.getId() == CONTINUE) {
                Log.d("d", "onGuidedActionClicked: "+getSelectedActionPosition());
                SecondStepFragment next2 = SecondStepFragment.newInstance(getSelectedActionPosition()-1);
                GuidedStepFragment.add(fm, next2);
            } else {
                getActivity().finishAfterTransition();
            }
            //ThirdStepFragment next = ThirdStepFragment.newInstance(getSelectedActionPosition() - 1);
            //GuidedStepFragment.add(fm, next);
        }
    }

    public static class SecondStepFragment extends GuidedStepFragment {
        private final static String ARG_DB_IDX = "arg.db.index";
        public static SecondStepFragment newInstance(int option) {
            final SecondStepFragment f = new SecondStepFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_DB_IDX, option);
            f.setArguments(args);
            return f;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }


        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {

            Log.d("tt", "onGuidedActionClicked: "+getArguments().getInt(ARG_DB_IDX));


            if(getArguments().getInt(ARG_DB_IDX)>-1 && serverList.names().length()>1){
                try {
                    serverName = new GuidedAction.Builder()
                            .id(OLD)
                            .title(getString(R.string.guidedstep_serveraddress))
                            .descriptionEditable(false)
                            .description(serverList.getString(serverList.names().getString(getArguments().getInt(ARG_DB_IDX))))
                            .build();



                    GuidedAction delete = new GuidedAction.Builder()
                            .id(DELETE)
                            .title(getString(R.string.delete))
                            .build();

                    actions.add(serverName);
                    actions.add(delete);
                }
                catch(JSONException ex){
                    ex.printStackTrace();
                }


            } else {

                serverName = new GuidedAction.Builder()
                        .id(NEW)
                        .title(getString(R.string.guidedstep_serveraddress))
                        .descriptionEditable(true)
                        .description("http://example.com/.json")
                        .build();


                GuidedAction save = new GuidedAction.Builder()
                        .id(CONTINUE)
                        .title(getString(R.string.save))
                        .build();

                actions.add(serverName);
                actions.add(save);
            }


            GuidedAction back = new GuidedAction.Builder()
                    .id(BACK)
                    .title(getString(R.string.back))
                    .build();

            actions.add(back);

        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();

            if (action.getId() == CONTINUE) {
                ThirdStepFragment next3 = ThirdStepFragment.newInstance(serverName.getDescription().toString());
                GuidedStepFragment.add(fm, next3);
            } else if (action.getId() == DELETE){
                try {
                    ThirdStepFragment next3 = ThirdStepFragment.newInstance(serverList.names().getString(getArguments().getInt(ARG_DB_IDX)));
                    GuidedStepFragment.add(fm, next3);
                }
                catch (JSONException ex){
                    ex.printStackTrace();
                }
            } else if (action.getId() == BACK){
                getFragmentManager().popBackStack();
            }


        }

    }

    public static class ThirdStepFragment extends GuidedStepFragment {
        private final static String ARG_OPTION_IDX = "arg.option.idx";

        public static ThirdStepFragment newInstance(String option) {
            final ThirdStepFragment f = new ThirdStepFragment();
            final Bundle args = new Bundle();
            args.putString(ARG_OPTION_IDX, option);
            f.setArguments(args);
            return f;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_third_title);
            String breadcrumb = getString(R.string.guidedstep_third_breadcrumb);
            String arg = getArguments().getString(ARG_OPTION_IDX);
            String desc2 = "";

            if(arg.matches("\\d+(?:\\.\\d+)?")){
                desc2 = getString(R.string.confirmDelete);
            } else {
                desc2 = getString(R.string.confirmNew);
                /*
                if(!testURL(arg)){
                    Toast.makeText(getActivity(), "file not exist on "+arg, Toast.LENGTH_LONG).show();
                    getFragmentManager().popBackStack();
                }
                */
            }
            String description = getString(R.string.guidedstep_third_command)  + desc2;

            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, CONTINUE, getString(R.string.yes) , getString(R.string.save));
            addAction(actions, BACK, getString(R.string.no) , getString(R.string.back));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            String arg = getArguments().getString(ARG_OPTION_IDX);
            SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

            if (action.getId() == CONTINUE) {
                if(arg.matches("\\d+(?:\\.\\d+)?")){
                    Log.d("11", "onGuidedActionClicked: "+1);
                    getActivity().getContentResolver().delete(VideoContract.VideoEntry.CONTENT_URI,
                            "suggest_is_live = ?", new String[] {arg});
                    serverList.remove(arg);
                    Log.d("1111", "onCreateActions: "+ serverList);
                    sharedPreferencesEditor.putString("serverList",serverList.toString());
                    sharedPreferencesEditor.apply();

                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    arg = arg.replace("@usb", sharedPreferences.getString("USB_path",""));
                    Log.d("11", "onGuidedActionClicked: "+2+arg);
                    Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
                    serviceIntent.putExtra("data_url", arg);
                    getActivity().startService(serviceIntent);
                }
                getActivity().finishAfterTransition();
            } else {
                getFragmentManager().popBackStack();
            }
        }

        public boolean testURL(String strUrl){

            try {
                URL url = new URL(strUrl);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setConnectTimeout(5000);
                urlConn.connect();
                return urlConn.getResponseCode() == HttpURLConnection.HTTP_OK;
            } catch (IOException | NetworkOnMainThreadException e) {
                Log.d("GuidedStepAcitivity:", "testURL: \"Error creating HTTP connection\""+e.toString());
                e.printStackTrace();

                return false;

            }
        }

    }

}
