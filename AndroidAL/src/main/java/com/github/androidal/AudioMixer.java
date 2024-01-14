package com.github.android_al;

import android.media.AudioTrack;
import android.util.Log;

import java.util.Arrays;
import java.util.Map;

class AudioMixer {
	static final String TAG = "AndroidAL";

	static final double nanoToMilli = 1 / 1_000_000_000.0;
	static final float nanoToMicro = 1 / 1_000_000.0f;

	// Mixing trackers
	long lastThreadSleepTime = 0; // Last time the thread was put to sleep.
	long lastPlaybackHeadPosition = 0; //
	long estimatedPlaybackHeadPosition = 0; //
	float[] mixingBuffer; // Mixing buffer to be sent to hardware
	static final int mixingHz = 46; // Default to a 46Hz update cycle
	Thread mixingThread = null;

	int defaultOutputSampleRate;
	AudioContext context;

	final Map<Integer, AudioBuffer> buffers; // Sounds data managed by the driver

	AudioMixer(final int defaultMinBufferSizeInBytes, final int defaultOutputSampleRate, final AudioTrack audioTrack, final Map<Integer, AudioBuffer> buffers) {
		this.defaultOutputSampleRate = defaultOutputSampleRate;
		this.buffers = buffers;

		mixingBuffer = new float[defaultMinBufferSizeInBytes / Float.BYTES];
		Log.i(TAG, String.format("Mixing buffer size: %d bytes / %d samples", defaultMinBufferSizeInBytes, mixingBuffer.length));

		// Setup the mixing thread so audio is mixed OFF of the render thread.
		mixingThread = new Thread(() -> mix(audioTrack));
	}

	void setContext(final AudioContext context) { this.context = context; }

