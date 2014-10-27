package com.plusot.javacommon.util;


public class Matrix {
	private MathVector[] vectors;
	
	public Matrix(int size) {
		vectors = new MathVector[size];
		for (int i = 0; i < size; i++) {
			vectors[i] = new MathVector(size, i);
		}
	}
	
	public MathVector product(MathVector v) {
		MathVector result = new MathVector(vectors.length);
		for (int i = 0; i < Math.min(v.size(), vectors.length); i++) {
			for (int j = 0; j < result.size(); j++) {
				result.values[j] += vectors[i].values[j] * v.values[i]; 
			}
			
		}
		return result;
	}
	
	public Matrix product(Matrix m) {
		Matrix result = new Matrix(vectors.length);
		for (int i = 0; i < vectors.length; i++) {
			result.vectors[i] = product(m.vectors[i]);
		}
		return result;
		
	}
	public void productMe(Matrix m) {
		this.vectors = product(m).vectors;
	}
	
	/*
	 * angle in radians
	 */
	public void setRotationXY(double angle) {
		if (vectors.length < 2) return;
		vectors[0].values[0] = Math.cos(angle);
		vectors[0].values[1] = Math.sin(angle);
		vectors[1].values[0] = -Math.sin(angle);
		vectors[1].values[1] = Math.cos(angle);
	}
	
	public void setRotation(MathVector v, double angle) {
		if (vectors.length < 3) return;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		vectors[0].values[0] = cos + v.values[0] * v.values[0] * (1.0 - cos);
		vectors[0].values[1] = v.values[1] * v.values[0] * (1.0 - cos) + v.values[2] * sin;
		vectors[0].values[3] = v.values[2] * v.values[0] * (1.0 - cos) - v.values[1] * sin;
		vectors[1].values[0] = v.values[0] * v.values[1] * (1.0 - cos) - v.values[2] * sin;
		vectors[1].values[1] = cos + v.values[1] * v.values[1] * (1.0 - cos);
		vectors[1].values[2] = v.values[2] * v.values[1] * (1.0 - cos) - v.values[0] * sin;
		vectors[2].values[0] = v.values[0] * v.values[2] * (1.0 - cos) - v.values[1] * sin;
		vectors[2].values[1] = v.values[1] * v.values[2] * (1.0 - cos) - v.values[0] * sin;
		vectors[2].values[2] = cos + v.values[2] * v.values[2] * (1.0 - cos);
		
	}
	
	/*public void rotateXY(double angle) {
		Matrix matrix = new Matrix(vectors.length);
		matrix.setRotationXY(angle);
		productMe(matrix);
	}*/
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i < vectors.length; i++)
			sb.append(vectors[i].toString());
		sb.append(')');
		return sb.toString();
	}

}
