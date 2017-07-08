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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;
import static com.aengbee.android.leanback.utils.ConvertEntoKo.concatOldNew;
import static com.aengbee.android.leanback.utils.ConvertEntoKo.engToKor;
import static com.aengbee.android.leanback.utils.ConvertEntoKo.splitKor2Eng;

import com.aengbee.android.leanback.BuildConfig;
import com.aengbee.android.leanback.card.presenters.CardPresenterSelector;
import com.aengbee.android.leanback.model.Card;
import com.aengbee.android.leanback.model.CardRow;
import com.aengbee.android.leanback.utils.CardListRow;
import com.aengbee.android.leanback.utils.InputTables;
import com.aengbee.android.leanback.utils.KoreanAutomata;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v17.leanback.app.*;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.DividerRow;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SearchBar;
import android.support.v17.leanback.widget.SectionRow;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.aengbee.android.leanback.R;
import com.aengbee.android.leanback.utils.Utils;
import com.aengbee.android.leanback.data.VideoContract;
import com.aengbee.android.leanback.model.Video;
import com.aengbee.android.leanback.model.VideoCursorMapper;
import com.aengbee.android.leanback.player.ExtractorRendererBuilder;
import com.aengbee.android.leanback.player.VideoPlayer;
import com.aengbee.android.leanback.presenter.CardPresenter;
import com.google.gson.Gson;
import com.aengbee.android.leanback.utils.ConvertEntoKo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/*
 * The PlaybackOverlayFragment class handles the Fragment associated with displaying the UI for the
 * media controls such as play / pause / skip forward / skip backward etc.
 *
 * The UI is updated through events that it receives from its MediaController
 */
public class PlaybackOverlayFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, TextureView.SurfaceTextureListener, VideoPlayer.Listener
{
    private static final String TAG = "PlaybackOverlayFragment";
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_NONE;
    private static final String AUTO_PLAY = "auto_play";
    private static final Bundle mAutoPlayExtras = new Bundle();
    private static final int RECOMMENDED_VIDEOS_LOADER = 1;
    private static final int QUEUE_VIDEOS_LOADER = 2;
    private static final int SEARCH_VIDEOS_LOADER = 3;
    private static PlaybackControlsRow controlsRow;
    private static int mode = 0;

    private static final String SPECIAL_CHARACTERS = "`~!@#$%^&*()-_=+|\\;:'\",<.>/?";
    private KoreanAutomata kauto;
    private static StringBuilder mComposing = new StringBuilder();
    public static int currentKeyboard = 0;
    private static final int mKoreanKeyboard = 0;
    private static final int mQwertyKeyboard = 1;
    private static final int mSymbolsKeyboard = 2;
    public static String mQuery;
    public static List mBook = new ArrayList<String>();
    private ArrayObjectAdapter mListRowAdapter;

    static {
        mAutoPlayExtras.putBoolean(AUTO_PLAY, true);
    }

    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();
    private int mSpecificVideoLoaderId = 4;
    private int mQueueIndex = -1;
    private Video mSelectedVideo; // Video is the currently playing Video and its metadata.
    private ArrayObjectAdapter mRowsAdapter;
    private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();
    private CursorObjectAdapter mVideoCursorAdapter;
    private MediaSessionCompat mSession; // MediaSession is used to hold the state of our media playback.
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
    private MediaController mMediaController;
    private PlaybackControlHelper mGlue;
    private MediaController.Callback mMediaControllerCallback;
    private VideoPlayer mPlayer;
    private boolean mIsMetadataSet = false;
    private AudioManager mAudioManager;
    private boolean mHasAudioFocus;
    private boolean mPauseTransient;
    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            abandonAudioFocus();
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (mGlue.isMediaPlaying()) {
                                pause();
                                mPauseTransient = true;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mPlayer.mute(true);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            if (mPauseTransient) {
                                play();
                            }
                            mPlayer.mute(false);
                            break;
                    }
                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = this;

