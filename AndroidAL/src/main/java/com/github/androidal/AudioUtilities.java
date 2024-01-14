package com.github.android_al;

import android.annotation.SuppressLint;
import android.util.Log;

class AudioUtilities {
	static final String TAG = "AndroidAL";

	// Performance variable(s)...
	static final float[] temp1 = new float[3];

	/**
	 * Resample an 8 bit mono source to the native sample rate of the output device.
	 *
	 * @param data      Data to resample.
	 * @param frequency Data current frequency(sample rate).
	 */
	@SuppressLint("DefaultLocale")
	static float[] resample8bit(final byte[] data, final int frequency, final float defaultOutputSampleRate) {
		// Calculate the sample rate conversion and stop sizes
		final int sampleCount = data.length;
		final double frequencyAdj = defaultOutputSampleRate / frequency;
		final double adjIndex = frequency / defaultOutputSampleRate;
		Log.d(TAG, String.format("Convert 8bit to %f from %d, multiply by %f",
			defaultOutputSampleRate, frequency, frequencyAdj));

		// Dereference source buffer and allocation destination buffer
		float[] sampleData = new float[(int) (sampleCount * frequencyAdj)];

		// Calculate the inverse of the scalar to avoid division in inner loop
		final float scalar = (1.0f / (Byte.MAX_VALUE + 1));

		// Perform the actual resampling and conversion to float as required
		double srcIndex = 0; // Index in source
		int dstIndex = 0; // Index in destination
		int sample; // Working sample
		while (dstIndex < sampleData.length) {
			sample = Utilities.byte2Int(data[(int) srcIndex]);
			sampleData[dstIndex++] = scalar * sample; // Float conversion
			srcIndex += adjIndex; // Move to next resample position
		}

		return sampleData;
	}

	/**
	 * Resample a 16 bit mono source to the native sample rate of the output device.
	 *
	 * @param data      Data to resample.
	 * @param frequency Data current frequency(sample rate).
	 */
	@SuppressLint("DefaultLocale")
	static float[] resample16bit(final byte[] data, final int frequency, final float defaultOutputSampleRate) {
		// Calculate the sample rate conversion and stop sizes
		final int sampleCount = data.length / 2;
		final double frequencyAdj = defaultOutputSampleRate / frequency;
		final double adjIndex = frequency / defaultOutputSampleRate;
		Log.d(TAG, String.format("Convert 16bit to %f from %d, multiply by %f",
			defaultOutputSampleRate, frequency, frequencyAdj));

		// Dereference source buffer and allocation destination buffer
		float[] sampleData = new float[(int) (sampleCount * frequencyAdj)];

		// Calculate the inverse of the scalar to avoid division in inner loop
		final float scalar = (1.0f / Short.MAX_VALUE);

		// Perform the actual resampling and conversion to float as required
		double srcIndex = 0; // Index in source
		int dstIndex = 0; // Index in destination
		int sample; // Working sample
		while (dstIndex < sampleData.length) {
			sample = Utilities.swapShort(data, (int) (srcIndex) << 1); // << 1 == * 2
			sampleData[dstIndex++] = scalar * sample; // Float conversion
			srcIndex += adjIndex; // Move to next resample position
		}

		return sampleData;
	}

	/**
	 * Normalize the input array so that the min or max value is either -1 ot +1.<br>
	 * This has the effect of making all the sound samples the same volume.
	 *
	 * @param sampleData Array of sample data as floating point values.
	 */
	@SuppressLint("DefaultLocale")
	static void normalizeAudio(final float[] sampleData) {
		// Find the min and max ranges of the sample...
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for (float sampleDatum : sampleData) {
			min = Math.min(min, sampleDatum);
			max = Math.max(max, sampleDatum);
		}
		Log.d(TAG, String.format("Normalize audio: min %f, max %f", min, max));

		// Calculate the normalizing factor
		final float normalize = 1.0f / Math.max(Math.abs(min), max);

		// Normalize the audio to either a +1 or -1 maximum range.
		for (int i = 0; i < sampleData.length; i++) {
			sampleData[i] *= normalize;
		}
	}

