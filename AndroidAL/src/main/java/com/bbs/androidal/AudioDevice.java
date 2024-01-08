package com.bbs.android_al;

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
