package com.serenegiant.recordingservice;
/*
 *
 * Copyright (c) 2016-2018 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.service.AbstractServiceRecorder;
import com.serenegiant.service.PostMuxRecService;
import com.serenegiant.service.PostMuxRecorder;
import com.serenegiant.utils.FileUtils;

import java.io.File;

public class CameraFragment extends AbstractCameraFragment {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = CameraFragment.class.getSimpleName();
	
	private PostMuxRecorder mPostMuxRecorder;
	
	public CameraFragment() {
		super();
		// need default constructor
	}

	@Override
	protected boolean isRecording() {
		return mPostMuxRecorder != null;
	}

	@Override
	protected void internalStartRecording() {
		if (mPostMuxRecorder == null) {
			mPostMuxRecorder = PostMuxRecorder.newInstance(getActivity(),
				PostMuxRecService.class, mCallback,
				PostMuxRecService.MUX_INTERMEDIATE_TYPE_CHANNEL);
			
		}
	}

	@Override
	protected void internalStopRecording() {
		if (mPostMuxRecorder != null) {
			mPostMuxRecorder.release();
			mPostMuxRecorder = null;
		}
	}

	@Override
	protected void onFrameAvailable() {
		if (mPostMuxRecorder != null) {
			mPostMuxRecorder.frameAvailableSoon();
		}
	}

	private int mRecordingSurfaceId = 0;
	private AbstractServiceRecorder.Callback mCallback
		= new AbstractServiceRecorder.Callback() {
		@Override
		public void onConnected() {
			if (DEBUG) Log.v(TAG, "onConnected:");
			if (mRecordingSurfaceId != 0) {
				mCameraView.removeSurface(mRecordingSurfaceId);
				mRecordingSurfaceId = 0;
			}
			if (mPostMuxRecorder != null) {
				try {
					mPostMuxRecorder.setVideoSettings(VIDEO_WIDTH, VIDEO_HEIGHT, 30, 0.25f);
					mPostMuxRecorder.prepare();
				} catch (final Exception e) {
					Log.w(TAG, e);
					stopRecording();	// 非同期で呼ばないとデッドロックするかも
				}
			}
		}
		
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@Override
		public void onPrepared() {
			if (DEBUG) Log.v(TAG, "onPrepared:");
			if (mPostMuxRecorder != null) {
				try {
					final Surface surface = mPostMuxRecorder.getInputSurface();
					if (surface != null) {
						mRecordingSurfaceId = surface.hashCode();
						mCameraView.addSurface(mRecordingSurfaceId, surface, true);
					} else {
						Log.w(TAG, "surface is null");
						stopRecording();	// 非同期で呼ばないとデッドロックするかも
					}
				} catch (final Exception e) {
					Log.w(TAG, e);
					stopRecording();	// 非同期で呼ばないとデッドロックするかも
				}
			}
		}
		
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@Override
		public void onReady() {
			if (DEBUG) Log.v(TAG, "onReady:");
			if (mPostMuxRecorder != null) {
				try {
					final File dir = new File(
						Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_MOVIES),
						"RecordingService");
					dir.mkdirs();
					mPostMuxRecorder.start(dir.toString(), FileUtils.getDateTimeString());
				} catch (final Exception e) {
					Log.w(TAG, e);
					stopRecording();	// 非同期で呼ばないとデッドロックするかも
				}
			}
		}
		
		@Override
		public void onDisconnected() {
			if (DEBUG) Log.v(TAG, "onDisconnected:");
			if (mRecordingSurfaceId != 0) {
				mCameraView.removeSurface(mRecordingSurfaceId);
				mRecordingSurfaceId = 0;
			}
			stopRecording();
		}
	};

}
