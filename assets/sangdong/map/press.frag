#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_center;
uniform vec2 u_size;
uniform float u_radius;
uniform float u_press;

void main() {
    vec2 offset = (v_texCoords - u_center) * u_size;
    vec4 color = texture2D(u_texture, v_texCoords);
    // The badge and road share one image. Anchor the rim and deform only its inner face.
    float influence = 1.0 - smoothstep(u_radius * 0.72, u_radius * 0.90, length(offset));
    if (influence > 0.0) {
        vec2 local = offset - vec2(0.0, 5.0 * u_press * influence);
        local /= vec2(1.0 - 0.06 * u_press * influence, 1.0 - 0.10 * u_press * influence);
        color = texture2D(u_texture, u_center + local / u_size);
        color.rgb *= 1.0 - 0.10 * u_press * influence;
    }
    gl_FragColor = v_color * color;
}
