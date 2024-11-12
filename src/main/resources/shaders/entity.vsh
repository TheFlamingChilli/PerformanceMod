#version 460 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 uvCoord;
layout(location = 2) in vec3 instancePosition;
layout(location = 3) in vec3 instanceRotation;
layout(location = 4) in float isHeadVertex;

out vec2 texCoord;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform vec3 headOffset;

void main() {
    float cosAngle = cos(-instanceRotation.x);
    float sinAngle = sin(-instanceRotation.x);
    mat3 entityYawMatrix = mat3(
        cosAngle, 0.0, sinAngle,
        0.0, 1.0, 0.0,
        -sinAngle, 0.0, cosAngle
    );
    mat3 entityRollMatrix = mat3(
            -1, 0, 0,
            0, -1, 0,
            0, 0, 1
    );
    float cosHeadAngle = cos(-instanceRotation.z);
    float sinHeadAngle = sin(-instanceRotation.z);
    mat3 headYawMatrix = mat3(
        cosHeadAngle, 0.0, sinHeadAngle,
        0.0, 1.0, 0.0,
        -sinHeadAngle, 0.0, cosHeadAngle
    );
    cosHeadAngle = cos(-instanceRotation.y);
    sinHeadAngle = sin(-instanceRotation.y);
    mat3 headPitchMatrix = mat3(
            1, 0, 0,
            0, cosHeadAngle, -sinHeadAngle,
            0, sinHeadAngle, cosHeadAngle
    );

    vec3 rotatedPosition;
    if (isHeadVertex == 1.0) {
        vec3 headVertexOriginalPosition = vertexPosition - headOffset;
        vec3 headVertexRotatedPosition = (headYawMatrix * headPitchMatrix * headVertexOriginalPosition) + headOffset;
        rotatedPosition = entityRollMatrix * entityYawMatrix * headVertexRotatedPosition;
    } else {
        rotatedPosition = entityRollMatrix * entityYawMatrix * vertexPosition;
    }
    rotatedPosition.y += 1.5;
    gl_Position = projectionMatrix * viewMatrix * vec4(rotatedPosition + instancePosition, 1.0);
    texCoord = uvCoord;
}
