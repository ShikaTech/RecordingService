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

import android.content.Context;
import android.os.IBinder;
import android.support.annotation.NonNull;

/**
 * PostMux録画サービスアクセス用のヘルパークラス
 * #prepare => #start => #stop => #release
 */
public class PostMuxRecorder extends AbstractServiceRecorder {
	private static final boolean DEBUG = true;	// FIXME set false on production
	private static final String TAG = PostMuxRecorder.class.getSimpleName();
	
	public PostMuxRecorder(final Context context,
		@NonNull Class<? extends PostMuxRecService> serviceClazz,
		@NonNull final Callback callback) {

		super(context, serviceClazz, callback);
	}

	@Override
	protected AbstractRecorderService getService(final IBinder service) {
		return ((PostMuxRecService.LocalBinder)service).getService();
	}
}
