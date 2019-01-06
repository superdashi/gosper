#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
  #define gl_FragColor mgl_FragColor
#endif

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PI  3.1415926535897932384626433832795
#define TAU 6.28318530718
#define CAUSTIC_MAX_ITER 5

const vec4 def_specular = vec4(1.0, 1.0, 1.0, 0.0);
const vec4 zero4        = vec4(0.0, 0.0, 0.0, 0.0);
const vec3 zero3        = vec3(0.0, 0.0, 0.0);
const vec4 one4         = vec4(1.0, 1.0, 1.0, 1.0);
const vec3 one3         = vec3(1.0, 1.0, 1.0);
const vec2 one2         = vec2(1.0, 1.0);
const vec3 thirds       = vec3(0.33333333, 0.33333333, 0.33333333);

uniform sampler2D u_texture;
uniform sampler2D u_data;
uniform sampler2D u_chars;
uniform sampler2D u_palettes;
uniform float u_time;
uniform vec3 u_ambient_color;
uniform vec3 u_light_direction;
uniform vec3 u_light_color;
uniform float u_palette;
uniform vec2 u_char_scale;

varying vec3  v_position; // vertex position in camera space
varying vec3  v_normal;   // vertex normal in camera space
varying vec4  v_color;    // vertex color
varying vec2  v_tx_coord; // texture coordinates
varying vec4  v_shader;   // parameters for the shader

// shader config for PULSE:   time-offset [s], time-scale [t], ring-scale [p], ring-proportion [q]
// shader config for TRANS:   start-time [s], color-time [t], fade-time [p], finish-time [q]
// shader config for FADE:    start-time [s], commence-time [t], end-time [p], finish-time [q]
// shader config for PLASMA:  x-scale [s], y-scale [t], x-offset [p], y-offset [q]
// shader config for BORDER:  color [rgb], thickness [a]
// shader config for NOISE:   color [rgb], ([a] unused)
// shader config for PULSE:   start-time [s], pulse period [t], band width [p] band fill proportion [q]
// shader config for CONSOLE: columns [s], rows [t]

#ifdef LIT
void light (inout vec4 sample, in vec4 specular) {
  vec3 n = normalize(v_normal);
  // diffuse
  float diff_cos = dot(n, -u_light_direction);
# ifdef TWO_SIDED
  float diff_coe = abs(diff_cos);
# else
  float diff_coe = max(diff_cos, 0.0);
# endif
  vec3  diff_col = sample.rgb * u_light_color * diff_coe;
  
  // ambient
  vec3  ambi_col = sample.rgb * u_ambient_color;
  
  // specular
  vec3  spec_col = vec3(0.0);
  if (diff_coe > 0.0 && specular.a > 0.003) {
    vec3  refl_vec = reflect(u_light_direction, n);
    float spec_cos = max(dot(normalize(-v_position), refl_vec), 0.0);
    float spec_coe = pow(spec_cos, 3.0);
          spec_col = specular.xyz * specular.a * spec_coe;
  }
  
  // combined
  vec3  comb_col = clamp(diff_col + ambi_col + spec_col, 0.0, 1.0);
  sample.rgb = comb_col;
}
#endif

