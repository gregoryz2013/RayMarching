#version 330 core

layout(location = 0) in vec2 position;
out vec2 v_UV;

void main() {
    v_UV = position * 0.5 + 0.5; // от [-1,1] к [0,1]
    gl_Position = vec4(position, 0.0, 1.0);
}