/*
 * Copyright (c) 2016 The Android Open Source Project
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

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.InputType;
import android.widget.Toast;

import com.aengbee.android.leanback.R;
import com.aengbee.android.leanback.data.FetchVideoService;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AuthenticationActivity extends Activity {
    private static final int CONTINUE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), android.R.id.content);
        }
    }

    public static class FirstStepFragment extends GuidedStepFragment {
        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_First;
        }

        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.pref_title_screen_signin);
            String description = getString(R.string.pref_title_login_description);
            Drawable icon = getActivity().getDrawable(R.drawable.launch_screen);
            return new GuidanceStylist.Guidance(title, description, "", icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction enterUsername = new GuidedAction.Builder()
                    .title(getString(R.string.pref_title_username))
                    .descriptionEditable(true)
                    .build();
            GuidedAction enterPassword = new GuidedAction.Builder()
                    .title(getString(R.string.pref_title_password))
                    .descriptionEditable(true)
                    .descriptionInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT)
                    .build();
            GuidedAction login = new GuidedAction.Builder()
                    .id(CONTINUE)
                    .title(getString(R.string.guidedstep_continue))
                    .build();
            actions.add(enterUsername);
            actions.add(enterPassword);
            actions.add(login);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == CONTINUE) {
                // TODO Authenticate your account
                // Assume the user was logged in
                String id = getActions().get(0).getDescription().toString();
                String pw = getActions().get(1).getDescription().toString();

                if(id.equals("admin")){

                    new getDatabase().execute("http://fytoz.asuscomm.com/android-tv/index.php?pw="+pw);

                }
                else{
                    Toast.makeText(getActivity(), getResources().getString(R.string.try_again), Toast.LENGTH_LONG).show();
                    getActivity().finishAfterTransition();
                }
            }
        }

        private class getDatabase extends AsyncTask<String, Long, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... args) {
                // 전달된 URL 사용 작업
                StringBuffer response = new StringBuffer();
                try  {

                    String url = args[0].toString();
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    // optional default is GET
                    con.setRequestMethod("GET");

                    //add request header 헤더를 만들어주는것.
                    con.setRequestProperty("User-Agent", "Chrome/version");
                    con.setRequestProperty("Accept-Charset", "UTF-8");
                    con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                    int responseCode = con.getResponseCode();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();


                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response.toString();
            }


            @Override
            protected void onProgressUpdate(Long... progress) {
                // 파일 다운로드 퍼센티지 표시 작업
            }

            @Override
            protected void onPostExecute(String result) {
                // doInBackground 에서 받아온 total 값 사용 장소
                if(result.contains("fail")){
                    Toast.makeText(getActivity(), getResources().getString(R.string.try_again), Toast.LENGTH_LONG).show();
                    getActivity().finishAfterTransition();
                }
                else{
                    Toast.makeText(getActivity(), "activated", Toast.LENGTH_LONG).show();
                    Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
                    serviceIntent.putExtra("data_url", result);
                    getActivity().startService(serviceIntent);
                    getActivity().finishAfterTransition();
                }
            }
        }

    }
}
