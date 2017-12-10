package com.serenegiant.service;
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

import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.view.Surface;

import java.io.IOException;

/**
 * 録画サービスへアクセスするためのインターフェース
 */
public interface IServiceRecorder {

	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_BINDING = 1;
	public static final int STATE_BIND = 2;
	public static final int STATE_UNBINDING = 3;

	public interface Callback {
		public void onConnected();
		public void onPrepared();
		public void onReady();
		public void onDisconnected();
	}
	
	/**
	 * 関係するリソースを破棄する
	 */
	public void release();

	/**
	 * サービスとバインドして使用可能になっているかどうかを取得
	 * @return
	 */
	public boolean isReady();

	/**
	 * 録画中かどうかを取得
	 * @return
	 */
	public boolean isRecording();

	/**
	 * 録画の準備
	 * @param width
	 * @param height
	 * @param frameRate
	 * @param bpp
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void prepare(final int width, final int height,
		final int frameRate, final float bpp)
			throws IllegalStateException, IOException;
	/**
	 * 録画開始
	 * @param outputDir 出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void start(@NonNull final String outputDir, @NonNull final String name)
		throws IllegalStateException, IOException;

	/**
	 * 録画開始
	 * @param outputDir 出力ディレクトリ
	 * @param name 出力ファイル名(拡張子なし)
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void start(@NonNull final DocumentFile outputDir, @NonNull final String name)
		throws IllegalStateException, IOException;

	/**
	 * 録画終了
	 */
	public void stop();

	/**
	 * 録画用の映像を入力するためのSurfaceを取得
	 * @return
	 */
	public Surface getInputSurface();

	/**
	 * 録画用の映像フレームが準備できた時に録画サービスへ通知するためのメソッド
	 */
	public void frameAvailableSoon();
	
}