package util.serialized;

import lombok.Data;
import util.Vector3f;
import util.Vector4f;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */

@Data
public strictfp class LightingSettings {
    private float LightingMultiplier;
    private Vector3f SunDirection;
    private Vector3f SunAmbience;
    private Vector3f SunColor;
    private Vector3f ShadowFillColor;
    private Vector4f SpecularColor;
    private float Bloom;
    private Vector3f FogColor;
    private float FogStart;
    private float FogEnd;

}