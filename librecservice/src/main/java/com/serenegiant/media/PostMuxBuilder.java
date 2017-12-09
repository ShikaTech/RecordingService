package com.serenegiant.media;
/*
 * Copyright (c) 2016-2017.  saki t_saki@serenegiant.com
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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by saki on 2017/12/08.
 *
 */
class PostMuxBuilder extends PostMuxCommon {
	private static final boolean DEBUG = true; // FIXME set false on production
	private static final String TAG = PostMuxBuilder.class.getSimpleName();
	
	private static final long MSEC30US = 1000000 / 30;

	private volatile boolean mIsRunning;
	
	public PostMuxBuilder() {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
	}
	
	public void cancel() {
		mIsRunning = false;
	}
	
	/**
	 * 一時ファイルからmp4ファイルを生成する。
	 * 終了まで返らないのでUIスレッドでは呼び出さないこと
	 * @param tempDirPath
	 * @param outputPath
	 * @throws IOException
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void build(@NonNull final String tempDirPath,
		@NonNull final String outputPath) throws IOException {

		if (DEBUG) Log.v(TAG, "build:");
		final File tempDir = new File(tempDirPath);
		final File videoFile = new File(tempDir, VIDEO_NAME);
		final File audioFile = new File(tempDir, AUDIO_NAME);
		final File output = new File(outputPath);
		final boolean hasVideo = videoFile.exists() && videoFile.canRead();
		final boolean hasAudio = audioFile.exists() && audioFile.canRead();
		if (hasVideo || hasAudio) {
			final IMuxer muxer = new MediaMuxerWrapper(outputPath,
				MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			if (muxer != null) {
				mIsRunning = true;
				try {
					final DataInputStream videoIn = hasVideo
						? new DataInputStream(
							new BufferedInputStream(new FileInputStream(videoFile)))
						: null;
					final DataInputStream audioIn = hasAudio
						? new DataInputStream(
							new BufferedInputStream(new FileInputStream(audioFile)))
						: null;
					internalBuild(muxer, videoIn, audioIn);
				} finally {
					mIsRunning = false;
					muxer.release();
				}
			} // if (muxer != null)
		}
		if (DEBUG) Log.v(TAG, "build:finished");
	}
	
	public void build(@NonNull final String tempDirPath,
		final int accessId) throws IOException {

		if (DEBUG) Log.v(TAG, "build:");
		final File tempDir = new File(tempDirPath);
		final File videoFile = new File(tempDir, VIDEO_NAME);
		final File audioFile = new File(tempDir, AUDIO_NAME);
		final boolean hasVideo = videoFile.exists() && videoFile.canRead();
		final boolean hasAudio = audioFile.exists() && audioFile.canRead();
		if (hasVideo || hasAudio) {
			final IMuxer muxer = null;	// FIXME 未実装
			if (muxer != null) {
				try {
					final DataInputStream videoIn = hasVideo
						? new DataInputStream(
							new BufferedInputStream(new FileInputStream(videoFile)))
						: null;
					final DataInputStream audioIn = hasAudio
						? new DataInputStream(
							new BufferedInputStream(new FileInputStream(audioFile)))
						: null;
					internalBuild(muxer, videoIn, audioIn);
				} finally {
					mIsRunning = false;
					muxer.release();
				}
			} // if (muxer != null)
		}
		if (DEBUG) Log.v(TAG, "build:finished");
	}
	
	/**
	 * #buildの実態
	 * @param muxer
	 * @param videoIn
	 * @param audioIn
	 * @throws IOException
	 */
	private void internalBuild(@NonNull final IMuxer muxer,
		@Nullable final DataInputStream videoIn,
		@Nullable final DataInputStream audioIn) throws IOException {
		
		if (DEBUG) Log.v(TAG, "internalBuild:");
		int videoTrack = -1;
		int audioTrack = -1;
		if (videoIn != null) {
			final MediaFormat format = readFormat(videoIn);
			if (format != null) {
				videoTrack = muxer.addTrack(format);
				if (DEBUG) Log.v(TAG, "found video data:format=" + format
					+ "track=" + videoTrack);
			}
		}
		if (audioIn != null) {
			final MediaFormat format = readFormat(audioIn);
			if (format != null) {
				audioTrack = muxer.addTrack(format);
				if (DEBUG) Log.v(TAG, "found audio data:format=" + format
					+ "track=" + audioTrack);
			}
		}
		if ((videoTrack >= 0) || (audioTrack >= 0)) {
			if (DEBUG) Log.v(TAG, "start muxing");
			mIsRunning = true;
			ByteBuffer videoBuf = null;
			MediaCodec.BufferInfo videoBufInfo = null;
			MediaFrameHeader videoFrameHeader = null;
			if (videoTrack >= 0) {
				videoBufInfo = new MediaCodec.BufferInfo();
				videoFrameHeader = new MediaFrameHeader();
			}
			ByteBuffer audioBuf = null;
			MediaCodec.BufferInfo audioBufInfo = new MediaCodec.BufferInfo();
			MediaFrameHeader audioFrameHeader = null;
			if (audioTrack >= 0) {
				audioBufInfo = new MediaCodec.BufferInfo();
				audioFrameHeader = new MediaFrameHeader();
			}
			final byte[] readBuf = new byte[64 * 1024];
			int videoSequence = 0;
			int audioSequence = 0;
			long videoTimeOffset = -1, videoPresentationTimeUs = -MSEC30US;
			long audioTimeOffset = -1, audioPresentationTimeUs = -MSEC30US;
			muxer.start();
			for (; mIsRunning && ((videoTrack >= 0) || (audioTrack >= 0)); ) {
				if (videoTrack >= 0) {
					try {
						videoBuf = readStream(videoIn, videoFrameHeader, videoBuf, readBuf);
						videoFrameHeader.asBufferInfo(videoBufInfo);
						if (videoSequence !=  videoFrameHeader.sequence) {
							videoSequence = videoFrameHeader.sequence;
							videoTimeOffset = videoPresentationTimeUs
								- videoBufInfo.presentationTimeUs + MSEC30US;
						}
						videoBufInfo.presentationTimeUs += videoTimeOffset;
						muxer.writeSampleData(videoTrack, videoBuf, videoBufInfo);
						videoPresentationTimeUs = videoBufInfo.presentationTimeUs;
					} catch (final IllegalArgumentException e) {
						if (DEBUG) Log.d(TAG,
							String.format("MuxerTask(video):size=%d,presentationTimeUs=%d,",
								videoBufInfo.size, videoBufInfo.presentationTimeUs)
							+ videoFrameHeader, e);
						videoTrack = -1;	// end
					} catch (IOException e) {
						videoTrack = -1;	// end
					}
				}
				if (audioTrack >= 0) {
					try {
						audioBuf = readStream(audioIn, audioFrameHeader, audioBuf, readBuf);
						audioFrameHeader.asBufferInfo(audioBufInfo);
						if (audioSequence !=  audioFrameHeader.sequence) {
							audioSequence = audioFrameHeader.sequence;
							audioTimeOffset = audioPresentationTimeUs
								- audioBufInfo.presentationTimeUs + MSEC30US;
						}
						audioBufInfo.presentationTimeUs += audioTimeOffset;
						muxer.writeSampleData(audioTrack, audioBuf, audioBufInfo);
						audioPresentationTimeUs = audioBufInfo.presentationTimeUs;
					} catch (final IllegalArgumentException e) {
						if (DEBUG) Log.d(TAG,
							String.format("MuxerTask(audio):size=%d,presentationTimeUs=%d,",
								audioBufInfo.size, audioBufInfo.presentationTimeUs)
							+ audioFrameHeader, e);
						audioTrack = -1;	// end
					} catch (IOException e) {
						audioTrack = -1;	// end
					}
				}
			}
			muxer.stop();
		}
		if (videoIn != null) {
			videoIn.close();
		}
		if (audioIn != null) {
			audioIn.close();
		}
	}
}
