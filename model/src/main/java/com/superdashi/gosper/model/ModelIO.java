package com.superdashi.gosper.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

// data structure:
//  handle as 3xfloats
//  number of materials
//  each material:
//   fg color argb
//   bg color argb
//  no of faces (1 third of number of vertices
//  vertices as 3xfloats
//  normals as 3xfloats
//  material indices as ints


public class ModelIO {

	private enum StlState {
		START,
		START_FACE,
		START_LOOP,
		VERTEX_1,
		VERTEX_2,
		VERTEX_3,
		END_LOOP,
		END_FACE,
		END;
	}

	private static final Pattern PAT_START = Pattern.compile("solid\\s+.*");
	private static final Pattern PAT_START_FACE = Pattern.compile("facet\\s+normal\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)");
	private static final Pattern PAT_START_LOOP = Pattern.compile("outer\\s+loop");
	private static final Pattern PAT_VERTEX = Pattern.compile("vertex\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)");
	private static final Pattern PAT_END_LOOP = Pattern.compile("endloop");
	private static final Pattern PAT_END_FACE = Pattern.compile("endfacet");
	private static final Pattern PAT_END = Pattern.compile("endsolid\\s+.*");

	private static final int DEFAULT_STL_FACES = 100;
	private static final Daub DEFAULT_DAUB = new Daub(0);

	private static float readFloat(ReadStream s) {
		float f = s.readFloat();
		if (Float.isNaN(f)) throw new IllegalArgumentException("float NaN");
		if (Float.isInfinite(f)) throw new IllegalArgumentException("float infinite");
		return f;
	}

//	private static float readComponent(ReadStream s) {
//		return Math.min(Math.max(readFloat(s), 0f), 1f);
//	}


	private static void record(Vertex v, ModelFace mf, Map<Vertex, List<ModelFace>> map) {
		List<ModelFace> list = map.get(v);
		if (list == null) {
			list = new ArrayList<>(4);
			map.put(v, list);
		}
		list.add(mf);
	}

	public Model readSTL(Reader reader, ModelMaterial material) throws IOException {
		return readSTL(reader, new float[3], 1f, new ModelMaterial[] { material }, t -> DEFAULT_DAUB, Collections.emptyMap());
	}

	public Model readSTL(Reader reader, ModelMaterial material, float smoothnessThreshold) throws IOException {
		return readSTL(reader, new float[3], 1f, new ModelMaterial[] { material }, t -> DEFAULT_DAUB, Collections.singletonMap(0, smoothnessThreshold));
	}

	public Model readSTL(Reader reader, float[] handle, float scale, ModelMaterial material) throws IOException {
		return readSTL(reader, handle, scale, new ModelMaterial[] { material }, t -> DEFAULT_DAUB, Collections.emptyMap());
	}

