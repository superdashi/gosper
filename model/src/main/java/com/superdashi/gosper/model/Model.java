/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.model;

public class Model {

	// all faces triangular

	// 3 coordinates of the model's physical centre
	public final float[] handle;
	// the materials associated with this model
	public final ModelMaterial[] materials;
	// packed into an array, 3 floats to a vertex, 3 vertices for every face
	// no indexing, simply every 3 vertices represents a triangle
	public final float[] vertices;
	// packed into an array, 3 floats to a normal, one normal for every face
	public final float[] normals;
	// packed into an array, 2 floats to each vertex
	public final float[] texCoords;
	// maps number of triangles of each material
	// triangles are consolidated into groups of like-material
	public final int[] matCounts;

	public Model(float[] handle, ModelMaterial[] materials, float[] vertices, float[] normals, float[] texCoords, int[] matCounts) {
		this.handle = handle;
		this.materials = materials;
		this.vertices = vertices;
		this.normals = normals;
		this.texCoords = texCoords;
		this.matCounts = matCounts;
	}
}
