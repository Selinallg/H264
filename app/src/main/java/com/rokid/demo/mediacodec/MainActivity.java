/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.rokid.demo.mediacodec;


import android.Manifest;
import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";

    private AutoFitSurfaceView mPlaybackView;
    //    private SurfaceView  mPlaybackView;
    private TimeAnimator       mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodecWrapper mCodecWrapper;
    //    String path = Environment.getExternalStorageDirectory() + "/capture.h265";
//    String path = Environment.getExternalStorageDirectory() + "/4k_24FPS_14Mbps.mp4";
//    String path = Environment.getExternalStorageDirectory() + "/VR360_1.mp4";
    String path = Environment.getExternalStorageDirectory() + "/360上下.mp4";
    private MediaExtractor mExtractor;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate: 0");
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: 1");
        //setContentView(R.layout.activity_fun);
        Log.e(TAG, "onCreate: 2");
        mExtractor = new MediaExtractor();
        Log.e(TAG, "onCreate: 3");
        //SurfaceView surface = (SurfaceView) findViewById(R.id.preview);
        Log.e(TAG, "onCreate: 4");
        // mPlaybackView = surface;
        Log.e(TAG, "onCreate: 5");
        //mPlaybackView.getHolder().addCallback(this);
        Log.e(TAG, "onCreate: 6");
        mPlaybackView = new AutoFitSurfaceView(this);
        setContentView(mPlaybackView);
        mPlaybackView.getHolder().addCallback(this);
        checkPermission();
        Log.e(TAG, "onCreate: 7");
        //mExtractor.setDataSource(path);

        if (true) {
            try {
                mExtractor.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "onCreate: ppppppppppppp");
            }
        } else {

            File            file = new File(path);
            FileInputStream fis  = null;

            try {
                Log.e(TAG, "onCreate 8");
                fis = new FileInputStream(file);
                FileDescriptor fd = fis.getFD();
                mExtractor.setDataSource(fd);
                Log.e(TAG, "onCreate: sucess");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "onCreate: ======================", e);
            } finally {
                //Release stuff
                //mExtractor.release();
                //Log.e(TAG, "onCreate: 555");
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "onCreate: xxxxxxxxx", e);
                }

                Log.e(TAG, "onCreate 9");
            }
            Log.e(TAG, "onCreate: over");


        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null) {
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
        }
    }

    public void startPlayback() {

        Log.e(TAG, "startPlayback: ");

        // Construct a URI that points to the video resource that we want to play
        /*
        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                + R.raw.capture_h265);
         */

        String path     = Environment.getExternalStorageDirectory() + "/capture.h265";
        Uri    videoUri = Uri.parse(path);

        try {

            // BEGIN_INCLUDE(initialize_extractor)
            // mExtractor.setDataSource(this, videoUri, null);
//            mExtractor.setDataSource(path);
            int nTracks = mExtractor.getTrackCount();
            Log.e(TAG, "startPlayback: ");

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < nTracks; ++i) {
                mExtractor.unselectTrack(i);
            }


            // Find the first video track in the stream. In a real-world application
            // it's possible that the stream would contain multiple tracks, but this
            // sample assumes that we just want to play the first one.
            for (int i = 0; i < nTracks; ++i) {
                // Try to create a video codec for this track. This call will return null if the
                // track is not a video track, or not a recognized video format. Once it returns
                // a valid MediaCodecWrapper, we can break out of the loop.
                mCodecWrapper = MediaCodecWrapper.fromVideoFormat(mExtractor.getTrackFormat(i),
                        mPlaybackView.getHolder().getSurface());
                if (mCodecWrapper != null) {
                    mExtractor.selectTrack(i);
                    break;
                }
            }
            // END_INCLUDE(initialize_extractor)

            if (mCodecWrapper == null) {
                Log.e(TAG, "startPlayback: mCodecWrapper==null");
                return;
            }


            // By using a {@link TimeAnimator}, we can sync our media rendering commands with
            // the system display frame rendering. The animator ticks as the {@link Choreographer}
            // receives VSYNC events.
            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(final TimeAnimator animation,
                                         final long totalTime,
                                         final long deltaTime) {

                    boolean isEos = ((mExtractor.getSampleFlags() & MediaCodec
                            .BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                    // BEGIN_INCLUDE(write_sample)
                    if (!isEos) {
                        // Try to submit the sample to the codec and if successful advance the
                        // extractor to the next available sample to read.
                        boolean result = mCodecWrapper.writeSample(mExtractor, false,
                                mExtractor.getSampleTime(), mExtractor.getSampleFlags());

                        if (result) {
                            // Advancing the extractor is a blocking operation and it MUST be
                            // executed outside the main thread in real applications.
                            mExtractor.advance();
                        }
                    }
                    // END_INCLUDE(write_sample)

                    // Examine the sample at the head of the queue to see if its ready to be
                    // rendered and is not zero sized End-of-Stream record.
                    MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
                    mCodecWrapper.peekSample(out_bufferInfo);

                    // BEGIN_INCLUDE(render_sample)
                    if (out_bufferInfo.size <= 0 && isEos) {
                        mTimeAnimator.end();
                        mCodecWrapper.stopAndRelease();
                        mExtractor.release();
                    } else if (out_bufferInfo.presentationTimeUs / 1000 < totalTime) {
                        // Pop the sample off the queue and send it to {@link Surface}
                        mCodecWrapper.popSample(true);
                    }
                    // END_INCLUDE(render_sample)

                }
            });

            // We're all set. Kick off the animator to process buffers and render video frames as
            // they become available
            mTimeAnimator.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "============startPlayback:========= ", e);
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Log.e(TAG, "surfaceCreated: ");
        startPlayback();

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.e(TAG, "surfaceChanged: ");
//        mPlaybackView.setAspectRatio(2560, 720);
//        startPlayback();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.e(TAG, "surfaceDestroyed: ");
        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }
    }


    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.CAPTURE_AUDIO_OUTPUT,
                    Manifest.permission.RECORD_AUDIO
            }, 1);

        }
        return false;
    }
}
