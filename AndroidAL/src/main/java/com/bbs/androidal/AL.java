package com.bbs.android_al;

import java.util.Arrays;

public enum AL {
	/**
	 * General tokens.
	 */
	AL_INVALID(0xFFFFFFFF),
	AL_NONE(0x0),
	AL_FALSE(0x0),
	AL_TRUE(0x1),
	/**
	 * Error conditions.
	 */
	AL_NO_ERROR(0x0),
	AL_INVALID_NAME(0xA001),
	AL_INVALID_ENUM(0xA002),
	AL_INVALID_VALUE(0xA003),
	AL_INVALID_OPERATION(0xA004),
	AL_OUT_OF_MEMORY(0xA005),
	/**
	 * Numerical queries.
	 */
	AL_DOPPLER_FACTOR(0xC000),
	AL_DISTANCE_MODEL(0xD000),
	/**
	 * String queries.
	 */
	AL_VENDOR(0xB001),
	AL_VERSION(0xB002),
	AL_RENDERER(0xB003),
	AL_EXTENSIONS(0xB004),
	/**
	 * Distance attenuation models.
	 */
	AL_INVERSE_DISTANCE(0xD001),
	AL_INVERSE_DISTANCE_CLAMPED(0xD002),
	/**
	 * Source types.
	 */
	AL_SOURCE_ABSOLUTE(0x201),
	AL_SOURCE_RELATIVE(0x202),
	/**
	 * Listener and Source attributes.
	 */
	AL_POSITION(0x1004),
	AL_VELOCITY(0x1006),
	AL_GAIN(0x100A),
	/**
	 * Source attributes.
	 */
	AL_CONE_INNER_ANGLE(0x1001),
	AL_CONE_OUTER_ANGLE(0x1002),
	AL_PITCH(0x1003),
	AL_DIRECTION(0x1005),
	AL_LOOPING(0x1007),
	AL_BUFFER(0x1009),
	AL_SOURCE_STATE(0x1010),
	AL_CONE_OUTER_GAIN(0x1022),
	AL_SOURCE_TYPE(0x1027),
	/**
	 * Source state.
	 */
	AL_INITIAL(0x1011),
	AL_PLAYING(0x1012),
	AL_PAUSED(0x1013),
	AL_STOPPED(0x1014),
	/**
	 * Listener attributes.
	 */
	AL_ORIENTATION(0x100F),
	/**
	 * Queue state.
	 */
	AL_BUFFERS_QUEUED(0x1015),
	AL_BUFFERS_PROCESSED(0x1016),
	/**
	 * Gain bounds.
	 */
	AL_MIN_GAIN(0x100D),
	AL_MAX_GAIN(0x100E),
	/**
	 * Distance model attributes),
	 */
	AL_REFERENCE_DISTANCE(0x1020),
	AL_ROLLOFF_FACTOR(0x1021),
	AL_MAX_DISTANCE(0x1023),
	/**
	 * Buffer attributes),
	 */
	AL_FREQUENCY(0x2001),
	AL_BITS(0x2002),
	AL_CHANNELS(0x2003),
	AL_SIZE(0x2004),
	/**
	 * Buffer formats.
	 */
	AL_FORMAT_MONO8(0x1100),
	AL_FORMAT_MONO16(0x1101),
	AL_FORMAT_STEREO8(0x1102),
	AL_FORMAT_STEREO16(0x1103),
	/**
	 * Buffer state.
	 */
	AL_UNUSED(0x2010),
	AL_PENDING(0x2011),
	AL_PROCESSED(0x2012),
	/**
	 * AL11 - General tokens.
	 */
	AL_SEC_OFFSET(0x1024),
	AL_SAMPLE_OFFSET(0x1025),
	AL_BYTE_OFFSET(0x1026),
	AL_STATIC(0x1028),
	AL_STREAMING(0x1029),
	AL_UNDETERMINED(0x1030),
	AL_ILLEGAL_COMMAND(0xA004),
	AL_SPEED_OF_SOUND(0xC003),
	AL_LINEAR_DISTANCE(0xD003),
	AL_LINEAR_DISTANCE_CLAMPED(0xD004),
	AL_EXPONENT_DISTANCE(0xD005),
	AL_EXPONENT_DISTANCE_CLAMPED(0xD006);

	final int _v;

	AL(final int v) {
		_v = v;
	}

	public final int value() {
		return _v;
	}

	public static AL getEnum(final Integer v) {
		return Arrays.stream(AL.values())
			.filter(o -> o.value() == v).findFirst().orElse(AL.AL_NONE);
	}
}
