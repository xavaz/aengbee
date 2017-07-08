/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.aengbee.android.leanback.media;


import com.aengbee.android.leanback.showcase.media.MusicPlaybackService;
import com.aengbee.android.leanback.showcase.media.VideoMediaPlayerGlue;

public class MediaUtils {
    /**
     * Theses states reflect different playback states sent out either by the
     * {@link MusicPlaybackService} or the {@link VideoMediaPlayerGlue}
     */
    // If the playback is in the process of preparing a media
    public static final int MEDIA_STATE_PREPARING = 0;
    // If the media is prepared by playback
    public static final int MEDIA_STATE_PREPARED = 1;
    // If the media is currently playing
    public static final int MEDIA_STATE_PLAYING = 2;
    // If the media is currently paused
    public static final int MEDIA_STATE_PAUSED = 3;
    // If the media item is finished playing
    public static final int MEDIA_STATE_COMPLETED = 4;
    // If the entire playlist is finished playing (no repeat buttons are enabled in this case)
    public static final int MEDIA_STATE_MEDIALIST_COMPLETED = 5;
}