	/**
	 * Brute force calculation of left/right gains based on location data.
	 *
	 * @param context Audio context for reference
	 * @param listener Audio listener
	 * @param source Audio source
	 * @return 2D gains for left/right
	 */
	static float[] computeLeftRightGains(final AudioContext context, final AudioListener listener, final AudioSource source) {
		// Compute the normalized direction vector to the sound source from listener.
		// NOTE: This DOES NOT take into account vertical displacement.
		Utilities.subtract(source.position, listener.position, temp1);
		Utilities.normalize(temp1);

		// DOT Product is the root for this algorithm
		float dot = Utilities.dotProduct(temp1, listener.orientRt);

		// Using the DOT result augment the left/right gains.
		float[] panningGain = new float[]{ 1.0f, 1.0f }; // Assume center
		if (dot < 0) {
			panningGain[1] += dot; // Left side; reduce right (y)
		} else if (dot > 0) {
			panningGain[0] -= dot; // Right side; reduce left (x)
		}

		return panningGain;
	}

	/**
	 * Compute the roll-off distance - OpenAL formulas
	 *
	 * @param context Audio context for reference
	 * @param listener Audio listener
	 * @param source Audio source
	 * @return Gain at distance source - listener.
	 */
	static float computeDistanceRolloff(final AudioContext context, final AudioListener listener, final AudioSource source) {
		if (context.AL_DISTANCE_MODEL.equals(AL.AL_NONE)) { return 1.0f; }

		Utilities.subtract(source.position, listener.position, temp1);

		// Trap for source and emitter are the same.
		final float lengthSquared = Utilities.dotProduct(temp1, temp1);
		if (lengthSquared == 0) {
			return 1.0f;
		}

		float distance = Utilities.magnitude(temp1, temp1);
		float gain;

		switch (context.AL_DISTANCE_MODEL) {
			case AL_INVERSE_DISTANCE_CLAMPED:
				distance = Math.max(context.AL_REFERENCE_DISTANCE, Math.min(distance, context.AL_MAX_DISTANCE));
				// Fall-through
			case AL_INVERSE_DISTANCE:
				gain = context.AL_REFERENCE_DISTANCE
					/ (context.AL_REFERENCE_DISTANCE + context.AL_ROLLOFF_FACTOR * (distance - context.AL_REFERENCE_DISTANCE));
				gain *= 10.0f; // Why?
				break;
			case AL_LINEAR_DISTANCE_CLAMPED:
				distance = Math.max(context.AL_REFERENCE_DISTANCE, Math.min(distance, context.AL_MAX_DISTANCE));
				// Fall-through
			case AL_LINEAR_DISTANCE:
				distance = Math.min(distance, context.AL_MAX_DISTANCE); // avoid negative gain
				gain = (1.0f - context.AL_ROLLOFF_FACTOR * (distance - context.AL_REFERENCE_DISTANCE)
					/ (context.AL_MAX_DISTANCE - context.AL_REFERENCE_DISTANCE));
				break;
			case AL_EXPONENT_DISTANCE_CLAMPED:
				distance = Math.max(context.AL_REFERENCE_DISTANCE, Math.min(distance, context.AL_MAX_DISTANCE));
				// Fall-through
			case AL_EXPONENT_DISTANCE:
				gain = (float) Math.pow((distance / context.AL_REFERENCE_DISTANCE), (-context.AL_ROLLOFF_FACTOR));
				break;
			case AL_NONE:
			default:
				gain = 1.0f;
				break;
		}

//		Log.i(TAG, String.format("%s, %s, %s, %f, %f, %f",
//			Arrays.toString(lPos),
//			Arrays.toString(sPos),
//			Arrays.toString(temp1),
//			lengthSquared, distance, gain);
		return gain;
	}