	/**
	 * Mixing thread method
	 *
	 * @param _audioTrack Android AudioTrack for audio output
	 */
	void mix(final AudioTrack _audioTrack) {
		/** Reference to AudioTrack */
		AudioTrack audioTrack = _audioTrack;

		// Determine the sleep delay to nano second resolution
		double rateMills =  1000.0 / mixingHz;
		int partMills = (int) rateMills;
		int partNanos = (int) ((rateMills - partMills) * 1_000_000);

		// Trackers per pass...
		long currentPlaybackHeadDelta = 0;
		int samplesToWrite = 0;
		int samplesWritten = 0;
		float thisThreadProcessingTime = 0;

		long thisThreadWakeupTime;
		long thisThreadPausedTime;
		long currentPlaybackHeadPosition;
		long currentPlaybackCount;

		try {
			while (true) {
				// Current time of update
				thisThreadWakeupTime = System.nanoTime(); // When we woke up
				thisThreadPausedTime = (thisThreadWakeupTime - lastThreadSleepTime);

				// Track the 'perceived' head position.  SOMETIMES there is an audible click in the track
				// that I think is from the two frames sent to the hardware not matching up.  If the thread
				// is too fast OR the hardware is too slow, we may not get two frames that 'seam' up.
				// The following logic looks to see how far apart the 'heads' are and tried to compensate.
				// it did help some with the clocking, BUT sometimes the click is worse OR its not there
				// at all.  Overall, I think this is a good approach, it needs refinement to ensure the
				// next frame going to the hardware seams up with the current playback position.  Maybe
				// experiment with pause and play with the steam OR a deeper hardware analysis.  For now,
				// 2024.01.06, this will suffice as a 'working' OpenAL supplement using native Android calls.
				currentPlaybackHeadPosition = Integer.toUnsignedLong(audioTrack.getPlaybackHeadPosition());
//				currentPlaybackCount = currentPlaybackHeadPosition - lastPlaybackHeadPosition;
				if (currentPlaybackHeadPosition != lastPlaybackHeadPosition || lastPlaybackHeadPosition == 0) {
					if (currentPlaybackHeadPosition != estimatedPlaybackHeadPosition && lastPlaybackHeadPosition != 0) {
						currentPlaybackHeadDelta = (currentPlaybackHeadPosition - estimatedPlaybackHeadPosition);
//						Log.v(TAG, String.format("Head positions off: %d - %d = %d",
//							currentPlaybackHeadPosition, estimatedPlaybackHeadPosition, currentPlaybackHeadDelta));
					}

					// Based off of the output sample rate, how many samples to write this frame.
					// Two options here...send a fixed size plus delta OR a varied size (based no thread
					// timing) plus delta.  Both are helping.  We many still need to jitter the buffer
					// positions in the mixer, meaning move the buffer position back sometimes if needed.
//					samplesToWrite = (int) (defaultOutputSampleRate * (1.0f / mixingHz) + currentPlaybackHeadDelta);
					samplesToWrite = (int) (defaultOutputSampleRate * (thisThreadPausedTime * nanoToMilli) + currentPlaybackHeadDelta);

					// Only fill the buffer once we have gone through once
					if (lastThreadSleepTime != 0) {
						samplesWritten = this.fillBuffer(audioTrack, samplesToWrite); // Actual off thread mixing
						lastPlaybackHeadPosition = Integer.toUnsignedLong(audioTrack.getPlaybackHeadPosition());
						estimatedPlaybackHeadPosition += samplesToWrite;
					}

					// Output the thread statistics, how long, etc...
//					thisThreadProcessingTime = (thisThreadProcessingTime * 0.9f) + (((System.nanoTime() - thisThreadWakeupTime) * nanoToMicro) * 0.1f);
//					Log.i(TAG, String.format("Mixing: sleeping: %fms, processing: %fms; writing: %d @ %d - wrote: %d / %d",
//						thisThreadPausedTime * nanoToMicro, thisThreadProcessingTime,
//						samplesToWrite, audioTrack.getPlaybackHeadPosition(), samplesWritten, currentPlaybackCount
//					));
				}

//				this.outputSourceStates(System.nanoTime());

				lastThreadSleepTime = System.nanoTime(); // When we go to sleep
				Thread.sleep(partMills, partNanos); // Sleep for the desiredRefresh Hz rate in millis
			}
		} catch (InterruptedException e) {
			Log.e(TAG, String.format("mixingThread::InterruptedException: %s", e.getMessage()));
			mixingThread.interrupt();
		} catch (Exception e) {
			Log.e(TAG, String.format("mixingThread::Exception: %s", e.getMessage()));
		}
	}

	/**
	 * Fill the mixing buffer with enough data for the next frame.<br>
	 * NOTE: Always delayed by the initial frame time (1/60sec ~16ms)
	 *
	 * @param audioTrack Android AudioTrack for audio output
	 */
	int fillBuffer(final AudioTrack audioTrack, final int samplesToWrite) {
		try {
			if (samplesToWrite < 0) { return 0; } // Sometimes we get here with negative samples count...

			// Compute the minimum number of samples to write so we don't go out of bounds.
			final int totalToWrite = Math.min(mixingBuffer.length, samplesToWrite * 2);

			// Clear the mixing buffer to silence for what will be written.
			Arrays.fill(mixingBuffer, 0, totalToWrite, 0.0f);

			// Mix the active sounds into the sound buffer.
			mixActiveSounds(samplesToWrite, totalToWrite);

			// Write the written mixing buffer to the hardware...
			return audioTrack.write(mixingBuffer, 0, totalToWrite, AudioTrack.WRITE_NON_BLOCKING);
		} catch (Exception ex) {
			Log.e(TAG, String.format("fillBuffer: : %s", ex.getMessage()));
			return -1;
		}
	}

