#version 150

uniform vec4 HighlightColor;
uniform float PulseTime;
out vec4 fragColor;

void main() {
    float pulse = (13.0 + 11.0 * sin(PulseTime * 6.28318530718 / 1.2)) / 255.0;
    fragColor = vec4(HighlightColor.rgb, HighlightColor.a * pulse);
}