	/**
	 * Doppler Shift
	 * <p>
	 * The Doppler effect depends on the velocities of source and listener relative to the medium, and the
	 * propagation speed of sound in that medium. The application might want to emphasize or de-emphasize the
	 * Doppler effect as physically accurate calculation might not give the desired results. The amount of
	 * frequency shift (pitch change) is proportional to the speed of listener and source along their line of sight.
	 * <p>
	 * The Doppler effect as implemented by OpenAL is described by the formula below. Effects of the
	 * medium (air, water) moving with respect to listener and source are ignored.
	 * <p>
	 * SS: AL_SPEED_OF_SOUND = speed of sound (default value 343.3)
	 * DF: AL_DOPPLER_FACTOR = Doppler factor (default 1.0)
	 * vls: Listener velocity scalar (scalar, projected on source-to-listener vector)
	 * vss: Source velocity scalar (scalar, projected on source-to-listener vector)
	 * f: Frequency of sample
	 * f': effective Doppler shifted frequency
	 * <p>
	 * 3D Mathematical representation of vls and vss:
	 * <p>
	 * Mag(vector) = sqrt(vector.x * vector.x + vector.y * vector.y + vector.z * vector.z)
	 * DotProduct(v1, v2) = (v1.x * v2.x + v1.y * v2.y + v1.z * v2.z)
	 * SL = source to listener vector
	 * SV = Source Velocity vector
	 * LV = Listener Velocity vector
	 * vls = DotProduct(SL, LV) / Mag(SL)
	 * vss = DotProduct(SL, SV) / Mag(SL)
	 * <p>
	 * Dopper Calculation:
	 * vss = min(vss, SS/DF)
	 * vls = min(vls, SS/DF)
	 * f' = f * (SS - DF*vls) / (SS - DF*vss)
	 * <p>
	 * There are two API calls global to the current context that provide control of the speed of sound and
	 * Doppler factor. AL_DOPPLER_FACTOR is a simple scaling of source and listener velocities to exaggerate
	 * or de-emphasize the Doppler (pitch) shift resulting from the calculation.
	 * <p>
	 * void alDopplerFactor(ALfloat dopplerFactor);
	 * <p>
	 * A negative value will result in an AL_INVALID_VALUE error, the command is then ignored. The
	 * default value is 1. The current setting can be queried using alGetFloat{v} and AL_DOPPLER_FACTOR.
	 * <p>
	 * AL_SPEED_OF_SOUND allows the application to change the reference (propagation) speed used in the Doppler
	 * calculation. The source and listener velocities should be expressed in the same units as the speed of sound.
	 * <p>
	 * void alSpeedOfSound(ALfloat speed);
	 * <p>
	 * A negative or zero value will result in an AL_INVALID_VALUE error, and the command is ignored. The
	 * default value is 343.3 (appropriate for velocity units of meters and air as the propagation medium).
	 * The current setting can be queried using alGetFloat{v} and AL_SPEED_OF_SOUND.
	 * <p>
	 * Distance and velocity units are completely independent of one another (so you could use different units
	 * for each if desired). If an OpenAL application doesn't want to use Doppler effects, then leaving all
	 * velocities at zero will achieve that result.
	 *
	 * @param context Audio context for reference
	 * @param listener Audio listener
	 * @param source Audio source
	 * @return Doppler shift (pitch from 1.0)
	 */
	static float computeDopplerShift(final AudioContext context, final AudioListener listener, final AudioSource source) {
		// Short circuit
		if (context.AL_DOPPLER_FACTOR == 0.0f) { return 1.0f; }

		Utilities.subtract(source.position, listener.position, temp1);
		float magnitude = Utilities.magnitude(temp1, temp1);
		if (magnitude == 0) { return 1.0f; }

		// Compute the speed of sound (through the medium) scaled by the factor.
		float speedOfSound = context.AL_SPEED_OF_SOUND / context.AL_DOPPLER_FACTOR;

		// Compute the doppler shift; returning a 'pitch' from 1.0 + or -.
		float vls = (Utilities.dotProduct(temp1, listener.velocity) / magnitude) * -context.AL_DOPPLER_FACTOR;
		float vss = (Utilities.dotProduct(temp1, source.velocity) / magnitude) * -context.AL_DOPPLER_FACTOR;

		// Check for faster than speed of sound OR compute shift.
		/* Listener moving away from the source at the speed of sound.
		 * Sound waves can't catch it. */
		if (vls > speedOfSound) { return 0.0f; }
		/* Source moving toward the listener at the speed of sound. Sound
		 * waves bunch up to extreme frequencies. */
		if (vss > speedOfSound) { return Float.POSITIVE_INFINITY; }
		/* Source and listener movement is nominal. Calculate the proper doppler shift.  */
		return (context.AL_SPEED_OF_SOUND - (context.AL_DOPPLER_FACTOR * vls))
			/ (context.AL_SPEED_OF_SOUND - (context.AL_DOPPLER_FACTOR * vss));
	}
}