	/**
	 * Mix any sounds that are active into the mix buffer from their respective source positions
	 * and writing totalToWrite samples to the buffer but tracking samplesToWrite advancement.
	 *
	 * @param samplesToWrite How many samples are we processing this pass.
	 * @param totalToWrite   How many samples are we writing to the buffer (go long)
	 */
	void mixActiveSounds(final int samplesToWrite, final int totalToWrite) {
		if (context.listener == null) { return; }

		try {
			for (final AudioSource source : context.sources.values()) {
				if (!source.state.equals(AL.AL_PLAYING)) { continue; }

				// Locate the buffer associated with the source.
				AudioBuffer buffer = buffers.get(source.bufferId);
				if (buffer == null) {
					Log.e(TAG, String.format("Unable to locate buffer with Id: %d", source.bufferId));
					continue;
				}

				// Capture the initial, panning and distance gains
				final float gainInit = source.gain;

				// If a listener is defined compute panning and distance gains
				float[] gainLR = AudioUtilities.computeLeftRightGains(context, context.listener, source);
				float	gainDist = AudioUtilities.computeDistanceRolloff(context, context.listener, source);
				float dopplerPitch = AudioUtilities.computeDopplerShift(context, context.listener, source);

				// Compute the source pitch by the computed doppler pitch shift.
				float pitch = source.pitch * dopplerPitch;
//				Log.i(TAG, String.format("%d) %s, %s, %s, %f, %f",
//					source.id, Arrays.toString(listener.position),
//					Arrays.toString(source.position),
//					Arrays.toString(gainLR),
//					gainDist, pitch);

				// Compute the overall gain from initial, distance and panning
				gainLR[0] *= (gainInit * gainDist);
				gainLR[1] *= (gainInit * gainDist);

				// NOTE: Skip 2 samples in mixing buffer for stereo mixing
				// NOTE: This writes the 'totalToWrite' which runs longer than the interval
				int startOffset = source.byteOffset;
				float readOffset = 0.0f;
				for (int mixBufferIndex = 0; mixBufferIndex < totalToWrite; mixBufferIndex += 2) {
					int bufferPosition = startOffset + (int)readOffset;
					if (bufferPosition == buffer.data.length) {
						if (source.looping) { // Wrap
							startOffset = 0; readOffset = 0; bufferPosition = 0;
						} else {
							break; // End of sound...transition to stop
						}
					}


					// Sum all samples together methodology
//					float before = mixingBuffer[mixBufferIndex];
//					float sample = buffer.data[bufferPosition];
					mixingBuffer[mixBufferIndex] += (buffer.data[bufferPosition] * gainLR[0]); // left
					mixingBuffer[mixBufferIndex + 1] += (buffer.data[bufferPosition] * gainLR[1]); // right
//					float after = mixingBuffer[mixBufferIndex];

//					Log.i(TAG, String.format("%d / %d / %d = %f / %f / %f",
//						mixBufferIndex, totalToWrite, bufferPosition,
//						before, sample, after);

					// Advance offset (adjusting for pitch)
					readOffset += pitch;
				}

				// Adjust and wrap the source position if looping, otherwise stop the source.
				// NOTE: This adjusts the position by the 'samplesToWrite' which is the preferred step.
				source.byteOffset += samplesToWrite;
				if (source.byteOffset >= buffer.data.length) {
					if (source.looping) { // Wrap
						source.byteOffset %= buffer.data.length;
					} else {
						source.state = AL.AL_STOPPED;
					}
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, String.format("mixActiveSounds: %s", ex.getMessage()));
		}
	}

	@SuppressWarnings("unused")
	long updateNanoTime = -1;

	@SuppressWarnings("unused")
	void outputSourceStates(final AudioContext context, final long thisNanoTime) {
		if (updateNanoTime == -1) {
			updateNanoTime = thisNanoTime;
			return; // First time
		}

		if ((thisNanoTime - updateNanoTime) < 1_000_000_000) {
			return; // Too early
		}
		updateNanoTime = thisNanoTime;

		StringBuilder states = new StringBuilder();
		for (AudioSource source : context.sources.values()) {
			switch (source.state) {
				case AL_INITIAL:
					states.append("I");
					break;
				case AL_PAUSED:
					states.append("|");
					break;
				case AL_PLAYING:
					states.append(">");
					break;
				case AL_STOPPED:
				default:
					states.append("X");
					break;
			}
		}
		Log.i(TAG, String.format("States: %s", states.toString()));
	}
}
