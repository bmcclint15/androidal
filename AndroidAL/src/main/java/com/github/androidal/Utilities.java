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

class Utilities {
	/**
	 * produce a unit length vector for the supplied vector inplace.
	 * @param v1 vector1
	 */
	static void normalize(final float[] v1) {
		float d = 1.0f / magnitude(v1, v1);
		v1[0] *= d;
		v1[1] *= d;
		v1[2] *= d;
	}

	/**
	 * magnitude(length) of the vectors.
	 * <p>if v1 and v2 are the same object, its the magnitude of the vector.</p>
	 * <p>if v1 and v2 are different, then is the magnitude between the two vectors.</p>
	 * @param v1 vector1
	 * @param v2 vector2
	 * @return results as scalar
	 */
	static float magnitude(final float[] v1, final float[] v2) {
		return (float) Math.sqrt(dotProduct(v1, v2));
	}

	/**
	 * dot product (magnitude squared) of the vector.
	 * <p>unit vectors return -1.0 to 1.0.</p>
	 * <p>non-unit vectors return magnitude(length)squared.</p>
	 * @param v1 vector1
	 * @param v2 vector2
	 * @return results as scalar
	 */
	static float dotProduct(final float[] v1, final float[] v2) {
		return (v1[0] * v2[0]) + (v1[1] * v2[1]) + (v1[2] * v2[2]);
	}

	/**
	 * Compute the cross product of the orientation data.
	 * @param v1 orientation vector (at) to cross.
	 * @param v2 orientation vector (up) to cross.
	 * @param result results
	 */
	static void crossProduct(final float[] v1, final float[] v2, final float[] result) {
		result[0] = (v1[1] * v2[2]) - (v1[2] * v2[1]);
		result[1] = (v1[2] * v2[0]) - (v1[0] * v2[2]);
		result[2] = (v1[0] * v2[1]) - (v1[1] * v2[0]);
	}

	/**
	 * Subtract vector1 from vector2 and place results in result.
	 * @param v1 vector1
	 * @param v2 vector2
	 * @param result results
	 */
	static void subtract(final float[] v1, final float[] v2, final float[] result) {
		result[0] = v1[0] - v2[0];
		result[1] = v1[1] - v2[1];
		result[2] = v1[2] - v2[2];
	}

	/**
	 * Perform an endian swap for a short value.
	 *
	 * @param data  byte array.
	 * @param index in byte array to start at.
	 * @return endian swapped short.
	 */
	static short swapShort(final byte[] data, final int index) {
		return (short) ((byte2Int(data[index + 1]) << 8) + byte2Int(data[index]));
	}

	/**
	 * Convert a signed byte value to an integer.
	 *
	 * @param value to convert.
	 * @return value as integer.
	 */
	static int byte2Int(final byte value) {
		return (value & 0xff);
	}
}
