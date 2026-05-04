#version 150

uniform float Time;
uniform float Fade;
uniform vec2 Resolution;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p, int octaves, float persistence, float lacunarity) {
    float value = 0.0;
    float amp = 0.5;
    float freq = 4.0;
    for(int i = 0; i < 5; i++) {
        if(i >= octaves) break;
        value += amp * noise(p * freq);
        amp *= persistence;
        freq *= lacunarity;
    }
    return value;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 uv2 = gl_FragCoord.xy / Resolution.xy * 2.0 - 1.0;

    float t = Time * 0.2;

    vec2 p = uv * 4.0;
    p.x += t * 0.3;
    p.y += t * 0.2;

    float n1 = fbm(p, 4, 0.5, 2.0);
    float n2 = fbm(p * 2.0 + t, 3, 0.6, 2.2);
    float n3 = fbm(p * 4.0 - t * 1.5, 3, 0.4, 2.5);

    float dist = length(uv2);
    float vignette = 1.0 - smoothstep(0.3, 1.0, dist) * 0.5;

    float r = 0.4 + 0.3 * sin(Time * 0.5 + uv.x * 5.0) + n1 * 0.2;
    float g = 0.2 + 0.4 * sin(Time * 0.7 + uv.y * 6.0 + 2.0) + n2 * 0.3;
    float b = 0.6 + 0.4 * sin(Time * 0.6 + (uv.x + uv.y) * 4.0 + 4.0) + n3 * 0.4;

    r = clamp(r, 0.2, 1.0);
    g = clamp(g, 0.1, 0.9);
    b = clamp(b, 0.4, 1.0);

    vec3 color1 = vec3(0.4, 0.2, 0.8);
    vec3 color2 = vec3(0.8, 0.3, 0.6);
    vec3 color3 = vec3(0.2, 0.1, 0.5);

    vec3 finalColor = mix(color1, color2, n1);
    finalColor = mix(finalColor, color3, n2);
    finalColor += vec3(r * 0.3, g * 0.2, b * 0.4);

    float alpha = Fade * vignette;

    fragColor = vec4(finalColor, alpha);
}