	@SuppressWarnings({ "incomplete-switch", "resource" })
	public Model readSTL(Reader reader, float[] handle, float scale, ModelMaterial[] materials, Function<Face, Daub> dauber, Map<Integer, Float> smoothMats) throws IOException {
		BufferedReader r;
		if (reader instanceof BufferedReader) {
			r = (BufferedReader) reader;
		} else {
			r = new BufferedReader(reader);
		}

		int count = 0;
		int maxCount = DEFAULT_STL_FACES;
		ModelFace[] faces = new ModelFace[maxCount];
		int[] matCounts = new int[materials.length];

		{
			StlState s = StlState.START;
			Normal n = null;
			Vertex v1 = null;
			Vertex v2 = null;
			Vertex v3 = null;
			while (true) {
				String line = r.readLine();
				if (line == null) {
					if (s == StlState.END) break;
					throw new IllegalArgumentException("unexpected end of file");
				}
				if (line.isEmpty()) {
					if (s == StlState.END) continue;
					throw new IllegalArgumentException("unexpected empty line");
				}
				line = line.trim();
				switch (s) {
				case START:
					if (!PAT_START.matcher(line).matches()) throw new IllegalArgumentException("expected 'solid' on first line");
					s = StlState.START_FACE;
					break;
				case START_FACE:
					if (PAT_END.matcher(line).matches()) {
						if (count == 0) throw new IllegalArgumentException("no faces");
						s = StlState.END;
					} else {
						Matcher matcher = PAT_START_FACE.matcher(line);
						if (!matcher.matches()) throw new IllegalArgumentException("invalid facet normal");
						float x = Float.parseFloat(matcher.group(1));
						float y = Float.parseFloat(matcher.group(2));
						float z = Float.parseFloat(matcher.group(3));
						n = new Normal(x, y, z);
						s = StlState.START_LOOP;
					}
					break;
				case START_LOOP:
					if (!PAT_START_LOOP.matcher(line).matches()) {
						throw new IllegalArgumentException("expected 'outer loop'");
					}
					s = StlState.VERTEX_1;
					break;
				case VERTEX_1:
				case VERTEX_2:
				case VERTEX_3:
					Matcher matcher = PAT_VERTEX.matcher(line);
					if (!matcher.matches()) throw new IllegalArgumentException("invalid vertex");
					float x = Float.parseFloat(matcher.group(1)) * scale;
					float y = Float.parseFloat(matcher.group(2)) * scale;
					float z = Float.parseFloat(matcher.group(3)) * scale;
					Vertex v = new Vertex(x, y, z);
					switch (s) {
					case VERTEX_1: s = StlState.VERTEX_2; v1 = v; break;
					case VERTEX_2: s = StlState.VERTEX_3; v2 = v; break;
					case VERTEX_3: s = StlState.END_LOOP; v3 = v; break;
					}
					break;
				case END_LOOP:
					if (!PAT_END_LOOP.matcher(line).matches()) {
						throw new IllegalArgumentException("expected 'endloop'");
					}
					s = StlState.END_FACE;
					break;
				case END_FACE:
					if (!PAT_END_FACE.matcher(line).matches()) {
						throw new IllegalArgumentException("expected 'endfacet'");
					}
					s = StlState.START_FACE;
					Face face = new Face(new Triangle(v1, v2, v3), n);
					Daub daub = dauber.apply(face);
					matCounts[daub.mat] ++;
					faces[count] = new ModelFace(count, face, daub);
					v1 = null;
					v2 = null;
					v3 = null;
					n = null;
					if (++count == maxCount) {
						maxCount = maxCount * 2;
						faces = Arrays.copyOf(faces, maxCount);
					}
					break;
				case END:
					throw new IllegalArgumentException("expected end of file");
				}
			}
		}

		final ModelFace[] mfs = faces;
		for (int mat : smoothMats.keySet()) {
			Map<Vertex, List<ModelFace>> map = new HashMap<>();
			Arrays.stream(faces, 0, count).filter(mf -> mf.daub.mat == mat).forEach(mf -> {
				Triangle t = mf.face.triangle;
				record(t.v1, mf, map);
				record(t.v2, mf, map);
				record(t.v3, mf, map);
			});
			float threshold = smoothMats.get(mat);
			map.entrySet().forEach(entry -> {
				Vertex v = entry.getKey();
				List<ModelFace> list = entry.getValue();
				if (list.size() == 1) return;
				Map<Normal, List<ModelFace>> ns = new HashMap<>();
				list.forEach(mf -> {
					Normal n = mf.face.normal;
					for (Entry<Normal,List<ModelFace>> ent : ns.entrySet()) {
							if (ent.getKey().asVector().dot(n.asVector()) > threshold) {
							ent.getValue().add(mf);
							return;
						}
					}
					List<ModelFace> l = new ArrayList<>(4);
					l.add(mf);
					ns.put(n, l);
				});
				ns.values().forEach(l -> {
					if (l.size() == 1) return;
					VectorMath vm = new VectorMath();
					l.forEach(mf -> {
						vm.add(mf.face.normal.asVector());
					});
					Normal n = vm.toNormal();
					l.forEach(mf -> {
						int i = mf.index;
						mfs[i] = mfs[i].withNormal(v, n);
					});
				});
			});
		}

		float[] normals  = new float [9 * count];
		float[] vertices = new float [9 * count];
		float[] texCoords = new float [6 * count];
		int[] indices = new int[materials.length];
		for (int i = 1; i < indices.length; i++) {
			indices[i] = matCounts[i - 1];
		}
		for (int j = 0; j < count; j++) {
			ModelFace mf = faces[j];
			Daub d = mf.daub;
			int i = indices[d.mat] ++;
			texCoords[i * 6    ] = d.p1.x;
			texCoords[i * 6 + 1] = d.p1.y;
			texCoords[i * 6 + 2] = d.p2.x;
			texCoords[i * 6 + 3] = d.p2.y;
			texCoords[i * 6 + 4] = d.p3.x;
			texCoords[i * 6 + 5] = d.p3.y;
			mf.n1.asVector().writeToArray(normals, i * 9    );
			mf.n2.asVector().writeToArray(normals, i * 9 + 3);
			mf.n3.asVector().writeToArray(normals, i * 9 + 6);
			Triangle t = mf.face.triangle;
			t.v1.asVector().writeToArray(vertices, i * 9    );
			t.v2.asVector().writeToArray(vertices, i * 9 + 3);
			t.v3.asVector().writeToArray(vertices, i * 9 + 6);
		}

		return new Model(handle, materials, vertices, normals, texCoords, matCounts);
	}

	public Model readModel(ReadStream s) {
		float[] handle = new float[3];
		handle[0] = readFloat(s);
		handle[1] = readFloat(s);
		handle[2] = readFloat(s);

		int m = s.readInt();
		if (m < 1) throw new IllegalArgumentException("invalid number of materials");
		ModelMaterial[] materials = new ModelMaterial[m];
		for (int i = 0; i < materials.length; i++) {
			int color = s.readInt();
			materials[i] = new ModelMaterial(color);
		}

		int n = s.readInt();
		if (n < 1) throw new IllegalArgumentException("invalid number of faces");
		float[] vertices = new float [n * 9];
		for (int i = 0; i < vertices.length; i++) {
			vertices[n] = readFloat(s);
		}

		float[] normals = new float[n * 9];
		for (int i = 0; i < normals.length; i++) {
			normals[n] = readFloat(s);
		}

		float[] texCoords = new float[n * 6];
		for (int i = 0; i < texCoords.length; i++) {
			texCoords[n] = readFloat(s);
		}

		int[] matCounts = new int[m];
		for (int i = 0; i < matCounts.length; i++) {
			int count = s.readInt();
			if (count < 0) throw new IllegalArgumentException("invalid material count");
			matCounts[i] = count;
		}

		return new Model(handle, materials, vertices, normals, texCoords, matCounts);
	}

	void writeModel(WriteStream s, Model m) {

	}
}
