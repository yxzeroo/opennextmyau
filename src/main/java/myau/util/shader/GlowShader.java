package myau.util.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class GlowShader extends ShaderProgram {
    private static final String FRAGMENT_SHADER =
        "#version 120\n"
        + "uniform sampler2D texture;\n"
        + "uniform vec2 size;\n"
        + "uniform float radius;\n"
        + "void main(void) {\n"
        + "gl_FragColor = vec4(0.0);\n"
        + "vec4 xy = texture2D(texture, gl_TexCoord[0].xy);\n"
        + "if (xy.a == 0.0) {\n"
        + "for (float x = -radius; x <= radius; x++) {\n"
        + "for (float y = -radius; y <= radius; y++) {\n"
        + "vec4 color = texture2D(texture, gl_TexCoord[0].xy + vec2(size.x * x, size.y * y));\n"
        + "if (color.a != 0.0) {\n"
        + "gl_FragColor = color;\n"
        + "}\n"
        + "}\n"
        + "}\n"
        + "}\n"
        + "}";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public GlowShader() {
        super(FRAGMENT_SHADER);
    }

    @Override
    public void setupUniforms() {
        this.registerUniform("texture");
        this.registerUniform("size");
        this.registerUniform("radius");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform1i(this.getUniform("texture"), 0);
        GL20.glUniform2f(this.getUniform("size"), 1.0f / (float) mc.displayWidth, 1.0f / (float) mc.displayHeight);
        GL20.glUniform1f(this.getUniform("radius"), 1.5f);
    }
}
