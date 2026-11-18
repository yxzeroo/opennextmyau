package myau.util.shader;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

public abstract class ShaderProgram {
    private final Map<String, Integer> uniforms = new HashMap<>();
    private int programId;

    private int compileShader(String source, int shaderType) {
        int shaderId = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, 35713) == 0) {
            return -1;
        }
        return shaderId;
    }

    private void initProgram(String fragmentSource) {
        this.programId = GL20.glCreateProgram();
        GL20.glAttachShader(
            this.programId,
            this.compileShader(
                "#version 120\nvoid main(void) {\ngl_TexCoord[0] = gl_MultiTexCoord0;\ngl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}",
                35633
            )
        );
        GL20.glAttachShader(this.programId, this.compileShader(fragmentSource, 35632));
        GL20.glLinkProgram(this.programId);
        if (GL20.glGetProgrami(this.programId, 35714) == 0) {
            this.programId = -1;
            return;
        }
        this.setupUniforms();
    }

    public ShaderProgram(String fragmentSource) {
        this.initProgram(fragmentSource);
    }

    public int getUniform(String name) {
        return this.uniforms.get(name);
    }

    public void registerUniform(String name) {
        this.uniforms.put(name, GL20.glGetUniformLocation(this.programId, name));
    }

    public abstract void setupUniforms();

    public abstract void updateUniforms();

    public void use() {
        GL20.glUseProgram(this.programId);
        this.updateUniforms();
    }

    public void stop() {
        GL20.glUseProgram(0);
    }
}
