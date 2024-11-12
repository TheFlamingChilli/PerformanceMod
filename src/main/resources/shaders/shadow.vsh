#version 330 core
layout (location = 0) in vec3 vertexPosition;
layout (location = 1) in vec2 uvCoord;
layout (location = 2) in vec3 instancePosition;

out vec2 texCoord;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main() {
    vec3 offsetPos = vertexPosition;
    offsetPos.y += 0.001;
    gl_Position = projectionMatrix * viewMatrix * vec4(offsetPos + instancePosition, 1.0);
    texCoord = uvCoord;
}
