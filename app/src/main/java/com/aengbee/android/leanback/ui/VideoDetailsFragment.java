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

import android.Manifest;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.DetailsOverviewLogoPresenter;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.aengbee.android.leanback.R;
import com.aengbee.android.leanback.data.VideoContract;
import com.aengbee.android.leanback.model.Video;
import com.aengbee.android.leanback.model.VideoCursorMapper;
import com.aengbee.android.leanback.presenter.CardPresenter;
import com.aengbee.android.leanback.presenter.DetailsDescriptionPresenter;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStoragePublicDirectory;

/*
 * VideoDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
public class VideoDetailsFragment extends DetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int NO_NOTIFICATION = -1;
    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    // ID for loader that loads related videos.
    private static final int RELATED_VIDEO_LOADER = 1;

    // ID for loader that loads the video from global search.
    private int mGlobalSearchVideoId = 2;

    private Video mSelectedVideo;
    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private CursorObjectAdapter mVideoCursorAdapter;
    private FullWidthDetailsOverviewSharedElementHelper mHelper;
    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();
    private View mLayout;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
    private static final String TAG = "VideoDetailsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(mVideoCursorMapper);

        mSelectedVideo = (Video) getActivity().getIntent()
                .getParcelableExtra(VideoDetailsActivity.VIDEO);

        if (mSelectedVideo != null || !hasGlobalSearchIntent()) {
            removeNotification(getActivity().getIntent()
                    .getIntExtra(VideoDetailsActivity.NOTIFICATION_ID, NO_NOTIFICATION));
            setupAdapter();
            setupDetailsOverviewRow();
            setupMovieListRow();
            updateBackground(mSelectedVideo.bgImageUrl);

            // When a Related Video item is clicked.
            setOnItemViewClickedListener(new ItemViewClickedListener());
        }
    }

    private void removeNotification(int notificationId) {
        if (notificationId != NO_NOTIFICATION) {
            NotificationManager notificationManager = (NotificationManager) getActivity()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    /**
     * Check if there is a global search intent. If there is, load that video.
     */
    private boolean hasGlobalSearchIntent() {
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);

        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            String videoId = intentData.getLastPathSegment();

            Bundle args = new Bundle();
            args.putString(VideoContract.VideoEntry._ID, videoId);
            getLoaderManager().initLoader(mGlobalSearchVideoId++, args, this);
            return true;
        }
        return false;
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {
        Glide.with(this)
                .load(uri)
                .asBitmap()
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<Bitmap>(mMetrics.widthPixels, mMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource,
                            GlideAnimation<? super Bitmap> glideAnimation) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void ACTION_WATCH_TRAILER(){
        Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
        intent.putExtra(VideoDetailsActivity.VIDEO, mSelectedVideo);
        startActivity(intent);
    }

    private void ACTION_RENT(){
        Uri uri = Uri.parse(mSelectedVideo.videoUrl.toString());
        String Number = uri.getQueryParameter("i");
        Number = String.format("%05d", Integer.parseInt(Number));
        String company = uri.getQueryParameter("v");
        company = company.replace("cs","CS").replace("ky","audio").replace("tj","TJ");
        String duration = mSelectedVideo.duration.toString();
        start(company,Number,duration);
    }

    private void ACTION_SWITCH(boolean mode, int actionID){
        if(!mode){
            if(actionID==ACTION_WATCH_TRAILER)
                ACTION_WATCH_TRAILER();
            else if(actionID==ACTION_RENT)
                ACTION_RENT();
        }
        else {
            if(actionID==ACTION_WATCH_TRAILER)
                ACTION_RENT();
            else if(actionID==ACTION_RENT)
                ACTION_WATCH_TRAILER();
        }
    }
    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter(),
                        new MovieDetailsOverviewLogoPresenter());

        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.detail_view_background));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        mHelper = new FullWidthDetailsOverviewSharedElementHelper();
        mHelper.setSharedElementEnterTransition(getActivity(),
                VideoDetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(mHelper);
        detailsPresenter.setParticipatingEntranceTransition(false);
        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_WATCH_TRAILER) {
                    ACTION_SWITCH(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_mixing),false),ACTION_WATCH_TRAILER);
                } else if (action.getId() == ACTION_RENT) {
                    ACTION_SWITCH(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_mixing),false),ACTION_RENT);
                } else if (action.getId() == ACTION_BUY ) {
                    setSelectedPosition(1);
                } else {
                    Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RELATED_VIDEO_LOADER: {
                String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
                String desc = args.getString(VideoContract.VideoEntry.COLUMN_DESC);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        //VideoContract.VideoEntry.COLUMN_CATEGORY + " = ? AND " +
                        VideoContract.VideoEntry.COLUMN_DESC + " LIKE ? AND "+
                        VideoContract.VideoEntry.COLUMN_RATING_SCORE + " != ? ",
                        new String[]{"%" + desc + "%", "0"},
                        VideoContract.VideoEntry.COLUMN_NAME// Default sort order
                );
            }
            default: {
                // Loading video from global search.
                String videoId = args.getString(VideoContract.VideoEntry._ID);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry._ID + " = ?",
                        new String[]{videoId},
                        null
                );
            }
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToNext()) {
            switch (loader.getId()) {
                case RELATED_VIDEO_LOADER: {
                    mVideoCursorAdapter.changeCursor(cursor);
                    break;
                }
                default: {
                    // Loading video from global search.
                    mSelectedVideo = (Video) mVideoCursorMapper.convert(cursor);

                    setupAdapter();
                    setupDetailsOverviewRow();
                    setupMovieListRow();
                    updateBackground(mSelectedVideo.bgImageUrl);

                    // When a Related Video item is clicked.
                    setOnItemViewClickedListener(new ItemViewClickedListener());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
            public ViewHolder(View view) {
                super(view);
            }

            public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
                return mParentPresenter;
            }

            public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
                return mParentViewHolder;
            }
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

            Resources res = parent.getResources();
            int width = res.getDimensionPixelSize(R.dimen.detail_thumb_width);
            int height = res.getDimensionPixelSize(R.dimen.detail_thumb_height);
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);
            imageView.setImageDrawable(row.getImageDrawable());
            if (isBoundToImage((ViewHolder) viewHolder, row)) {
                MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                        (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }

    private void setupDetailsOverviewRow() {
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedVideo);

        Glide.with(this)
                .load(mSelectedVideo.cardImageUrl)
                .asBitmap()
                .dontAnimate()
                .error(R.drawable.movie)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource,
                            GlideAnimation glideAnimation) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });

        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();

        adapter.set(ACTION_WATCH_TRAILER, new Action(ACTION_WATCH_TRAILER, getResources()
                .getString(R.string.watch_trailer_1),
                getResources().getString(R.string.watch_trailer_2)));
        if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_mixing),false)==true)
            adapter.set(ACTION_RENT, new Action(ACTION_RENT, getResources().getString(R.string.rent_1), getResources().getString(R.string.rent_2)));
        adapter.set(ACTION_BUY, new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
                getResources().getString(R.string.buy_2)));

        row.setActionsAdapter(adapter);

        mAdapter.add(row);
    }

    private void setupMovieListRow() {
        String subcategories[] = {getString(R.string.related_movies)};

        // Generating related video list.
        String category = mSelectedVideo.category;
        String desc = mSelectedVideo.description;
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
        args.putString(VideoContract.VideoEntry.COLUMN_DESC, desc);
        getLoaderManager().initLoader(RELATED_VIDEO_LOADER, args, this);

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, mVideoCursorAdapter));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }



    private List<String> getListFiles(File parentDir, String fileNameToSearch) {
        ArrayList<String> inFiles = new ArrayList<String>();
        File[] files = parentDir.listFiles();
        if(files!=null){
            for (File file : files) {

                if (file.isDirectory()) {
                    if(file.getAbsolutePath().toString().toLowerCase().endsWith(fileNameToSearch.toLowerCase()) || file.getName().toString().toUpperCase().contains(fileNameToSearch.toUpperCase())){
                        inFiles.add(file.getName().toString());
                    }else {
                        inFiles.addAll(getListFiles(file, fileNameToSearch));
                    }
                } else {
                    if(file.getAbsolutePath().toString().toLowerCase().endsWith(fileNameToSearch.toLowerCase()) || file.getName().toString().toUpperCase().contains(fileNameToSearch.toUpperCase())){
                        inFiles.add(file.getName().toString());
                    }
                }

            }
        }
        return inFiles;
    }

    public String chooseVideo(String durationSong){
        int intDurationSong = Integer.parseInt(durationSong);
        List<String> fileList = getListFiles(new File(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES),"mp4");
        List<String> possibleList = new ArrayList<String>();
        if(fileList.size()>0){
            for(String file : fileList){

                int length = file.contains("mp4")? 900:getDurationVideo(new File(file));
                if(length>intDurationSong)
                    possibleList.add(file);
            }
        }
        String result = "";
        if(fileList==null || fileList.size() == 0 || possibleList == null || possibleList.size()<1) {
            result = Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/source.mp4";
        }
        else{
            if(possibleList.size()>1)
                Collections.shuffle(possibleList);
            result = String.format(getExternalStoragePublicDirectory(DIRECTORY_MOVIES).toString()+"/%s",possibleList.get(0));
        }

        return result;

    }



    public int getDurationVideo(File videoFile){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getActivity(), Uri.fromFile(videoFile));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int sec = (int) Long.parseLong(time)/1000;
        return sec;
    }

    public static boolean copyFile(String from, String to) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(from);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(from);
                FileOutputStream fs = new FileOutputStream(to);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void getFiles(String from, String toFolder, String toFile) throws IOException {
        if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_USB),false)==true){
            String to = toFolder+"/"+toFile;
            Files.copy(new File(from), new File(to));
        }
        else {
            new DownloadTask(getActivity()).execute(toFolder, toFile, from);
        }
    }

    public void start(String company, String number, String duration){
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example, if the request has been denied previously.


              Toast.makeText(getActivity(), "we need read and write permission on external storage for downloading the videos and the mixing", Toast.LENGTH_LONG).show();
            } else {
                // Contact permissions have not been granted yet. Request them directly.
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS_STORAGE, 1);
            }

        } else {

            File tempaudio = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/temp.mp3");
            File templyrics = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/temp.ass");
            if (tempaudio.exists()) {
                tempaudio.delete();
            }

            if (templyrics.exists()) {
                templyrics.delete();
            }


            if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_USB),false)==true) {
                String storagePath = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("USB_path","null");
                File lyricsFrom = new File(String.format(storagePath + "/%s/%s/%s.ass", company, number.substring(0, 2), number));
                File lyricsTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/temp.ass");
                //Toast.makeText(getActivity(),lyricsFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                File audioFrom = new File(String.format(storagePath + "/%s/%s/%s.mp3", company, number.substring(0, 2), number));
                File audioTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/temp.mp3");
                //Toast.makeText(getActivity(),audioFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                try{
                    Files.copy(lyricsFrom,lyricsTo);
                    Files.copy(audioFrom,audioTo);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                File source = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/source.mp4");
                if(!source.exists()){
                    File videoFrom = new File(String.format(storagePath + "/%s", "source.mp4"));
                    File videoTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES + "/source.mp4");
                    Toast.makeText(getActivity(),videoFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                    try {
                        Files.copy(videoFrom,videoTo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            else{
                String downloadURL = String.format("http://fytoz.asuscomm.com/4TB/%s/%s/%s.mp3",company,number.substring(0,2),number);
                new DownloadTask(getActivity()).execute(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES, "temp.ass", downloadURL.replace(".mp3",".ass"), "SearchFragment");
                new DownloadTask(getActivity()).execute(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_MOVIES, "temp.mp3", downloadURL, "SearchFragment");

            }


            //Log.d("dxd", "start: "+downloadURL);

            String cmdFormat="-i %s -i %s -c copy -map 0:v:0 -map 1:a:0 %s-y %s";
            //List<String> fileList = getListFiles(getExternalStoragePublicDirectory(DIRECTORY_MOVIES),"mp4");
            //List<String> fileList = getListFiles(new File(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES),"mp4");

            String filePath = chooseVideo(duration);
            int lengthofFile = filePath.contains("source.mp4")? 900: getDurationVideo(new File(filePath));

            //String joined = TextUtils.join(", ", fileList);
            //Toast.makeText(getActivity(), joined, Toast.LENGTH_SHORT).show();
            //Log.d("dxd", "start: "+duration+"|"+lengthofFile);
            String shortest = Integer.parseInt(duration) > lengthofFile ? "-shortest " : "-shortest ";
            //Toast.makeText(getActivity(), shortest+duration+"|"+lengthofFile, Toast.LENGTH_LONG).show();
            String audioPath = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES+"/temp.mp3";
            String cmd1 = String.format(cmdFormat,
                    filePath,
                    audioPath,
                    shortest,
                    Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/temp.mkv"
            );

            String cmd2 = String.format(cmdFormat,
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_key_USB),false) ? Environment.getExternalStorageDirectory().getPath()+"/"+ Environment.DIRECTORY_MOVIES+"/source.mp4" : "http://fytoz.asuscomm.com/4TB/audio/source.mp4" ,
                    audioPath,
                    shortest,
                    Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/temp.mkv"
            );

            //Log.d("kkk:", Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES);
            String[] command1 = cmd1.split(" ");
            String[] command2 = cmd2.split(" ");


            if(new File(filePath).exists() && tempaudio.exists() && templyrics.exists()){
                //Toast.makeText(getActivity(), cmd1, Toast.LENGTH_LONG).show();
                ((VideoDetailsActivity)getActivity()).execFFmpegBinary(command1);
            }
            else{
                //Toast.makeText(getActivity(), cmd2, Toast.LENGTH_LONG).show();
                ((VideoDetailsActivity)getActivity()).execFFmpegBinary(command2);
            }
        }

    }
}
