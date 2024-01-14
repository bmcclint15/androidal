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

/**
 * Simulate an OpenAL (READ-ONLY) buffer object.  This houses the data and length information.
 * <p>
 * This is a package private class as its only needed in this class.
 */
class AudioBuffer {
	static int nextBufferId = 0; // Static tracker to mimic driver Ids

	/**
	 * the ID of the attached buffer : Access via AL_BUFFER
	 */
	int id;
	/**
	 * frequency of buffer in Hz : Access via AL_FREQUENCY
	 */
	int frequency;
	/**
	 * bit depth of buffer : Access via AL_BITS
	 */
	int bits;
	/**
	 * number of channels in buffer > 1 is valid, but buffer won’t be positioned when played : Access via AL_CHANNELS
	 */
	int channels;
	/**
	 * size of buffer in bytes : Access via AL_SIZE
	 */
	int size;
	/**
	 * original location where data was copied from : Access via ?? AL_DATA ??
	 */
	float[] data;

	// -------------------------------------------------------------------------
	// Constructors

	/**
	 * Create a new sound buffer
	 *
	 * @param bufferId Id of the buffer created..
	 */
	AudioBuffer(final int bufferId) {
		this.id = bufferId;
	}
}
