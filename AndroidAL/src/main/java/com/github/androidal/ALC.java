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

import java.util.Arrays;

public enum ALC {
	/**
	 * General tokens.
	 */
	ALC_INVALID(0xFFFFFFFF),
	ALC_FALSE(0x0),
	ALC_TRUE(0x1),
	/**
	 * Context creation attributes.
	 */
	ALC_FREQUENCY(0x1007),
	ALC_REFRESH(0x1008),
	ALC_SYNC(0x1009),
	/**
	 * Error conditions.
	 */
	ALC_NO_ERROR(0x0),
	ALC_INVALID_DEVICE(0xA001),
	ALC_INVALID_CONTEXT(0xA002),
	ALC_INVALID_ENUM(0xA003),
	ALC_INVALID_VALUE(0xA004),
	ALC_OUT_OF_MEMORY(0xA005),
	/**
	 * String queries.
	 */
	ALC_DEFAULT_DEVICE_SPECIFIER(0x1004),
	ALC_DEVICE_SPECIFIER(0x1005),
	ALC_EXTENSIONS(0x1006),
	/**
	 * Integer queries.
	 */
	ALC_MAJOR_VERSION(0x1000),
	ALC_MINOR_VERSION(0x1001),
	ALC_ATTRIBUTES_SIZE(0x1002),
	ALC_ALL_ATTRIBUTES(0x1003),
	/**
	 * Context creation attributes.
	 */
	ALC_MONO_SOURCES(0x1010),
	ALC_STEREO_SOURCES(0x1011),
	/**
	 * String queries.
	 */
	ALC_DEFAULT_ALL_DEVICES_SPECIFIER(0x1012),
	ALC_ALL_DEVICES_SPECIFIER(0x1013),
	ALC_CAPTURE_DEVICE_SPECIFIER(0x310),
	ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER(0x311),
	/**
	 * Integer queries.
	 */
	ALC_CAPTURE_SAMPLES(0x312);

	final int _v;

	ALC(final int v) {
		_v = v;
	}

	public final int value() {
		return _v;
	}

	public static ALC getEnum(final Integer v) {
		return Arrays.stream(ALC.values())
			.filter(o -> o.value() == v).findFirst().orElse(ALC_FALSE);
	}
}
