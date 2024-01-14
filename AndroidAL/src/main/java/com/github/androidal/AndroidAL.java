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

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Concrete implementation of an OpenAL audio implementation for Android Applications.<br>
 * NOTE: This Android implementation is using AudioTrack and manual mixing in mimic [OpenAL]<br>
 * NOTE: There are potential latency concerns
 * <p>
 * <a href="https://www.openal.org/documentation/OpenAL_Programmers_Guide.pdf">
 * https://www.openal.org/documentation/OpenAL_Programmers_Guide.pdf
 * </a>
 *
 * @author bmcclint
 */
public class AndroidAL {
	static final String TAG = "AndroidAL";
	
	// Vendor specific
	static final String AL_RENDERER = "AndroidAL";
	static final String AL_VENDOR = "bmcclint";
	static final String AL_VERSION = "0.1";
	static final String AL_EXTENSIONS = "NONE";

	// Error states
	AL alErrorState = AL.AL_NO_ERROR;
	String alErrorDescr = "";
	
	ALC alcErrorState = ALC.ALC_NO_ERROR;
	String alcErrorDescr = "";

	// Simulated hardware object trackers
	final Map<Integer, AudioDevice> devices; // Object where samples are written
	final Map<Integer, AudioContext> contexts; // Context Ids per Device
	final Map<Integer, AudioBuffer> buffers; // Sounds data managed by the driver

	// Currently active 'simulated hardware' objects.
	AudioDevice device;
	AudioContext context;
	AudioMixer audioMixer;

	// -------------------------------------------------------------------------
	// Constructor(s)

	/**
	 * Default constructor.
	 */
	public AndroidAL() {
		this.devices = new HashMap<>();
		this.contexts = new HashMap<>();
		this.buffers = new HashMap<>();

		Log.i(TAG, String.format("New AndroidAudio::AudioTrack"));
	}

	// -------------------------------------------------------------------------
	// Device / Context methods

	public long alcOpenDevice(Object buffer) {
		try {
			device = new AudioDevice();
			devices.put(device.id, device);

			Log.i(TAG, String.format("Android AudioTrack sound system initialized: %d", device.id));
			return device.id;
		} catch (Exception ex) {
			Log.e(TAG, String.format("Android AudioTrack sound system exception: %s", ex.getMessage()));
			return -1;
		}
	}

	public final long alcCreateContext(final int deviceId, final int[] attributes) {
		// NOTE: attributes is an array of ALenum and int value.
		// ALC_FREQUENCY      - output frequency (output rate in Hz)
		// ALC_REFRESH        - update / refresh rate (how often to update the mixing buffer)
		// ALC_MONO_SOURCES   - requested MONO sources
		// ALC_STEREO_SOURCES - requested STEREO sources
		// ALC_SYNC           - AL_TRUE | AL_FALSE (default this to false???)

		if (devices.containsKey((int) deviceId)) {
			AudioDevice device = devices.get(deviceId);

			// Make a mixing buffer the size of the minimum buffer adjusted to floats.
			// The buffer used to mix data where even indexes are left channel and odd is right.
			audioMixer = new AudioMixer(device.defaultMinBufferSizeInBytes, device.defaultOutputSampleRate, device.audioTrack, buffers);

			int contextId = ++AudioContext.nextContextId;
			context = new AudioContext(deviceId, contextId);
			contexts.put(contextId, context);

			Log.i(TAG, String.format("Created context %d for device Id: %d", contextId, deviceId));
			return contextId;
		} else {
			Log.e(TAG, String.format("Error creating context for device Id: %d", deviceId));
			return -1;
		}
	}

	public final boolean alcMakeContextCurrent(final long contextId) {
		if (contextId > 0 && contexts.containsKey((int) contextId)) {
			context = contexts.get((int) contextId);
			if (context == null) {
				Log.e(TAG, String.format("Device Id for Context Id %d was NULL", contextId));
				return false;
			}

			// Start the audio track processing data...
			AudioDevice device = devices.get(context.deviceId);
			if (device != null) {
				device.audioTrack.play(); // Actually doesn't start until data is written
				audioMixer.setContext(context);
				audioMixer.mixingThread.start();

				Log.i(TAG, String.format("Started mixing thread @ %d Hz", audioMixer.mixingHz));
				return true;
			} else {
				Log.w(TAG, String.format("No AudioTrack for deviceId: %d", device.id));
				return false;
			}
		} else {
			Log.w(TAG, String.format("No device for contextId: %d", contextId));
			return false;
		}
	}

