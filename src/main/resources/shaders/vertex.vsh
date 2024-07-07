#version 330 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 uvCoord;
layout(location = 2) in vec4 instanceTransform;
out vec2 texCoord;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main() {
    float cosAngle = cos(-instanceTransform.w);
    float sinAngle = sin(-instanceTransform.w);
    mat3 yawRotationMatrix = mat3(
        cosAngle, 0.0, sinAngle,
        0.0, 1.0, 0.0,
        -sinAngle, 0.0, cosAngle
    );
    mat3 rollRotationMatrix = mat3(
            -1, 0, 0,
            0, -1, 0,
            0, 0, 1
    );
    vec3 rotatedPosition = rollRotationMatrix * yawRotationMatrix * vertexPosition;
    rotatedPosition.y += 1.5;
    gl_Position = projectionMatrix * viewMatrix * vec4(rotatedPosition + instanceTransform.xyz, 1.0);
    texCoord = uvCoord;
}
