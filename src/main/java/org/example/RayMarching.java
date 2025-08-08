package org.example;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.joml.*;

import java.io.IOException;
import java.lang.Math;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;

public class RayMarching {

    private long window;
    private int width = 1900;
    private int height = 1425;
    private int shaderProgram;
    private int vao;


    private Vector3f cameraPos = new Vector3f(0, 0, -3);
    private float yaw = (float) Math.toRadians(90), pitch = 0;
    private boolean firstMouse = true;
    private double lastX, lastY;

    public void run() {
        init();
        loop();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        if (!glfwInit()) throw new RuntimeException("Failed to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, "Ray Marching on GPU", 0, 0);
        if (window == 0) throw new RuntimeException("Failed to create window");

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(w, true);
        });

        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }
            double dx = xpos - lastX;
            double dy = ypos - lastY;
            lastX = xpos;
            lastY = ypos;

            yaw += dx * 0.0025;
            pitch -= dy * 0.0025;
            pitch = (float) Math.max(-Math.PI / 2, Math.min(Math.PI / 2, pitch));
        });

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void setupQuad() {
        float[] vertices = {
                -1, -1, 0, 0,
                1, -1, 1, 0,
                -1,  1, 0, 1,
                1,  1, 1, 1
        };

        int vbo = glGenBuffers();
        vao = glGenVertexArrays();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 8);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private int loadShader(String path, int type) throws IOException {
        String src = Files.readString(Paths.get(path));
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private void createShaderProgram() throws IOException {
        int vertexShader = loadShader("src/main/resources/shaders/vertex.glsl", GL_VERTEX_SHADER);
        int fragmentShader = loadShader("src/main/resources/shaders/fragment.glsl", GL_FRAGMENT_SHADER);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(shaderProgram));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private void loop() {
        try {
            createShaderProgram();
            setupQuad();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            processInput();

            glUseProgram(shaderProgram);
            glBindVertexArray(vao);

// перемести вычисление и передачу направлений сюда:
            Vector3f forward = new Vector3f(
                    (float) (Math.cos(yaw) * Math.cos(pitch)),
                    (float) Math.sin(pitch),
                    (float) (Math.sin(yaw) * Math.cos(pitch))
            ).normalize();

            Vector3f right = forward.cross(new Vector3f(0, 1, 0), new Vector3f()).normalize();
            Vector3f up = right.cross(forward, new Vector3f()).normalize(); // <- оставь только эту

            Vector3f uv = new Vector3f(0f, 0f, 0f);

            Vector3f rd = new Vector3f();
            rd.set(forward).mul(1.5f)
                    .fma(uv.x, right)
                    .fma(uv.y, up)
                    .normalize();

//            System.out.println("yaw = " + yaw);
//            System.out.println("pitch = " + pitch);
//            System.out.println("forward = " + forward);
//            System.out.println("right = " + right);
//            System.out.println("up = " + up);
//            System.out.println("rd = " + rd);


            int RotCamera = glGetUniformLocation(shaderProgram, "u_RotCamera");
            glUniform3f(glGetUniformLocation(shaderProgram, "u_Forward"), forward.x, forward.y, forward.z);
            glUniform3f(glGetUniformLocation(shaderProgram, "u_Right"), right.x, right.y, right.z);
            glUniform3f(glGetUniformLocation(shaderProgram, "u_Up"), up.x, up.y, up.z);

            glUniform3f(glGetUniformLocation(shaderProgram, "u_CameraPos"), cameraPos.x, cameraPos.y, cameraPos.z);
            glUniform3f(glGetUniformLocation(shaderProgram, "u_LightPos"), 10, 10, -10);
            glUniform1f(glGetUniformLocation(shaderProgram, "u_Time"), (float) glfwGetTime());
            glUniform2f(glGetUniformLocation(shaderProgram, "u_Resolution"), width, height);
            glUniform2f(glGetUniformLocation(shaderProgram, "u_Angles"), yaw, pitch);
            glUniform3f(RotCamera, yaw, pitch, 0);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void processInput() {
        float speed = 0.07f;

        Vector3f forward = new Vector3f(
                (float) (Math.cos(yaw) * Math.cos(pitch)),
                (float) Math.sin(pitch),
                (float) (Math.sin(yaw) * Math.cos(pitch))
        ).normalize();

        Vector3f right = forward.cross(new Vector3f(0, 1, 0), new Vector3f()).normalize();
        Vector3f up = right.cross(forward, new Vector3f()).normalize(); // <- оставь только эту


        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) cameraPos.add(forward.mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) cameraPos.sub(forward.mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) cameraPos.sub(right.mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) cameraPos.add(right.mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) cameraPos.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) cameraPos.y -= speed;

        // Передаём в шейдер basis vectors
        int locForward = glGetUniformLocation(shaderProgram, "u_Forward");
        int locRight = glGetUniformLocation(shaderProgram, "u_Right");
        int locUp = glGetUniformLocation(shaderProgram, "u_Up");
        int RotCamera = glGetUniformLocation(shaderProgram, "u_RotCamera");

        glUniform3f(locForward, forward.x, forward.y, forward.z);
        glUniform3f(locRight, right.x, right.y, right.z);
        glUniform3f(locUp, up.x, up.y, up.z);
        glUniform3f(RotCamera, yaw, pitch, 1);
    }


    public static void main(String[] args) {
        new RayMarching().run();
    }
}
