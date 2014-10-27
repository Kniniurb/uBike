/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.javacommon.util;


public class MathVector {
	protected double values[];
	private MathVector ref = null;

	public MathVector(final double d1, final double d2, final double d3) {
		values = new double[3];
		values[0] = d1;
		values[1] = d2;
		values[2] = d3;
		setReference();
	}

	public MathVector(final double d1, final double d2) {
		values = new double[2];
		values[0] = d1;
		values[1] = d2;
		setReference();
	}

	public MathVector(final double[] vector) {
		this(vector, true);

	}

	public MathVector(final float[] vector, int maxLength) {
		values = new double[Math.min(maxLength, vector.length)];
		for (int i = 0; i < values.length; i++) {
			values[i] = vector[i];
		}
		setReference();
	}

	private MathVector(final double[] vector, boolean isSetReference) {
		if (vector == null) return;
		values = new double[vector.length];
		System.arraycopy(vector, 0, values, 0, values.length);
		if (isSetReference) setReference();
	}

	public MathVector(final MathVector vector) {
		this(vector.values);
	}

	public MathVector(final int length) {
		this(length, -1);
	}
	
	public MathVector(final int length, final int unityPos) {
		values = new double[length];
		for (int i = 0; i < length; i++) {
			if (i == unityPos) values[i] = 1; else values[i] = 0;
		}
		setReference();
	}

	public int size() {
		return values.length;
	}
	
	public void assign(final float[] vector) {
		if (vector != null)
			for (int i = 0; i < Math.min(values.length, vector.length); i++) values[i] = vector[i];	
	}

	public void assign(MathVector vector) {
		if (vector != null)
			for (int i = 0; i < Math.min(values.length, vector.values.length); i++) values[i] = vector.values[i];	
	}

	public void setReference() {
		ref = new MathVector(values, false);
	}

	public double maxDelta() {
		if (ref == null) return Double.MAX_VALUE;
		MathVector delta = this.min(ref);
		return delta.maxAbs();
	}
	
	public double deltaLength() {
		if (ref == null) return Double.MAX_VALUE;
		MathVector delta = this.min(ref);
		return delta.length();
	}

	public MathVector min(final MathVector v) {
		if (v == null) return null;
		int length = Math.min(values.length, v.values.length);
		MathVector result = new MathVector(length);
		for (int i = 0; i < length; i++) {
			result.values[i] = values[i] - v.values[i];
		}
		return result;
	}

	public MathVector plus(final MathVector v) {
		if (v == null) return null;
		int length = Math.min(values.length, v.values.length);
		MathVector result = new MathVector(length);
		for (int i = 0; i < length; i++) {
			result.values[i] = values[i] + v.values[i];
		}
		return result;
	}

	public void minmin(final MathVector v) {
		if (v == null) return;
		int length = Math.min(values.length, v.values.length);
		for (int i = 0; i < length; i++) {
			values[i] -= v.values[i];
		}
	}

	public void plusplus(final MathVector v) {
		if (v == null) return;
		int length = Math.min(values.length, v.values.length);
		for (int i = 0; i < length; i++) {
			values[i] += v.values[i];
		}
	}

	public void timestimes (final double d) {
		for (int i = 0; i < values.length; i++) {
			values[i] *= d;
		}
	}

	public MathVector times (final double d) {
		MathVector result = new MathVector(values.length);
		for (int i = 0; i < values.length; i++) {
			result.values[i] = values[i] * d;
		}
		return result;
	}

	public double length() {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i] * values[i];
		}
		return Math.sqrt(sum);
	}
	
	public double maxAbs() {
		double max = 0;
		for (int i = 0; i < values.length; i++) {
			max = Math.max(max, Math.abs(values[i]));
		}
		return max;
	}
	
	public double avg() {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		return sum / values.length;
	}
	
	/*public double avgNoZeroOrNegative() {
		double sum = 0;
		int div = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] > 0) {
				sum += values[i];
				div++;
			}
		}
		if (div == 0) return 0;
		return sum / div;
	}*/

	public double dotProduct(MathVector other) {
		double result = 0;
		for (int i = 0; i < Math.min(values.length, other.values.length); i++) {
			result += values[i] * other.values[i];
		}
		return result;
	}

	public MathVector crossProduct(MathVector other) {
		MathVector result = new MathVector(3);
		if (values.length >= 3 && other.values.length >= 3) {
			result.values[0] = values[1] * other.values[2] - values[2] * other.values[1];
			result.values[1] = values[2] * other.values[0] - values[0] * other.values[2];
			result.values[2] = values[0] * other.values[1] - values[1] * other.values[0];
		}

		return result;
	}
	
	public double crossProductPart(MathVector other) {
		if (values.length >= 2 && other.values.length >= 2) {
			return values[0] * other.values[1] - values[1] * other.values[0];
		}
		return 0;
	}

	public MathVector unitVector() {
		MathVector result = new MathVector(this);
		double length = length();
		for (int i = 0; i < values.length; i++) {
			result.values[i] = values[i] / length;
		}
		return result;
	}

	public double[] getValues() {
		double result[] = new double[values.length];
		System.arraycopy(values, 0, result, 0, values.length);
		return result;
	}

	public double getValue(int index) {
		if (index >= values.length || index < 0) return 0;
		return values[index];
	}

	public double getAbsValue(int index) {
		if (index >= values.length || index < 0) return 0;
		return Math.abs(values[index]);
	}
	
	@Override
	public String toString() {
		return '(' + StringUtil.toString(values, ", ", true, 4) + ')';
	}

}
