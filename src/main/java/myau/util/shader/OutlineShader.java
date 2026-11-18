package myau.util.shader;

import java.awt.Color;
import org.lwjgl.opengl.GL20;

public class OutlineShader extends ShaderProgram {
    private static final String FRAGMENT_SHADER =
        "#version 120\n"
        + "uniform sampler2D texture;\n"
        + "uniform vec4 color;\n"
        + "void main() {\n"
        + "vec4 st = texture2D(texture, gl_TexCoord[0].st);\n"
        + "gl_FragColor = vec4(color.rgb, st.a > 0.0 ? color.a : 0.0);\n"
        + "}";

    public OutlineShader() {
        super(FRAGMENT_SHADER);
    }

    @Override
    public void setupUniforms() {
        this.registerUniform("texture");
        this.registerUniform("color");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform1i(this.getUniform("texture"), 0);
        GL20.glUniform4f(this.getUniform("color"), 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void setColor(Color color) {
        GL20.glUniform4f(
            this.getUniform("color"),
            (float) color.getRed() / 255.0f,
            (float) color.getGreen() / 255.0f,
            (float) color.getBlue() / 255.0f,
            (float) color.getAlpha() / 255.0f
        );
    }
}
