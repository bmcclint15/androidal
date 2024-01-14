/*
 * Copyright 2024 bmcclint15
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.androidal;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

class AudioDevice {
	static final String TAG = "AndroidAL";

	int id;
	int defaultOutputSampleRate;
	int defaultMinBufferSizeInBytes;
	AudioTrack audioTrack;

	AudioDevice() {
		// Query hardware specifics (Output sample rate and suggested minimum buffer size)
		defaultOutputSampleRate = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM);
		// Suggested minimum size to write
		defaultMinBufferSizeInBytes = AudioTrack.getMinBufferSize(
			(int) defaultOutputSampleRate,
			AudioFormat.CHANNEL_OUT_STEREO,
			AudioFormat.ENCODING_PCM_FLOAT);
		Log.i(TAG, String.format("Device default output sample rate in bytes per sec: %d", defaultOutputSampleRate));
		Log.i(TAG, String.format("Device default minimum buffer size in bytes: %d", defaultMinBufferSizeInBytes));

		// Correct for a default minimum size of 32k bytes
		defaultMinBufferSizeInBytes = Math.max(defaultMinBufferSizeInBytes, 32 * 1024); // 32k min
		Log.i(TAG, String.format("Device adjusted buffer size in bytes: %d", defaultMinBufferSizeInBytes));

		// Create the object where samples will be written, based on hardware data.
		audioTrack = new AudioTrack.Builder()
			.setAudioAttributes(new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_GAME)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build())
			.setAudioFormat(new AudioFormat.Builder()
				.setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
				.setSampleRate((int) defaultOutputSampleRate) // This 'could' be an attribute
				.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO) // Problem if only mono capable???
				.build())
			.setBufferSizeInBytes(defaultMinBufferSizeInBytes)
			.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
			.setTransferMode(AudioTrack.MODE_STREAM)
			.build();

		// Capture the audioTrack as the new deviceId
		id = audioTrack.getAudioSessionId();

		Log.i(TAG, String.format("Android AudioTrack sound system initialized: %d", id));
	}
}
