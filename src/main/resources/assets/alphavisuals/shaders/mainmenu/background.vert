#version 150

in vec3 Position;
in vec2 UV;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    texCoord = UV;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}