void main (void) {

#ifdef MODE_GONE
  gl_FragColor = zero4;
#endif

#ifdef MODE_PLAIN
  gl_FragColor = v_color;
# ifdef LIT
  light(gl_FragColor, v_shader);
# endif
#endif

#ifdef MODE_MASK
  vec4 t = texture2D(u_texture, v_tx_coord);
  gl_FragColor = mix(v_shader, v_color, t.r);
# ifdef LIT
  light(gl_FragColor, def_specular);
# endif
#endif

#ifdef MODE_TEXT
  vec4 t = texture2D(u_texture, v_tx_coord);
  float f = t.r;
  if (f <= 0.502) {
    gl_FragColor = v_shader;
    gl_FragColor.a = f * 2.0;
  } else {
    gl_FragColor = mix(v_shader, v_color, f * 2.0 - 1.0);
  }
# ifdef LIT
  light(gl_FragColor, def_specular);
# endif
#endif

#ifdef MODE_DISC
  vec2 v = v_tx_coord;
  float d = dot(v, v);
  gl_FragColor = mix(v_color, v_shader, step(0.25, d));
# ifdef LIT
  light(gl_FragColor, def_specular);
# endif
#endif

#ifdef MODE_PLATE
  gl_FragColor = texture2D(u_texture, v_tx_coord);
# ifdef LIT
  light(gl_FragColor, v_shader);
# endif
#endif

#ifdef MODE_TRANS
  float t = smoothstep(v_shader.s, v_shader.t, u_time) + smoothstep(v_shader.p, v_shader.q, u_time);
  vec4 c = texture2D(u_texture, v_tx_coord);
  float a = dot(c.rgb, thirds);
  gl_FragColor = vec4(t <= 1.0 ? mix(vec3(a,a,a), c.rgb, t) : mix(c.rgb, c.aaa, t - 1.0), 1.0);
# ifdef LIT
  light(gl_FragColor, v_color);
# endif
#endif

#ifdef MODE_FADE
  float t = smoothstep(v_shader.s, v_shader.t, u_time) + smoothstep(v_shader.p, v_shader.q, u_time);
  vec4 c = texture2D(u_texture, v_tx_coord);
  float a = c.a;
  gl_FragColor = vec4(t <= 1.0 ? mix(v_color.rgb, c.rgb, (t*t*t)*a + t*(1.0-a)) : mix(c.rgb, v_color.rgb, (t-1.0)*(t-1.0)*(t-1.0)*a + (t-1.0)*(1.0-a)), 1.0);
# ifdef LIT
  light(gl_FragColor, def_specular);
# endif
#endif

#ifdef MODE_BORDER
  float s = step(v_tx_coord.y, 1.0 - v_shader.a);
  gl_FragColor.rgb = mix(v_shader.rgb, v_color.rgb, s);
  gl_FragColor.a = v_color.a;
# ifdef LIT
  light(gl_FragColor, def_specular);
# endif
#endif

#ifdef MODE_NOISE
  float r = fract(sin(dot(floor(v_tx_coord) / 1024.0 ,vec2(12.9898,78.233)) + u_time) * 43758.5453);
  gl_FragColor.rgb = mix(v_shader.rgb, v_color.rgb, r);
  gl_FragColor.a = 1.0;
#endif

#ifdef MODE_PLASMA
  float scale = 4.0;
  vec2 c = v_tx_coord * v_shader.st - v_shader.pq;
  float v = 0.0;
  v += sin( (c.x + u_time)             );
  v += sin( (c.y + u_time)       * 0.5 );
  v += sin( (c.x + c.y + u_time) * 0.5 );
  c += scale * 0.5 * vec2( sin(u_time * 0.3), cos(u_time * 0.5) );
  v += sin( sqrt(dot(c,c) + 1.0) + u_time );
  //v += sin( length(c) + u_time );
  v = v * 0.5;
  //vec3 col = vec3(1, sin(PI*v), cos(PI*v));
  //gl_FragColor = vec4(col*.5 + .5, 1);
  v = clamp(sin(PI*v) * 0.5 + 0.5, 0.0, 1.0);
  /* TODO how do we handle the extra colour here? */
  gl_FragColor = mix(v_color, v_color, v);
#endif

#ifdef MODE_PULSE
  float t = fract((u_time - v_shader.s) / v_shader.t);
  vec2 v = v_tx_coord;
  float d = dot(v, v);
  float f = fract(d * v_shader.p * v_shader.p - t);
  gl_FragColor = d <= 0.25 && f <= v_shader.q ? v_color : zero4;
# ifdef LIT
    light(gl_FragColor, def_specular);
# endif
#endif

//TODO use v_color
#ifdef MODE_CONSOLE
  vec4 cc  = texture2D(u_data, v_tx_coord); // look up the character
  // geometry
  vec2 off = cc.xy * 255.0 * u_char_scale;           // the offset at which the character texture starts
  vec2 crd = v_tx_coord * v_shader.xy;             // the coordinates within the character matrix
  vec2 pos = fract(crd + vec2(-0.0001, 0.0));      // the relative position within the ASCII texture
  float p = texture2D(u_chars, off + pos * u_char_scale).r;
  // color
  float k = cc.z * 16.0;
  float fi = floor(k);
  float bi = floor(fract(k) * 16.0);
  vec4 fg = texture2D(u_palettes, vec2(fi/16.0, u_palette));
  vec4 bg = texture2D(u_palettes, vec2(bi/16.0, u_palette));
  gl_FragColor.rgb = mix(bg.rgb, fg.rgb, p);
  gl_FragColor.a = 1.0;
#endif

#ifdef MODE_CAUSTIC
  float speed = 0.25;
  float time = speed * u_time;
  vec2 p = mod(v_tx_coord * TAU, TAU) - 250.0;

    vec2 i = vec2(p);
	float c = 1.0;
	float inten = .005;
	float recip = 1.0 / inten;


  float f = time * 0.3;
  float sf = sin(f);
  float cf = cos(f);
  mat2 mf = mat2(cf, -sf, sf, cf);
  vec2 vt = vec2(cos(time), sin(time));

	for (int n = 0; n < CAUSTIC_MAX_ITER; n++) 
	{
/*
      float t = time * (1.0 - (3.5 / float(n+1)));
      i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
      c += recip / length(vec2(p.x / sin(t + i.x), p.y / cos(t + i.y)));
*/

      float cx = cos(i.x);
      float sx = sin(i.x);
      float cy = cos(i.y);
      float sy = sin(i.y);
      float ct = vt.x;
      float st = vt.y;
      i = p + vec2(
        //cos(t - i.x) + sin(t + i.y),
        ct*cx + st*sx + st*cy + ct*sy, 
        //sin(t - i.y) + cos(t + i.x)
        st*cy - ct*sy + ct*cx -st*sx
      );
      vec2 d = vec2(
        //sin(t + i.x),
        st*cx + ct*sx,
        //cos(t + i.y)
        ct*cy - st*sy
      );
      // accumulate the colour
      c += recip / length(p/d);
      // 'advance' the time
      vt *= mf;

	}
	c /= float(CAUSTIC_MAX_ITER);
	c = 1.17-pow(c, 1.4);
	vec3 colour = vec3(pow(abs(c), 8.0));
    colour = clamp(colour + vec3(0.0, 0.35, 0.5), 0.0, 1.0);
    

	gl_FragColor = vec4(colour, 1.0);

#endif

}