	public final int alcGetCurrentContext() { return context.contextId; }

	public void alcDestroyContext(final long contextId) {
		if (contexts.containsKey((int) contextId)) {
			if (audioMixer.mixingThread != null) {
				audioMixer.mixingThread.interrupt();
				audioMixer.mixingThread = null;
				Log.i(TAG, String.format("Stopping mixing thread"));
			}

			context.sources.clear();

			contexts.remove((int) contextId);
		} else {
			Log.w(TAG, String.format("No context for contextId: %d", contextId));
		}
	}

	public final boolean alcCloseDevice(final long deviceId) {
		if (devices.containsKey((int) deviceId)) {
			AudioDevice device = devices.get((int) deviceId);
			if (device == null) {
				Log.e(TAG, String.format("AudioTrack for device Id %d was NULL", deviceId));
				return false;
			}

			device.audioTrack.stop();
			device.audioTrack.flush();
			device.audioTrack.release();

			buffers.clear();

			devices.remove((int) deviceId);

			Log.i(TAG, String.format("Stopping AudioTrack"));
			return true;
		} else {
			Log.i(TAG, String.format("alcCloseDevice::device not found: %d", deviceId));
			return false;
		}
	}

	public int alcGetContextsDevice(int contextId) {
		if (contexts.containsKey((int) contextId)) {
			AudioContext context = contexts.get(contextId);
			return context.deviceId;
		} else {
			Log.w(TAG, String.format("No context for contextId: %d", contextId));
			return -1;
		}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// AL buffers

	/**
	 * Requests a number of buffer names.
	 *
	 * @param bufferIds the buffers that will receive the buffer names
	 */
	public void alGenBuffers(final int[] bufferIds) {
		if (bufferIds != null && bufferIds.length > 0) {
			for (int i = 0; i < bufferIds.length; i++) {
				bufferIds[i] = ++AudioBuffer.nextBufferId;
			}
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The buffer array isn't large enough to hold the number of buffers requested.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_OUT_OF_MEMORY - There is not enough memory available to generate all the buffers requested.
	}

	/**
	 * Requests the deletion of a number of buffers.
	 *
	 * @param bufferIds the buffers to delete
	 */
	public void alDeleteBuffers(final int[] bufferIds) {
		if (bufferIds != null && bufferIds.length > 0) {
			for (int id = 0; id < bufferIds.length; id++) {
				if (buffers.containsKey(id)) {
					buffers.remove(id);
				} else {
					alErrorState = AL.AL_INVALID_NAME;
					alErrorDescr = "A buffer name is invalid: " + id;
					Log.i(TAG, alErrorDescr + ": NOTE: some buffers deleted.");
					return;
				}
			}
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "Call to delete buffers with NULL or empty request.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - The buffer is still in use and can not be deleted.
	}

	/**
	 * Verifies whether the specified object name is a buffer name.
	 *
	 * @param bufferId the buffer to query
	 * @return AL_TRUE if valid and AL_FALSE if not.
	 */
	public final boolean alIsBuffer(final int bufferId) {
		return buffers.containsKey(bufferId);
	}

	/**
	 * Sets the sample data of the specified buffer.
	 *
	 * <p>The data specified is copied to an internal software, or if possible, hardware buffer. The implementation is free to apply decompression, conversion,
	 * resampling, and filtering as needed.</p>
	 *
	 * <p>8-bit data is expressed as an unsigned value over the range 0 to 255, 128 being an audio output level of zero.</p>
	 *
	 * <p>16-bit data is expressed as a signed value over the range -32768 to 32767, 0 being an audio output level of zero. Byte order for 16-bit values is
	 * determined by the native format of the CPU.</p>
	 *
	 * <p>Stereo data is expressed in an interleaved format, left channel sample followed by the right channel sample.</p>
	 *
	 * <p>Buffers containing audio data with more than one channel will be played without 3D spatialization features – these formats are normally used for
	 * background music.</p>
	 *
	 * <p>IMPLEMENTATION SPECIFIC: samples are converted to floats -1.0 to 1.0 and normalized to max values of -1.0 or 1.0.</p>
	 *
	 * @param bufferId  the buffer to modify
	 * @param format    the data format
	 * @param buffer      the sample data
	 * @param frequency the data frequency
	 */
	@SuppressLint("DefaultLocale")
	public void alBufferData(final int bufferId, final int format, final Object buffer, final int frequency) {
		byte[] data = (byte[]) buffer;
		if (!buffers.containsKey(bufferId)) {
			float[] sampleData; // Re-sampled float data
			AL param = AL.getEnum(format);

			// Build the buffer
			AudioBuffer audioBuffer = new AudioBuffer(bufferId);
			audioBuffer.frequency = frequency;
			audioBuffer.size = data.length;
			switch (param) {
				case AL_FORMAT_MONO8:
					audioBuffer.channels = 1;
					audioBuffer.bits = 8;
					sampleData = AudioUtilities.resample8bit(data, frequency, device.defaultOutputSampleRate);
					break;
				case AL_FORMAT_MONO16:
					audioBuffer.channels = 1;
					audioBuffer.bits = 16;
					sampleData = AudioUtilities.resample16bit(data, frequency, device.defaultOutputSampleRate);
					break;
				case AL_FORMAT_STEREO8:
				case AL_FORMAT_STEREO16:
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified format does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					return;
			}

			// TODO: This maybe can be optional
			AudioUtilities.normalizeAudio(sampleData); // Make source at 100% volume
			audioBuffer.data = sampleData;

			// Create a new sound buffer object and track.
			synchronized (this) {
				buffers.put(bufferId, audioBuffer);
			}
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The size parameter is not valid for the format specified, the buffer is in use, or the data is a NULL pointer.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_OUT_OF_MEMORY - There is not enough memory available to create this buffer.
	}

	/**
	 * Returns the integer value of the specified parameter.
	 *
	 * @param bufferId the buffer to query
	 * @param param    the parameter to query
	 * @param value    the value queried
	 */
	@SuppressLint("DefaultLocale")
	public void alGetBufferi(final int bufferId, final AL param, final int[] value) {
		if (!buffers.containsKey(bufferId)) {
			// Build the buffer
			AudioBuffer buffer = buffers.get(bufferId);
			if (buffer == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified buffer name is not valid: " + bufferId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_FREQUENCY:
					value[0] = buffer.frequency;
					break;
				case AL_BITS:
					value[0] = buffer.bits;
					break;
				case AL_CHANNELS:
					value[0] = buffer.channels;
					break;
				case AL_SIZE:
					value[0] = buffer.size;
					break;
				//case AL_DATA:
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_NAME;
			alErrorDescr = "The specified buffer doesn't have parameters (the NULL buffer), or doesn't exist.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_VALUE - The specified value pointer is not valid.
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// AL sources

	/**
	 * Requests a number of source names.
	 *
	 * @param sourceIds the sources that will receive the source names
	 */
	public void alGenSources(final int[] sourceIds) {
		if (sourceIds != null && sourceIds.length > 0) {
			for (int i = 0; i < sourceIds.length; i++) {
				sourceIds[i] = ++AudioContext.nextSourceId;
				context.sources.put(sourceIds[i], new AudioSource(sourceIds[i]));
			}
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The source array isn't large enough to hold the number of buffers requested.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_OUT_OF_MEMORY - There is not enough memory available to generate all the sources requested.
	}

	/**
	 * Requests the deletion of a number of sources.
	 *
	 * @param sourceIds the sources to delete
	 */
	public void alDeleteSources(final int[] sourceIds) {
		if (sourceIds != null && sourceIds.length > 0) {
			for (int id = 0; id < sourceIds.length; id++) {
				if (context.sources.containsKey(id)) {
					context.sources.remove(id);
				} else {
					alErrorState = AL.AL_INVALID_NAME;
					alErrorDescr = "A source name is invalid: " + id;
					Log.i(TAG, alErrorDescr + ": NOTE: some buffers deleted.");
					return;
				}
			}
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "Call to delete sources with NULL or empty request.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Verifies whether the specified object name is a source name.
	 *
	 * @param sourceId the source to query
	 * @return AL_TRUE if valid and AL_FALSE if not.
	 */
	public final boolean alIsSource(final int sourceId) {
		return context.sources.containsKey(sourceId);
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the float value of a source parameter.
	 *
	 * @param sourceId the source to modify
	 * @param param    the parameter to modify.
	 * @param value    the parameter value
	 */
	public void alSourcef(final int sourceId, final AL param, final float value) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_PITCH:
					source.pitch = value;
					break;
				case AL_GAIN:
					if (value > 0) {
						source.gain = value;
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The value parameter is invalid: " + value;
						Log.e(TAG, alErrorDescr);
					}
					break;
				case AL_MIN_GAIN:
					source.gainMin = value;
					break;
				case AL_MAX_GAIN:
					source.gainMax = value;
					break;
				case AL_MAX_DISTANCE:
					source.maxDistance = value;
					break;
				case AL_ROLLOFF_FACTOR:
					source.rollOffFactor = value;
					break;
				case AL_CONE_OUTER_GAIN:
					source.coneOuterGain = value;
					break;
				case AL_CONE_INNER_ANGLE:
					source.coneInnerAngle = value;
					break;
				case AL_CONE_OUTER_ANGLE:
					source.coneOuterAngle = value;
					break;
				case AL_REFERENCE_DISTANCE:
					source.referenceDistance = value;
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null: " + sourceId;
			Log.e(TAG, alErrorDescr);
		}
	}

	/**
	 * Sets the 3D float values of a source parameter.
	 *
	 * @param param the parameter to modify.
	 * @param v1    the first value
	 * @param v2    the second value
	 * @param v3    the third value
	 */
	public void alSource3f(final int sourceId, final AL param, final float v1, final float v2, final float v3) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_POSITION:
					source.position[0] = v1;
					source.position[1] = v2;
					source.position[2] = v3;
					break;
				case AL_VELOCITY:
					source.velocity[0] = v1;
					source.velocity[1] = v2;
					source.velocity[2] = v3;
					break;
				case AL_DIRECTION:
					source.direction[0] = v1;
					source.direction[1] = v2;
					source.direction[2] = v3;
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null: " + sourceId;
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_VALUE - The value given is out of range.
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the 3 dimensional values of a source parameter.
	 *
	 * @param param  the parameter to modify
	 * @param values the parameter values
	 */
	public void alSourcefv(final int sourceId, final AL param, final float[] values) {
		if (values == null || values.length != 3) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_DIRECTION:
					System.arraycopy(values, 0, source.direction, 0, 3);
					break;
				case AL_POSITION:
					System.arraycopy(values, 0, source.position, 0, 3);
					break;
				case AL_VELOCITY:
					System.arraycopy(values, 0, source.velocity, 0, 3);
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the integer value of a source parameter.
	 *
	 * @param sourceId the source to modify
	 * @param param    the parameter to modify.
	 * @param value    the parameter value
	 */
	public void alSourcei(final int sourceId, final AL param, final int value) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_BUFFER:
					source.bufferId = value;
					break;
				case AL_SOURCE_STATE:
					source.state = AL.getEnum(value);
					break;
				case AL_LOOPING:
					source.looping = (value == 1);
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null: " + sourceId;
			Log.e(TAG, alErrorDescr);
		}
	}

	/**
	 * Returns the float value of the specified source parameter.
	 *
	 * @param sourceId the source to query
	 * @param param    the parameter to query.
	 * @param value    the parameter value
	 */
	public void alGetSourcef(final int sourceId, final AL param, final float[] value) {
		if (value == null || value.length != 1) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_PITCH:
					value[0] = source.pitch;
					break;
				case AL_GAIN:
					value[0] = source.gain;
					break;
				case AL_MIN_GAIN:
					value[0] = source.gainMin;
					break;
				case AL_MAX_GAIN:
					value[0] = source.gainMax;
					break;
				case AL_MAX_DISTANCE:
					value[0] = source.maxDistance;
					break;
				case AL_ROLLOFF_FACTOR:
					value[0] = source.rollOffFactor;
					break;
				case AL_CONE_OUTER_GAIN:
					value[0] = source.coneOuterGain;
					break;
				case AL_CONE_INNER_ANGLE:
					value[0] = source.coneInnerAngle;
					break;
				case AL_CONE_OUTER_ANGLE:
					value[0] = source.coneOuterAngle;
					break;
				case AL_REFERENCE_DISTANCE:
					value[0] = source.referenceDistance;
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Returns the integer value of the specified source parameter.
	 *
	 * @param sourceId the source to query
	 * @param param    the parameter to query.
	 * @param value    the parameter value
	 */
	public void alGetSourcei(final int sourceId, final AL param, final int[] value) {
		if (value == null || value.length != 1) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_BUFFER:
					value[0] = source.bufferId;
					break;
				case AL_SOURCE_RELATIVE:
					value[0] = source.sourceRelative ? AL.AL_TRUE.value() : AL.AL_FALSE.value();
					break;
				case AL_SOURCE_STATE:
					value[0] = source.state.value();
					break;
				case AL_BUFFERS_PROCESSED:
				case AL_BUFFERS_QUEUED:
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Returns float values of a source parameter.
	 *
	 * @param param  the parameter to query
	 * @param values the parameter values
	 */
	public void alGetSourcefv(final int sourceId, final AL param, final float[] values) {
		if (values == null || values.length != 3) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			switch (param) {
				case AL_DIRECTION:
					System.arraycopy(source.direction, 0, values, 0, 3);
					break;
				case AL_POSITION:
					System.arraycopy(source.position, 0, values, 0, 3);
					break;
				case AL_VELOCITY:
					System.arraycopy(source.velocity, 0, values, 0, 3);
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the source state to AL_PLAYING.
	 *
	 * <p>alSourcePlay applied to an AL_INITIAL source will promote the source to AL_PLAYING, thus the data found in the buffer will be fed into the processing,
	 * starting at the beginning. alSourcePlay applied to a AL_PLAYING source will restart the source from the beginning. It will not affect the configuration,
	 * and will leave the source in AL_PLAYING state, but reset the sampling offset to the beginning. alSourcePlay applied to a AL_PAUSED source will resume
	 * processing using the source state as preserved at the alSourcePause operation. alSourcePlay applied to a AL_STOPPED source will propagate it to
	 * AL_INITIAL then to AL_PLAYING immediately.</p>
	 *
	 * @param sourceId the source to play
	 */
	public void alSourcePlay(final int sourceId) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			if (!source.state.equals(AL.AL_PAUSED)) {
				source.byteOffset = 0;
			}
			source.state = AL.AL_PLAYING;
		} else {
			alErrorState = AL.AL_INVALID_NAME;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the source state to AL_PAUSED.
	 *
	 * <p>alSourcePause applied to an AL_INITIAL source is a legal NOP. alSourcePause applied to a AL_PLAYING source will change its state to AL_PAUSED. The
	 * source is exempt from processing, its current state is preserved. alSourcePause applied to a AL_PAUSED source is a legal NOP. alSourcePause applied to a
	 * AL_STOPPED source is a legal NOP.</p>
	 *
	 * @param sourceId the source to pause
	 */
	public void alSourcePause(final int sourceId) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			source.state = AL.AL_PAUSED;
		} else {
			alErrorState = AL.AL_INVALID_NAME;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the source state to AL_STOPPED.
	 *
	 * <p>alSourceStop applied to an AL_INITIAL source is a legal NOP. alSourceStop applied to a AL_PLAYING source will change its state to AL_STOPPED. The source
	 * is exempt from processing, its current state is preserved. alSourceStop applied to a AL_PAUSED source will change its state to AL_STOPPED, with the same
	 * consequences as on a AL_PLAYING source. alSourceStop applied to a AL_STOPPED source is a legal NOP.</p>
	 *
	 * @param sourceId the source to stop
	 */
	public void alSourceStop(final int sourceId) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			source.state = AL.AL_STOPPED;
		} else {
			alErrorState = AL.AL_INVALID_NAME;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the source state to AL_INITIAL.
	 *
	 * <p>alSourceRewind applied to an AL_INITIAL source is a legal NOP. alSourceRewind applied to a AL_PLAYING source will change its state to AL_STOPPED then
	 * AL_INITIAL. The source is exempt from processing: its current state is preserved, with the exception of the sampling offset, which is reset to the
	 * beginning. alSourceRewind applied to a AL_PAUSED source will change its state to AL_INITIAL, with the same consequences as on a AL_PLAYING source.
	 * alSourceRewind applied to an AL_STOPPED source promotes the source to AL_INITIAL, resetting the sampling offset to the beginning.</p>
	 *
	 * @param sourceId the source to rewind
	 */
	public void alSourceRewind(final int sourceId) {
		if (context.sources.containsKey(sourceId)) {
			AudioSource source = context.sources.get(sourceId);
			if (source == null) {
				alErrorState = AL.AL_INVALID_NAME;
				alErrorDescr = "The specified source name is not valid: " + sourceId;
				Log.e(TAG, alErrorDescr);
				return;
			}

			source.state = AL.AL_STOPPED;
			source.byteOffset = 0;
		} else {
			alErrorState = AL.AL_INVALID_NAME;
			alErrorDescr = "source is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// AL listener

	/**
	 * Sets the float value of a listener parameter.
	 *
	 * @param param the parameter to modify.
	 * @param value the parameter value
	 */
	public void alListenerf(final AL param, final float value) {
		if (context.listener != null) {
			switch (param) {
				case AL_GAIN:
					if (value > 0) {
						context.listener.gain = value;
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The value parameter is invalid: " + value;
						Log.e(TAG, alErrorDescr);
					}
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "listener is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the float value of a listener parameter.
	 *
	 * @param param the parameter to modify.
	 * @param v1    the first value
	 * @param v2    the second value
	 * @param v3    the third value
	 */
	public void alListener3f(final AL param, final float v1, final float v2, final float v3) {
		if (context.listener != null) {
			switch (param) {
				case AL_POSITION:
					context.listener.position[0] = v1;
					context.listener.position[1] = v2;
					context.listener.position[2] = v3;
					break;
				case AL_VELOCITY:
					context.listener.velocity[0] = v1;
					context.listener.velocity[1] = v2;
					context.listener.velocity[2] = v3;
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "listener is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_VALUE - The value given is not valid.
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Gets the float values of a listener parameter.
	 *
	 * @param param  the parameter to modify
	 * @param values the parameter values
	 */
	public void alListenerfv(final AL param, final float[] values) {
		if (values == null || (values.length != 3 && values.length != 6)) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.listener != null) {
			switch (param) {
				case AL_ORIENTATION:
					if (values.length == 6) {
						System.arraycopy(values, 0, context.listener.orientAt, 0, 3);
						System.arraycopy(values, 3, context.listener.orientUp, 0, 3);
						Utilities.crossProduct(context.listener.orientAt, context.listener.orientUp, context.listener.orientRt);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + values.length;
						Log.e(TAG, alErrorDescr);
					}
					break;
				case AL_POSITION:
					if (values.length == 3) {
						System.arraycopy(values, 0, context.listener.position, 0, 3);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + values.length;
						Log.e(TAG, alErrorDescr);
					}
					break;
				case AL_VELOCITY:
					if (values.length == 3) {
						System.arraycopy(values, 0, context.listener.velocity, 0, 3);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + values.length;
						Log.e(TAG, alErrorDescr);
					}
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "listener is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Gets the float value of a listener parameter.
	 *
	 * @param param the parameter to query.
	 * @param value the parameter value
	 */
	public void alGetListenerf(final AL param, final float[] value) {
		if (value == null || value.length != 1) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.listener != null) {
			switch (param) {
				case AL_GAIN:
					value[0] = context.listener.gain;
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "listener is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Returns float values of a listener parameter.
	 *
	 * @param param  the parameter to query
	 * @param values the parameter values
	 */
	public void alGetListenerfv(final AL param, final float[] values) {
		if (values == null || (values.length != 3 && values.length != 6)) {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The value pointer given is not valid.";
			Log.e(TAG, alErrorDescr);
			return;
		}

		if (context.listener != null) {
			switch (param) {
				case AL_ORIENTATION:
					if (values.length == 6) {
						System.arraycopy(context.listener.orientAt, 0, values, 0, 3);
						System.arraycopy(context.listener.orientUp, 3, values, 3, 3);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + ((values == null) ? 0 : values.length);
						Log.e(TAG, alErrorDescr);
					}
					break;
				case AL_POSITION:
					if (values.length == 3) {
						System.arraycopy(context.listener.position, 0, values, 0, 3);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + ((values == null) ? 0 : values.length);
						Log.e(TAG, alErrorDescr);
					}
					break;
				case AL_VELOCITY:
					if (values.length == 3) {
						System.arraycopy(context.listener.velocity, 0, values, 0, 3);
					} else {
						alErrorState = AL.AL_INVALID_VALUE;
						alErrorDescr = "The values parameter length is invalid or null: " + ((values == null) ? 0 : values.length);
						Log.e(TAG, alErrorDescr);
					}
					break;
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "listener is not defined or null.";
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// State properties

	/**
	 * Returns the float value of the specified parameter.
	 *
	 * @param param the parameter to query.
	 * @return value queried
	 */
	public final float alGetFloat(final AL param) {
		switch (param) {
			case AL_DOPPLER_FACTOR:
				return context.AL_DOPPLER_FACTOR;
			case AL_SPEED_OF_SOUND:
				return context.AL_SPEED_OF_SOUND;
			case AL_DISTANCE_MODEL:
				return context.AL_DISTANCE_MODEL.value();
			default:
				alErrorState = AL.AL_INVALID_ENUM;
				alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
				Log.e(TAG, alErrorDescr);
				return Float.NaN;
		}
	}

	/**
	 * Returns the string value of the specified parameter
	 *
	 * @param param the parameter to query.
	 * @return NULL terminated String
	 */
	public final String alGetString(final AL param) {
		switch (param) {
			case AL_RENDERER:
				return AL_RENDERER;
			case AL_VENDOR:
				return AL_VENDOR;
			case AL_VERSION:
				return AL_VERSION;
			case AL_EXTENSIONS:
				return AL_EXTENSIONS;
			default:
				alErrorState = AL.AL_INVALID_ENUM;
				alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
				Log.e(TAG, alErrorDescr);
				return null;
		}
	}

	/**
	 * Sets the doppler effect factor.
	 *
	 * @param value the doppler factor
	 */
	public void alDopplerFactor(final float value) {
		if (value >= 0) {
			context.AL_DOPPLER_FACTOR = value;
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The specified value is not valid: " + value;
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the speed of sound.
	 *
	 * @param value the speed of sound
	 */
	public void alSpeedOfSound(final float value) {
		if (value >= 0) {
			context.AL_SPEED_OF_SOUND = value;
		} else {
			alErrorState = AL.AL_INVALID_VALUE;
			alErrorDescr = "The specified value is not valid: " + value;
			Log.e(TAG, alErrorDescr);
		}
		// Not implemented: AL_INVALID_OPERATION - There is no current context.
	}

	/**
	 * Sets the distance attenuation model.
	 *
	 * <p>Samples usually use the entire dynamic range of the chosen format/encoding, independent of their real world intensity. For example, a jet engine and a
	 * clockwork both will have samples with full amplitude. The application will then have to adjust source gain accordingly to account for relative differences.</p>
	 *
	 * <p>Source gain is then attenuated by distance. The effective attenuation of a source depends on many factors, among which distance attenuation and source
	 * and listener gain are only some of the contributing factors. Even if the source and listener gain exceed 1.0 (amplification beyond the guaranteed
	 * dynamic range), distance and other attenuation might ultimately limit the overall gain to a value below 1.0.</p>
	 *
	 * <p>OpenAL currently supports three modes of operation with respect to distance attenuation, including one that is similar to the IASIG I3DL2 model. The
	 * application can choose one of these models (or chooses to disable distance-dependent attenuation) on a per-context basis.</p>
	 *
	 * @param modelName distance model to apply
	 */
	public void alDistanceModel(final AL modelName) {
		switch (modelName) {
			case AL_INVERSE_DISTANCE:
			case AL_INVERSE_DISTANCE_CLAMPED:
			case AL_LINEAR_DISTANCE:
			case AL_LINEAR_DISTANCE_CLAMPED:
			case AL_EXPONENT_DISTANCE:
			case AL_EXPONENT_DISTANCE_CLAMPED:
			case AL_NONE:
				context.AL_DISTANCE_MODEL = modelName;
				break;
			default:
				alErrorState = AL.AL_INVALID_ENUM;
				alErrorDescr = "The specified modelName does not exist or unhandled: " + modelName;
				Log.e(TAG, alErrorDescr);
				break;
		}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// AL capture

	// TODO: Wouldn't this be nice...

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// ALC properties

	/**
	 * Returns the string value of the specified parameter
	 *
	 * @param deviceId device to query
	 * @param param    the parameter to query.
	 * @return NULL terminated String
	 */
	public final String alcGetString(final long deviceId, final ALC param) {
		if (devices.containsKey((int) deviceId)) {
			switch (param) {
				case ALC_EXTENSIONS:
				case ALC_DEVICE_SPECIFIER:
				case ALC_DEFAULT_DEVICE_SPECIFIER:
				case ALC_CAPTURE_DEVICE_SPECIFIER:
				case ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER:
					return "NONE";
				default:
					alcErrorState = ALC.ALC_INVALID_ENUM;
					alcErrorDescr = "The specified parameter is not valid.";
					Log.e(TAG, alcErrorDescr);
					return null;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "device is not defined or null: " + deviceId;
			Log.e(TAG, alErrorDescr);
			return null;
		}
	}

	/**
	 * Obtains integer value(s) from ALC.
	 *
	 * @param deviceId device to query
	 * @param param    parameter to query
	 * @param value    integer value
	 */
	public void alcGetIntegerv(final int deviceId, final ALC param, final int[] value) {
		if (devices.containsKey(deviceId)) {
//			AudioTrack audioTrack = devices.get(deviceId);
			switch (param) {
				case ALC_MAJOR_VERSION:
				case ALC_MINOR_VERSION:
				case ALC_FREQUENCY: // output frequency
				case ALC_REFRESH:   // update rate of context processing
				case ALC_SYNC:      // flag indicating a synchronous context
					value[0] = 0;
					break;
				case ALC_ALL_ATTRIBUTES:
				default:
					alErrorState = AL.AL_INVALID_ENUM;
					alErrorDescr = "The specified parameter does not exist or unhandled: " + param;
					Log.e(TAG, alErrorDescr);
					break;
			}
		} else {
			alErrorState = AL.AL_INVALID_OPERATION;
			alErrorDescr = "device is not defined or null: " + deviceId;
			Log.e(TAG, alErrorDescr);
		}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// Error handlers

	/**
	 * Obtains error information.
	 *
	 * <p>Each detectable error is assigned a numeric code. When an error is detected by AL, a flag is
	 * set and the error code is recorded. Further errors, if they occur, do not affect this recorded
	 * code. When alGetError is called, the code is returned and the flag is cleared, so that a further
	 * error will again record its code. If a call to alGetError returns AL_NO_ERROR then there has been
	 * no detectable error since the last call to alGetError (or since the AL was initialized).</p>
	 *
	 * <p>Error codes can be mapped to strings. The alGetString function returns a pointer to a constant
	 * (literal) string that is identical to the identifier used for the enumeration value, as defined
	 * in the specification.</p>
	 */
	public final AL alGetError() {
		AL lastError = alErrorState;
		alErrorState = AL.AL_NO_ERROR;
		return lastError;
	}

	/**
	 * Queries ALC errors.
	 *
	 * <p>ALC uses the same conventions and mechanisms as AL for error handling. In particular, ALC
	 * does not use conventions derived from X11 (GLX) or Windows (WGL).</p>
	 *
	 * <p>Error conditions are specific to the device, and (like AL) a call to alcGetError resets the
	 * error state.</p>
	 *
	 * @param deviceId the device to query
	 */
	public final ALC alcGetError(final int deviceId) {
		ALC lastError = alcErrorState;
		alcErrorState = ALC.ALC_NO_ERROR;
		return lastError;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// Custom error methods

	public final boolean alIsError() {
		return !alErrorState.equals(AL.AL_NO_ERROR);
	}
	public final String alGetErrorDescr() {
		return alErrorDescr;
	}
	public final boolean alcIsError() {
		return !alcErrorState.equals(ALC.ALC_NO_ERROR);
	}
	public final String alcGetErrorDescr() {
		return alcErrorDescr;
	}
}
