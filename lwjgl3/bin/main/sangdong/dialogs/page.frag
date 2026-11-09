#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_isInterior;
uniform vec4 u_region;
uniform vec2 u_sourceSize;
uniform vec4 u_interior;
uniform float u_radius;
uniform float u_feather;
varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    vec4 pixel = texture2D(u_texture, v_texCoords);
    // Remove nearly transparent matte noise and keep the page fully opaque inside.
    float alpha = smoothstep(0.05, 0.95, pixel.a);
    if (u_isInterior > 0.5) {
        vec2 position = (v_texCoords - u_region.xy) / u_region.zw * u_sourceSize;
        vec2 halfSize = u_interior.zw * 0.5;
        vec2 delta = abs(position - (u_interior.xy + halfSize)) - (halfSize - vec2(u_radius));
        float distance = length(max(delta, vec2(0.0))) + min(max(delta.x, delta.y), 0.0) - u_radius;
        alpha *= 1.0 - smoothstep(-u_feather, 0.0, distance);
    }
    gl_FragColor = v_color * vec4(pixel.rgb, alpha);
}
