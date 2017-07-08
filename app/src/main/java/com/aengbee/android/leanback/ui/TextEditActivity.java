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
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class TextEditActivity extends Activity {
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
            String title = "USB Storage";
            String description = "storage path";
            Drawable icon = getActivity().getDrawable(R.drawable.launch_screen);
            return new GuidanceStylist.Guidance(title, description, "", icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            GuidedAction enterUsername = new GuidedAction.Builder()
                    .title("Path")
                    .description(sharedPreferences.getString("USB_path","/storage/usbotg/usbotg-sda5"))
                    .descriptionEditable(true)
                    .build();

            GuidedAction login = new GuidedAction.Builder()
                    .id(CONTINUE)
                    .title(getString(R.string.guidedstep_continue))
                    .build();
            actions.add(enterUsername);

            actions.add(login);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == CONTINUE) {
                // TODO Authenticate your account
                // Assume the user was logged in
                String id = getActions().get(0).getDescription().toString();
                SharedPreferences.Editor sharedPreferencesEditor =
                        android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                sharedPreferencesEditor.putString("USB_path", id);
                sharedPreferencesEditor.apply();
                Toast.makeText(getActivity(), id, Toast.LENGTH_LONG).show();
                getActivity().finishAfterTransition();

            }
        }


    }
}