        createMediaSession();
    }

    @Override
    public void onStop() {
        super.onStop();

        mSession.release();
        releasePlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        mSession.release();
        releasePlayer();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set up UI
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        if (!updateSelectedVideo(video)) {
            return;
        }

        mGlue = new PlaybackControlHelper(getActivity(), this, mSelectedVideo);
        PlaybackControlsRowPresenter controlsRowPresenter = mGlue.createControlsRowAndPresenter();

        mMediaControllerCallback = mGlue.createMediaControllerCallback();

        mMediaController = getActivity().getMediaController();
        mMediaController.registerCallback(mMediaControllerCallback);

        ClassPresenterSelector ps = new ClassPresenterSelector();

        ps.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);
        ps.addClassPresenter(CardListRow.class, new ListRowPresenter());
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        kauto = new KoreanAutomata();
        if(!kauto.IsKoreanMode())
            kauto.ToggleMode();

        mRowsAdapter = new ArrayObjectAdapter(ps);
        controlsRow = mGlue.getControlsRow();

        PlayMode();
        setAdapter(mRowsAdapter);
        startPlaying();
        getRowsFragment().setAlignment(160);

    }


    @Override
    public void onResume() {
        super.onResume();
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        if (!updateSelectedVideo(video)) {
            return;
        }

        startPlaying();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        // Initialize instance variables.
        TextureView textureView = (TextureView) getActivity().findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        setBackgroundType(BACKGROUND_TYPE);
        // Set up listener.
        setOnItemViewClickedListener(new ItemViewClickedListener());

    }

    private boolean updateSelectedVideo(Video video) {
        Intent intent = new Intent(getActivity().getIntent());
        intent.putExtra(VideoDetailsActivity.VIDEO, video);
        if (mSelectedVideo != null && mSelectedVideo.equals(video)) {
            return false;
        }
        mSelectedVideo = video;

        PendingIntent pi = PendingIntent.getActivity(
                getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        return true;
    }

    @TargetApi(VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();
        if (mGlue.isMediaPlaying()) {
            boolean isVisibleBehind = getActivity().requestVisibleBehind(true);
            boolean isInPictureInPictureMode =
                   PlaybackOverlayActivity.supportsPictureInPicture(getActivity())
                            && getActivity().isInPictureInPictureMode();
            if (!isVisibleBehind && !isInPictureInPictureMode) {
                pause();
            }
        } else {
            getActivity().requestVisibleBehind(false);
        }

    }

    @Override
    public void onPictureInPictureModeChanged(boolean pictureInPictureMode) {
        if (pictureInPictureMode) {
            mGlue.setFadingEnabled(false);
            setFadingEnabled(true);
            fadeOut();
        } else {
            mGlue.setFadingEnabled(true);
        }
    }

    public String queryNormalizer(String query){
        String normalized = "";
        if(query!=null && !query.isEmpty()){
            normalized = query.replaceAll(SPECIAL_CHARACTERS,"").toLowerCase();
            normalized = normalized.replace(" ","");
        }

        return "%"+normalized+"%";
    }

    public CursorLoader CursorLoaderNormalizer(String query){
        query += "";
        return new CursorLoader(
                getActivity(),
                VideoContract.VideoEntry.CONTENT_URI,
                null, // Projection to return - null means return all fields.
                //VideoContract.VideoEntry.COLUMN_CATEGORY + " = ? AND " +
                " ( lower ( trim ( " + VideoContract.VideoEntry.COLUMN_NAME + " , ? ) ) LIKE ? OR " +
                        " lower ( trim ( " + VideoContract.VideoEntry.COLUMN_DESC + " , ? ) ) LIKE ? ) AND " +
                        VideoContract.VideoEntry.COLUMN_RATING_SCORE + " == ? AND " +
                        VideoContract.VideoEntry.COLUMN_CATEGORY + " == ? ",
                // Selection clause is category.
                new String[]{SPECIAL_CHARACTERS, "%" + query + "%", SPECIAL_CHARACTERS, "%" + query + "%", "1", "KY"},
                VideoContract.VideoEntry.COLUMN_NAME// Default sort order
        );

    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        int state = args.getInt("state", 4);
        switch (state) {
            case RECOMMENDED_VIDEOS_LOADER: {
              return CursorLoaderNormalizer(args.getString(VideoContract.VideoEntry.COLUMN_DESC,""));
            }
            case QUEUE_VIDEOS_LOADER: {
                //Long videoId = args.getLong(VideoContract.VideoEntry._ID);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null, // Projection to return - null means return all fields.
                        VideoContract.VideoEntry._ID + " IN ( ? ) OR " +
                        VideoContract.VideoEntry.COLUMN_CARD_IMG + " LIKE ? ", // Selection clause is id.
                        // Selection clause is category.
                        new String[]{TextUtils.join(",", mBook),"%add.jpg"}, // Select based on the id.
                        null//"INSTR (',"+TextUtils.join(",", mBook)+",', ',' || "+ VideoContract.VideoEntry._ID + " || ',')" // Default sort order
                );
            }
            case SEARCH_VIDEOS_LOADER: {
                // Loading a specific video.
                return CursorLoaderNormalizer(mQuery);
            }
            default: {
                if(mQuery==null){
                    return new CursorLoader(
                            getActivity(),
                            VideoContract.VideoEntry.CONTENT_URI,
                            null, // Projection to return - null means return all fields.
                            VideoContract.VideoEntry._ID + " IN ( ? ) OR " +
                                    VideoContract.VideoEntry.COLUMN_CARD_IMG + " LIKE ? ", // Selection clause is id.
                            // Selection clause is category.
                            new String[]{TextUtils.join(",", mBook),"%add.jpg"}, // Select based on the id.
                            null//"INSTR (',"+TextUtils.join(",", mBook)+",', ',' || "+ VideoContract.VideoEntry._ID + " || ',')" // Default sort order
                    );
                }
                else
                    return CursorLoaderNormalizer(mQuery);
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {

                if(cursor.getCount() == mBook.size()+1) {
                    //mQueue.clear();
                    while (!cursor.isAfterLast()) {
                        Video v = (Video) mVideoCursorMapper.convert(cursor);

                        // Set the queue index to the selected video.
                        if (v.id == mSelectedVideo.id) {
                            mQueueIndex = mQueue.size();
                        }

                        // Add the video to the queue.
                        MediaSessionCompat.QueueItem item = getQueueItem(v);
                        if (!mQueue.contains(item))
                            mQueue.add(item);
                        //mQueue.add(item);

                        cursor.moveToNext();
                    }

                    mSession.setQueue(mQueue);
                    mSession.setQueueTitle(getString(R.string.queue_name));
                    mVideoCursorAdapter.changeCursor(cursor);

                }
                else{
                    mVideoCursorAdapter.changeCursor(cursor);
                }
            }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private void setPosition(long position) {
        if (position > mPlayer.getDuration()) {
            mPlayer.seekTo(mPlayer.getDuration());
        } else if (position < 0) {
            mPlayer.seekTo(0L);
        } else {
            mPlayer.seekTo(position);
        }
    }

    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSessionCompat(getActivity(), "LeanbackSampleApp");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
            mSession.setActive(true);

            // Set the Activity's MediaController used to invoke transport controls / adjust volume.
            try {
                ((FragmentActivity) getActivity()).setSupportMediaController(
                        new MediaControllerCompat(getActivity(), mSession.getSessionToken()));
                setPlaybackState(PlaybackState.STATE_NONE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private MediaSessionCompat.QueueItem getQueueItem(Video v) {
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setDescription(v.description)
                .setMediaId(v.id + "")
                .setIconUri(Uri.parse(v.cardImageUrl))
                .setMediaUri(Uri.parse(v.videoUrl))
                .setSubtitle(v.studio)
                .setTitle(v.title)
                .build();
        return new MediaSessionCompat.QueueItem(desc, v.id);
    }

    public long getBufferedPosition() {
        if (mPlayer != null) {
            return mPlayer.getBufferedPosition();
        }
        return 0L;
    }

    public long getCurrentPosition() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition();
        }
        return 0L;
    }

    public long getDuration() {
        if (mPlayer != null) {
            return mPlayer.getDuration();
        }
        return ExoPlayer.UNKNOWN_TIME;
    }

    private long getAvailableActions(int nextState) {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                PlaybackState.ACTION_FAST_FORWARD |
                PlaybackState.ACTION_REWIND |
                PlaybackState.ACTION_PAUSE;

        if (nextState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void play() {
        // Request audio focus whenever we resume playback
        // because the app might have abandoned audio focus due to the AUDIOFOCUS_LOSS.
        requestAudioFocus();

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }
        if (!mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().start();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        }
    }

    private void pause() {
        mPauseTransient = false;

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }
        if (mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private void requestAudioFocus() {
        if (mHasAudioFocus) {
            return;
        }
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mHasAudioFocus = true;
        } else {
            pause();
        }
    }

    private void abandonAudioFocus() {
        mHasAudioFocus = false;
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    void updatePlaybackRow() {
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    /**
     * Creates a ListRow for related videos.
     */
    private void PlayMode() {
        mode = 0;
        if(mRowsAdapter.size()>0)
            mRowsAdapter.clear();

        mRowsAdapter.add(controlsRow);

        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        if(mSpecificVideoLoaderId==4)
            mBook.add(String.valueOf(mSelectedVideo.id));
        Bundle args = new Bundle();
        //args.putString(VideoContract.VideoEntry.COLUMN_DESC, mSelectedVideo.description);
        args.putInt("state", QUEUE_VIDEOS_LOADER);
        getLoaderManager().restartLoader(mSpecificVideoLoaderId++, args, mCallbacks);
        HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        mRowsAdapter.add(new ListRow(header, mVideoCursorAdapter));

        updatePlaybackRow();

    }

    private void SearchMode() {
        mode = 1;
        mRowsAdapter.clear();
        mRowsAdapter.add(createRows(currentKeyboard));

        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        Bundle args = new Bundle();
        args.putInt("state", SEARCH_VIDEOS_LOADER);
        mQuery = mSelectedVideo.description;
        getLoaderManager().restartLoader(mSpecificVideoLoaderId++, args, this);

        HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        mRowsAdapter.add(new ListRow(header, mVideoCursorAdapter));
        mQuery = null;

    }

    private Row createRows(int i) {
        String json = Utils.inputStreamToString(getResources().openRawResource(R.raw.keyboard_layout));
        CardRow[] rows = new Gson().fromJson(json, CardRow[].class);
        return createCardRow(rows[i]);
    }

    private Row createCardRow(final CardRow cardRow) {
        switch (cardRow.getType()) {
            case CardRow.TYPE_SECTION_HEADER:
                return new SectionRow(new HeaderItem(cardRow.getTitle()));
            case CardRow.TYPE_DIVIDER:
                return new DividerRow();
            case CardRow.TYPE_DEFAULT:
            default:
                // Build main row using the ImageCardViewPresenter.
                PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
                for (Card card : cardRow.getCards()) {
                    listRowAdapter.add(card);
                }

                return new CardListRow(new HeaderItem(cardRow.getTitle()), listRowAdapter, cardRow);

        }
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(getActivity(), "ExoVideoPlayer");
        Uri contentUri = Uri.parse(mSelectedVideo.videoUrl);
        int contentType = Util.inferContentType(contentUri.getLastPathSegment());

        switch (contentType) {
            case Util.TYPE_OTHER: {
                return new ExtractorRendererBuilder(getActivity(), userAgent, contentUri);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + contentType);
            }
        }
    }

    private void preparePlayer() {
        if (mPlayer == null) {
            mPlayer = new VideoPlayer(getRendererBuilder());
            mPlayer.addListener(this);
            mPlayer.seekTo(0L);
            mPlayer.prepare();
        } else {
            mPlayer.stop();
            mPlayer.seekTo(0L);
            mPlayer.setRendererBuilder(getRendererBuilder());
            mPlayer.prepare();
        }
        mPlayer.setPlayWhenReady(true);

        requestAudioFocus();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        abandonAudioFocus();
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // Do nothing.
                break;
            case ExoPlayer.STATE_ENDED:
                mIsMetadataSet = false;
                mMediaController.getTransportControls().skipToNext();
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.
                break;
            case ExoPlayer.STATE_PREPARING:
                mIsMetadataSet = false;
                break;
            case ExoPlayer.STATE_READY:
                // Duration is set here.
                if (!mIsMetadataSet) {
                    updateMetadata(mSelectedVideo);
                    mIsMetadataSet = true;
                }
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        // Do nothing.
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mPlayer != null) {
            mPlayer.setSurface(new Surface(surfaceTexture));
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing.
    }

    private int getPlaybackState() {
        Activity activity = getActivity();

        if (activity != null) {
            PlaybackState state = activity.getMediaController().getPlaybackState();
            if (state != null) {
                return state.getState();
            } else {
                return PlaybackState.STATE_NONE;
            }
        }
        return PlaybackState.STATE_NONE;
    }

    private void setPlaybackState(int state) {
        long currPosition = getCurrentPosition();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions(state));
        stateBuilder.setState(state, currPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMetadata(final Video video) {
        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, video.id + "");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, video.studio);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                video.description);

        long duration = Utils.getDuration(video.videoUrl);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, video.studio);

        Resources res = getResources();
        int cardWidth = res.getDimensionPixelSize(R.dimen.playback_overlay_width);
        int cardHeight = res.getDimensionPixelSize(R.dimen.playback_overlay_height);

        Glide.with(this)
                .load(Uri.parse(video.cardImageUrl))
                .asBitmap()
                .centerCrop()
                .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                        mSession.setMetadata(metadataBuilder.build());
                    }
                });
    }

    private void playVideo(Video video, Bundle extras) {
        updateSelectedVideo(video);
        preparePlayer();
        setPlaybackState(PlaybackState.STATE_PAUSED);
        if (extras.getBoolean(AUTO_PLAY)) {
            play();
        } else {
            pause();
        }
    }

    private void startPlaying() {
        // Prepare the player and start playing the selected video
        playVideo(mSelectedVideo, mAutoPlayExtras);

        // Start loading videos for the queue
        Bundle args = new Bundle();
        args.putInt("state", QUEUE_VIDEOS_LOADER);
        getLoaderManager().restartLoader(mSpecificVideoLoaderId++, args, mCallbacks);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {

                if(mode == 1){
                    Toast.makeText(getActivity(), "SearchMode", Toast.LENGTH_SHORT).show();
                    kauto.FinishAutomataWithoutInput();
                    //mQuery = null;

                    Video v = (Video) item;
                    // Add the video to the queue.
                    /*
                    MediaSessionCompat.QueueItem items = getQueueItem(v);
                    if(!mQueue.contains(items)){
                        mQueue.add(items);
                        mSession.setQueue(mQueue);
                        mSession.setQueueTitle(getString(R.string.queue_name));
                    }
                    */

                    mBook.add(String.valueOf(((Video) item).id));
                    PlayMode();

                }
                if(mode == 0 && ((Video) item).cardImageUrl.endsWith("add.jpg")){
                    Toast.makeText(getActivity(), "PlayMode&add", Toast.LENGTH_SHORT).show();
                    SearchMode();
                }
                else {
                    Toast.makeText(getActivity(), "PlayMode", Toast.LENGTH_SHORT).show();
                    Video video = (Video) item;
                    if (video.videoUrl.contains(".html") || video.videoUrl.contains(".php")) {
                        getActivity().setContentView(R.layout.activity_playback_webview);
                    } else {
                        Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                        intent.putExtra(VideoDetailsActivity.VIDEO, video);

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                        getActivity().startActivity(intent, bundle);
                    }
                }

            }
            else if (item instanceof Card){
                String key = ((Card) item).getLocalImageResourceName().toString();

                if(key.equals("ic_language_white_48dp")){
                    currentKeyboard++;
                    if(currentKeyboard%3==0){
                        if(!kauto.IsKoreanMode())
                            kauto.ToggleMode();
                    }
                    else{
                        if(kauto.IsKoreanMode())
                            kauto.ToggleMode();
                    }


                    mRowsAdapter.replace(0, createRows(currentKeyboard%3));
                    mRowsAdapter.notifyArrayItemRangeChanged(0,1);
                }
                else if(key.equals("ic_clear_white_48dp")){
                    if (mComposing.length() > 0) {
                        mComposing.setLength(0);
                    }
                    kauto.FinishAutomataWithoutInput();
                    loadQuery();
                    mRowsAdapter.notifyArrayItemRangeChanged(1, 1);
                }
                else if(key.equals("ic_space_bar_white_48dp")) {
                    handleCharacter(' ');
                    loadQuery();
                    mRowsAdapter.notifyArrayItemRangeChanged(1, 1);
                }
                else if(key.equals("ic_backspace_white_48dp")) {
                    handleBackspace();
                    loadQuery();
                    mRowsAdapter.notifyArrayItemRangeChanged(1, 1);
                }
                else {

                    handleCharacter(key2char(key));
                    //mQuery = concatOldNew(mQuery, key);
                    loadQuery();
                    mRowsAdapter.notifyArrayItemRangeChanged(1,1);
                }

            }
            /*
            else{
                Toast.makeText(getActivity(), item.getClass().getName().toString(), Toast.LENGTH_LONG).show();
            }
            */
        }

    }
    private char key2char(String key){
        //String kor = "ㄱㄲㄴㄷㄸㄹㅁᄇㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
        String kor = "ㅁㅠㅊㅇㄷㄹㅎㅗㅑㅓㅏㅣㅡㅜㅐㅔㅂㄱㄴㅅㅕㅍㅈㅌㅛㅋㅃㅉㄸㄲㅆㅒㅖ";
        String eng = "abcdefghijklmnopqrstuvwxyzQWERTOP";

        return kor.contains(key) ? eng.charAt(kor.indexOf(key)) : key.charAt(0);
    }
    private void loadQuery(){

        mQuery = mComposing.toString();
        loadQuery(mQuery);
    }

    private void loadQuery(String query) {

        Bundle args = new Bundle();
        args.putInt("state", SEARCH_VIDEOS_LOADER);
        getLoaderManager().restartLoader(mSpecificVideoLoaderId++, args, this);

        HeaderItem header = new HeaderItem(getString(R.string.search_results, query));

        mRowsAdapter.replace(1, new ListRow(header, mVideoCursorAdapter));

        /*
        Cursor cursor = mVideoCursorAdapter.getCursor();
        int titleRes;
        if (cursor != null && cursor.moveToFirst()) {
            mResultsFound = true;
            titleRes = R.string.search_results;
        } else {
            mResultsFound = false;
            titleRes = R.string.no_search_results;
        }
        mVideoCursorAdapter.changeCursor(cursor);
        HeaderItem header = new HeaderItem(getString(titleRes, mQuery));
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        mRowsAdapter.replace(0, row);
         */
    }

    // An event was triggered by MediaController.TransportControls and must be handled here.
    // Here we update the media itself to act on the event that was triggered.
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            play();
        }

        @Override
        // This method should play any media item regardless of the Queue.
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Bundle args = new Bundle();
            args.putInt("state", QUEUE_VIDEOS_LOADER);
            getLoaderManager().restartLoader(mSpecificVideoLoaderId++, args, mCallbacks);
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onSkipToNext() {
            // Update the media to skip to the next video.
            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int nextIndex = ++mQueueIndex;
            if (nextIndex < mQueue.size()) {
                MediaSessionCompat.QueueItem item = mQueue.get(nextIndex);
                String mediaId = item.getDescription().getMediaId();
                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onSkipToPrevious() {
            // Update the media to skip to the previous video.
            setPlaybackState(PlaybackState.STATE_SKIPPING_TO_PREVIOUS);

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int prevIndex = --mQueueIndex;
            if (prevIndex >= 0) {
                MediaSessionCompat.QueueItem item = mQueue.get(prevIndex);
                String mediaId = item.getDescription().getMediaId();

                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onFastForward() {
            if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
                // Fast forward 10 seconds.
                int prevState = getPlaybackState();
                setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                setPosition(mPlayer.getCurrentPosition() + (10 * 1000));
                setPlaybackState(prevState);
            }
        }

        @Override
        public void onRewind() {
            // Rewind 10 seconds.
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_REWINDING);
            setPosition(mPlayer.getCurrentPosition() - (10 * 1000));
            setPlaybackState(prevState);
        }

        @Override
        public void onSeekTo(long position) {
            setPosition(position);
        }
    }


    private boolean isAlphabet(int code) {
        return Character.isLetter(code);
    }

    private void handleCharacter(int primaryCode) {
        int keyState = InputTables.KEYSTATE_NONE;

        if (isAlphabet(primaryCode)) {
            int ret = kauto.DoAutomata((char)primaryCode, keyState);
            if (ret < 0)
            {
                 //Log.v(TAG,"handleCharacter() - DoAutomata() call failed. primaryCode = " + primaryCode + " keyStete = " + keyState);
                if (kauto.IsKoreanMode())
                    kauto.ToggleMode();
            }
            else
            {
                // debug block..
                //Log.v(TAG, "handleCharacter - After calling DoAutomata()");
                //Log.v(TAG, "   KoreanMode = [" + (kauto.IsKoreanMode()? "true" : "false") + "]");
                //Log.v(TAG, "   CompleteString = [" + kauto.GetCompleteString() + "]");
                //Log.v(TAG, "   CompositionString = [" + kauto.GetCompositionString() + "]");
                //Log.v(TAG, "   State = [" + kauto.GetState() + "]");
                //Log.v(TAG, "   ret = [" + ret + "]");

                if ((ret & KoreanAutomata.ACTION_UPDATE_COMPLETESTR) != 0)
                {

                    if (mComposing.length() > 0)
                        mComposing.replace(mComposing.length()-1, mComposing.length(), kauto.GetCompleteString());
                    else
                        mComposing.append(kauto.GetCompleteString());

                    if (mComposing.length() > 0) {
                        //getCurrentInputConnection().setComposingText(mComposing, 1);

                    }
                }
                if ((ret & KoreanAutomata.ACTION_UPDATE_COMPOSITIONSTR) != 0)
                {
                    if ((mComposing.length() > 0) && ((ret & KoreanAutomata.ACTION_UPDATE_COMPLETESTR) == 0) && ((ret & KoreanAutomata.ACTION_APPEND) == 0))
                        mComposing.replace(mComposing.length()-1, mComposing.length(), kauto.GetCompositionString());
                    else
                        mComposing.append(kauto.GetCompositionString());
                    //getCurrentInputConnection().setComposingText(mComposing, 1);
                }
            }
            if ((ret & KoreanAutomata.ACTION_USE_INPUT_AS_RESULT) != 0)
            {

                mComposing.append((char) primaryCode);
                //getCurrentInputConnection().setComposingText(mComposing, 1);
            }
            //updateShiftKeyState(getCurrentInputEditorInfo());

        } else {
            if(kauto.IsKoreanMode()){
                if (mComposing.length() > 0)
                    mComposing.replace(mComposing.length()-1, mComposing.length(), kauto.GetCompleteString());
                else
                    mComposing.append(kauto.GetCompleteString());

                if ((mComposing.length() > 0))
                    mComposing.replace(mComposing.length()-1, mComposing.length(), kauto.GetCompositionString());
                else
                    mComposing.append(kauto.GetCompositionString());

                kauto.FinishAutomataWithoutInput();
            }

            mComposing.append((char) primaryCode);
        }
    }

    private void handleBackspace() {

        if (kauto.IsKoreanMode())
        {
            int ret = kauto.DoBackSpace();
            if (ret == KoreanAutomata.ACTION_ERROR)
            {
                // Log.v(TAG, "handleBackspace() - calling DoBackSpace() failed.");
                //// Log.v(TAG, "  mCompositionString = [" + kauto.GetCompositionString()+"]");
                // Log.v(TAG, "  mState = " + kauto.GetState());
                //updateShiftKeyState(getCurrentInputEditorInfo());
                return;
            }
            if ((ret & KoreanAutomata.ACTION_UPDATE_COMPOSITIONSTR) != 0)
            {
                if (kauto.GetCompositionString() != "")
                {

                    if (mComposing.length() > 0)
                    {
                        mComposing.replace(mComposing.length() -1, mComposing.length(), kauto.GetCompositionString());
                        //getCurrentInputConnection().setComposingText(mComposing, 1);
                    }

                    //updateShiftKeyState(getCurrentInputEditorInfo());
                    return;
                }

            }

        }
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            //getCurrentInputConnection().setComposingText(mComposing, 1);

        } else if (length > 0) {
            mComposing.setLength(0);
            //getCurrentInputConnection().commitText("", 0);

        } else {
            //keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        //updateShiftKeyState(getCurrentInputEditorInfo());
    }



    private void updateNowPlayingList(List<MediaSessionCompat.QueueItem> queue, long activeQueueId) {
        if (mListRowAdapter != null) {
            mListRowAdapter.clear();
            if (activeQueueId != MediaSessionCompat.QueueItem.UNKNOWN_ID) {
                Iterator<MediaSessionCompat.QueueItem> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    MediaSessionCompat.QueueItem queueItem = iterator.next();
                    if (activeQueueId != queueItem.getQueueId()) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
            mListRowAdapter.addAll(0, queue);
        }
    }


}
