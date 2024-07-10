#version 330 core

in vec2 texCoord;
out vec4 fragColor;
out vec4 normal;

uniform sampler2D textureSampler;

void main() {
    fragColor = texture(textureSampler, texCoord);
}
