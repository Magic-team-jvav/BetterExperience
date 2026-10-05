#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform sampler2D BoundsSampler;

const vec3 CORNERS[8] = vec3[8](
    vec3(0, 0, 0), vec3(1, 0, 0), vec3(1, 1, 0), vec3(0, 1, 0),
    vec3(0, 0, 1), vec3(1, 0, 1), vec3(1, 1, 1), vec3(0, 1, 1)
);
const int INDICES[36] = int[36](
    0, 1, 2, 0, 2, 3, 4, 7, 6, 4, 6, 5,
    0, 3, 7, 0, 7, 4, 1, 5, 6, 1, 6, 2,
    0, 4, 5, 0, 5, 1, 3, 2, 6, 3, 6, 7
);

vec3 boundsTexel(int index) {
    int width = textureSize(BoundsSampler, 0).x;
    return texelFetch(BoundsSampler, ivec2(index % width, index / width), 0).xyz;
}

void main() {
    vec3 lower = boundsTexel(gl_InstanceID * 2);
    vec3 upper = boundsTexel(gl_InstanceID * 2 + 1);
    vec3 position = mix(lower, upper, CORNERS[INDICES[gl_VertexID]]);
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
}
