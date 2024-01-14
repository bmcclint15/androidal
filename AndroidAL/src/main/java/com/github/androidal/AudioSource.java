package com.github.android_al;

/**
 * Simulate an OpenAL source object.  This houses the data and length information for playing audio.
 * <p>
 * This is a package private class as its only needed in this class.
 */
class AudioSource {
	int id = 0;
	/**
	 * the ID of the attached buffer : Access via AL_BUFFER
	 */
	int bufferId = 0;
	/**
	 * the state of the source (AL_STOPPED, AL_PLAYING, …) : Access via AL_SOURCE_STATE
	 */
	AL state = AL.AL_INITIAL;
	/**
	 * pitch multiplier always positive : Access via AL_PITCH
	 */
	float pitch = 1.0f;
	/**
	 * source gain value should be positive : Access via AL_GAIN
	 */
	float gain = 1.0f;
	/**
	 * the minimum gain for this source : Access via AL_MIN_GAIN
	 */
	float gainMin = 0.0f;
	/**
	 * the maximum gain for this source : Access via AL_MAX_GAIN
	 */
	float gainMax = 1.0f;
	/**
	 * turns looping on (AL_TRUE) or off (AL_FALSE) : Access via AL_LOOPING
	 */
	boolean looping = false;
	/**
	 * used with the Inverse Clamped Distance Model to set the distance where there will no longer be any attenuation of the source : Access via AL_MAX_DISTANCE
	 */
	float maxDistance = Float.MAX_VALUE;
	/**
	 * the roll-off rate for the source default is 1.0 : Access via AL_ROLLOFF_FACTOR
	 */
	float rollOffFactor = 1.0f;
	/**
	 * the distance under which the volume for the source would normally drop by half (before being influenced by roll-off factor or AL_MAX_DISTANCE) : Access via AL_REFERENCE_DISTANCE
	 */
	float referenceDistance = 1.0f;
	/**
	 * the gain when outside the oriented cone : Access via AL_CONE_OUTER_GAIN
	 */
	float coneOuterGain = 0.0f;
	/**
	 * the gain when inside the oriented cone : Access via AL_CONE_INNER_ANGLE
	 */
	float coneInnerAngle = 360.0f;
	/**
	 * outer angle of the sound cone, in degrees default is 360 : Access via AL_CONE_OUTER_ANGLE
	 */
	float coneOuterAngle = 360.0f;
	/**
	 * X, Y, Z position : Access via AL_POSITION
	 */
	float[] position = new float[]{0.0f, 0.0f, 0.0f};
	/**
	 * velocity vector : Access via AL_VELOCITY
	 */
	float[] velocity = new float[]{0.0f, 0.0f, 0.0f};
	/**
	 * direction vector : Access via AL_DIRECTION
	 */
	float[] direction = new float[]{0.0f, 0.0f, 0.0f}; // OpenGL reference frame
	/**
	 * determines if the positions are relative to the listener default is AL_FALSE : Access via AL_SOURCE_RELATIVE
	 */
	boolean sourceRelative = false; // default assumes positions are in world coordinates
	/**
	 * the source type – AL_UNDETERMINED, AL_STATIC, or AL_STREAMING : Access via AL_SOURCE_TYPE
	 */
	AL sourceType = AL.AL_STATIC;
	/**
	 * the playback position, expressed in bytes : Access via AL_BYTE_OFFSET
	 */
	int byteOffset = 0;
	//	AL_SEC_OFFSET f, fv, i, iv the playback position, expressed in seconds
	//	AL_SAMPLE_OFFSET f, fv, i, iv the playback position, expressed in samples
	//	AL_BUFFERS_QUEUED* i, iv the number of buffers queued on this source
	//	AL_BUFFERS_PROCESSED* i, iv the number of buffers in the queue that have been processed

	// -------------------------------------------------------------------------
	// Constructors

	/**
	 * Create a new source.
	 *
	 * @param sourceId Id of the source created..
	 */
	AudioSource(final int sourceId) {
		this.id = sourceId;
	}
}
