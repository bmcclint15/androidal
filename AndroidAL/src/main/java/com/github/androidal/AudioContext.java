package com.github.android_al;

import java.util.HashMap;
import java.util.Map;

class AudioContext {
	static final String TAG = "AndroidAL";

	static int nextContextId = 0; // Static tracker to mimic driver Ids
	static int nextSourceId = 0; // Static tracker to mimic driver Ids

	final int deviceId;
	final int contextId;

	// OpenAL enumerations / constants for implemented OpenAL functions
	// Our 'instance' variables...
	float AL_REFERENCE_DISTANCE = 1.0f;
	float AL_ROLLOFF_FACTOR = 1.0f;
	float AL_MAX_DISTANCE = Float.MAX_VALUE;
	float AL_DOPPLER_FACTOR = 1.0f;
	float AL_SPEED_OF_SOUND = 343.3f; // meters per second
	AL AL_DISTANCE_MODEL = AL.AL_INVERSE_DISTANCE_CLAMPED;

	// Driver variables for processing...WIP
	final AudioListener listener;

	// Sources for this context
	final Map<Integer, AudioSource> sources; // Configured sources

	AudioContext(final int deviceId, final int contextId) {
		this.deviceId = deviceId;
		this.contextId = contextId;

		this.sources = new HashMap<>();
		this.listener = new AudioListener();
	}
}
