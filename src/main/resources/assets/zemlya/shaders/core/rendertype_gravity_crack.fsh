#version 150

uniform sampler2D Sampler0;

uniform Globals {
    ivec3 CameraPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec4 vertexColor;
in vec2 texCoord0;
in float vInteraction;
in float vIsGlow;
in float vDistFade;

out vec4 fragColor;

void main() {
    vec4 centerTex = texture(Sampler0, texCoord0);
    
    // Base Crack (Ground layer)
    if (vIsGlow < 0.5) {
        if (centerTex.a < 0.1) {
            discard;
        }
        
        // Ground crack can pulse slightly too
        float timeSecs = GameTime * 1200.0;
        float pulse = (sin(timeSecs * 6.2831853) * 0.5 + 0.5);
        float glowIntensity = 0.3 + pulse * 0.4 + (vInteraction * 2.0);
        
        vec3 glowColor = vec3(0.6, 0.3, 0.9);
        vec3 crackColor = mix(vec3(0.1, 0.0, 0.15), glowColor, min(1.0, glowIntensity * 0.5));
        
        fragColor = vec4(crackColor, centerTex.a * vertexColor.a);
        return;
    }
    
    // Volumetric Glow Layers (Elevated layers)
    // The height of the layer (t) is passed via vIsGlow (from 0.0 to 1.0)
    float t = vIsGlow;
    
    // As it gets higher, we spread out the texture sampling for a soft blur
    float stepDist = 0.015 * t;
    
    float sumAlpha = 0.0;
    for(int y = -1; y <= 1; y++) {
        for(int x = -1; x <= 1; x++) {
            vec2 offset = vec2(x, y) * stepDist;
            sumAlpha += texture(Sampler0, texCoord0 + offset).a;
        }
    }
    float blurAlpha = sumAlpha / 9.0;
    // Boost slightly so it doesn't fade too quickly when blurring a thin line
    blurAlpha = min(1.0, blurAlpha * 2.0);
    
    if (blurAlpha < 0.01) {
        discard;
    }
    
    // Smooth opacity based on how high the layer is (passed via vertexColor.a)
    float baseAlpha = vertexColor.a;
    
    float timeSecs = GameTime * 1200.0;
    float pulse = (sin(timeSecs * 6.2831853) * 0.5 + 0.5); 
    
    float glowIntensity = 0.4 + pulse * 0.6;
    float interactionBoost = vInteraction * 2.0; 
    
    // Boost brightness in the center of the field
    float centerBoost = vDistFade * 0.6; 
    glowIntensity += interactionBoost + centerBoost;
    
    vec3 glowColor = vec3(0.6, 0.3, 0.9);
    
    // The glow beam fades smoothly with the layer's height
    float finalAlpha = blurAlpha * glowIntensity * 0.8 * baseAlpha;
    
    if (finalAlpha < 0.01) {
        discard;
    }
    
    fragColor = vec4(glowColor, finalAlpha);
}
