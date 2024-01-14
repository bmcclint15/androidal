package com.github.android_al;

/**
 * Simulate an OpenAL listener object.
 * <p>
 * This is a package private class as its only needed in this class.
 */
class AudioListener {
	/**
	 * “master gain” value should be positive : Access via AL_GAIN
	 */
	float gain = 1.0f;
	/**
	 * X, Y, Z position : Access via AL_POSITION
	 */
	float[] position = new float[]{0.0f, 0.0f, 0.0f};
	/**
	 * velocity vector : Access via AL_VELOCITY
	 */
	float[] velocity = new float[]{0.0f, 0.0f, 0.0f};
	/**
	 * orientation expressed as “at” and “up” vectors : Access via AL_ORIENTATION
	 */
	float[] orientAt = new float[]{0.0f, 0.0f, -1.0f};
	float[] orientUp = new float[]{0.0f, 1.0f, 0.0f};
	/**
	 * custom parameters...cross of at and up...
	 */
	float[] orientRt = new float[]{1.0f, 0.0f, 0.0f};

	// -------------------------------------------------------------------------
	// Constructors

	/**
	 * Create a new listener.
	 */
	AudioListener() {
	}
}
