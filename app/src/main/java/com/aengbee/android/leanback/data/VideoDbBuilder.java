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
package com.aengbee.android.leanback.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Rating;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.aengbee.android.leanback.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class VideoDbBuilder {
    public static final String TAG_MEDIA = "videos";
    public static final String TAG_GOOGLE_VIDEOS = "googlevideos";
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_STUDIO = "studio";
    public static final String TAG_SOURCES = "sources";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_CARD_THUMB = "card";
    public static final String TAG_BACKGROUND = "background";
    public static final String TAG_TITLE = "title";
    public static final String TAG_RATING_SCORE = "rating";
    public static final String TAG_PLAYABLE = "playable";


    private static final String TAG = "VideoDbBuilder";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */
    public VideoDbBuilder() {

    }

    public VideoDbBuilder(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull List<ContentValues> fetch(String url)
            throws IOException, JSONException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

        String strJson = sharedPreferences.getString("serverList","null");
        Log.d(TAG, "fetch: 0"+strJson);
        JSONObject serverList;
        int serverId = 0;

        if(strJson=="null" || strJson.equals("{}")){
            serverList = new JSONObject();
            Log.d(TAG, "fetch: 1"+strJson);
            serverId = 0;
        }
        else{
            serverList = new JSONObject(strJson);
            Log.d(TAG, "fetch: 2"+strJson);
            serverId = Integer.parseInt(serverList.names().getString(serverList.names().length()-1))+1;
        }


        serverList.put(String.valueOf(serverId), url);
        Log.d("2222", "onCreateActions: "+ serverList);
        sharedPreferencesEditor.putString("serverList", serverList.toString());
        sharedPreferencesEditor.apply();

        if(url.toLowerCase().startsWith("http")){
            JSONObject videoData = fetchJSON(url);
            return buildMedia(videoData,serverId);
        } else {
            JSONObject videoData = fetchJSONfromResource(url);
            return buildMedia(videoData,serverId);
        }
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
    public List<ContentValues> buildMedia(JSONObject jsonObj, int serverId) throws JSONException {

        JSONArray categoryArray = jsonObj.getJSONArray(TAG_GOOGLE_VIDEOS);
        List<ContentValues> videosToInsert = new ArrayList<>();
        /*
        if(categoryArray.getJSONObject(0).getString("category").equals("KY")){
            JSONObject add = new JSONObject("{\"description\":\"아이템 추가\",\"sources\":[\"http://fytoz.asuscomm.com/4TB/TJ/48/48288.html\"],\"card\":\"http://fytoz.asuscomm.com/android-tv/album/000/000/add.jpg\",\"background\":\"http://fytoz.asuscomm.com/android-tv/artist/000/000/0.jpg\",\"title\":\"검색 기능\",\"studio\":\"KY\"}");
            categoryArray.getJSONObject(0).getJSONArray("videos").put(add);
        }
        */
        for (int i = 0; i < categoryArray.length(); i++) {
            JSONArray videoArray;

            JSONObject category = categoryArray.getJSONObject(i);
            String categoryName = category.getString(TAG_CATEGORY);
            videoArray = category.getJSONArray(TAG_MEDIA);

            for (int j = 0; j < videoArray.length(); j++) {
                JSONObject video = videoArray.getJSONObject(j);

                // If there are no URLs, skip this video entry.
                JSONArray urls = video.optJSONArray(TAG_SOURCES);
                if (urls == null || urls.length() == 0) {
                    continue;
                }

                String title = video.optString(TAG_TITLE);
                String description = video.optString(TAG_DESCRIPTION);
                String videoUrl = (String) urls.get(0); // Get the first video only.
                String bgImageUrl = video.optString(TAG_BACKGROUND);
                String cardImageUrl = video.optString(TAG_CARD_THUMB);
                String studio = video.optString(TAG_STUDIO);

                float rating = Float.parseFloat(video.optString(TAG_RATING_SCORE,"0"));
                if(rating!=0)
                    rating = rating/2f;
                String content_type = "video/mp4";
                if(videoUrl.toLowerCase().endsWith("html"))
                    content_type="text/html";
                int playable = Integer.parseInt(video.optString(TAG_PLAYABLE, "1"));

                ContentValues videoValues = new ContentValues();
                videoValues.put(VideoContract.VideoEntry.COLUMN_CATEGORY, categoryName);
                videoValues.put(VideoContract.VideoEntry.COLUMN_NAME, title);
                videoValues.put(VideoContract.VideoEntry.COLUMN_DESC, description);
                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_CARD_IMG, cardImageUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, bgImageUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_STUDIO, studio);
                videoValues.put(VideoContract.VideoEntry.COLUMN_IS_LIVE, serverId); //json_url_id
                videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_SCORE, rating);
                videoValues.put(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, content_type);
                videoValues.put(VideoContract.VideoEntry.COLUMN_DURATION, playable); //playable 1 or not 0

                // Fixed defaults.
                videoValues.put(VideoContract.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG, "2.0");
                videoValues.put(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, 2014);
                videoValues.put(VideoContract.VideoEntry.COLUMN_RATING_STYLE,
                        Rating.RATING_5_STARS);

                if (mContext != null) {
                    videoValues.put(VideoContract.VideoEntry.COLUMN_PURCHASE_PRICE,
                            mContext.getResources().getString(R.string.buy_2));
                    videoValues.put(VideoContract.VideoEntry.COLUMN_RENTAL_PRICE,
                            mContext.getResources().getString(R.string.rent_2));
                    videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                            mContext.getResources().getString(R.string.global_search));
                }

                // TODO: Get these dimensions.
                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_WIDTH, 1280);
                videoValues.put(VideoContract.VideoEntry.COLUMN_VIDEO_HEIGHT, 720);

                videosToInsert.add(videoValues);
            }
        }
        return videosToInsert;
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
        BufferedReader reader = null;
        java.net.URL url = new java.net.URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } finally {
            urlConnection.disconnect();
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "JSON feed closed", e);
                }
            }
        }
    }

    private JSONObject fetchJSONfromResource(String resource) throws JSONException, IOException{
        BufferedReader reader = null;
        try {
            InputStream is = mContext.getAssets().open(resource);
            reader = new BufferedReader(new InputStreamReader(is,
                    "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);

        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "JSON feed closed", e);
                    e.printStackTrace();
                }
            }
        }

    }
}
