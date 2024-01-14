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
