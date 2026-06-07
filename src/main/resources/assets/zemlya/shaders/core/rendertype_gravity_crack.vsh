#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform Projection {
    mat4 ProjMat;
};

out vec4 vertexColor;
out vec2 texCoord0;
out float vInteraction;
out float vIsGlow;
out float vDistFade;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
    
    vertexColor = vec4(Color.rgb * ColorModulator.rgb, Color.a * ColorModulator.a);
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    
    vInteraction = Color.r;
    vIsGlow = Color.g;
    vDistFade = Color.b;
    
    vertexColor.r = 1.0; 
    vertexColor.g = 1.0;
    vertexColor.b = 1.0;
}
