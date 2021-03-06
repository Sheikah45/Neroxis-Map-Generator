package generator;

import map.*;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static util.ImageUtils.readImage;
import static util.ImageUtils.scaleImage;

public strictfp class PreviewGenerator {

    private static final String MASS_IMAGE = "/images/map_markers/mass.png";
    private static final String HYDRO_IMAGE = "/images/map_markers/hydro.png";
    private static final String ARMY_IMAGE = "/images/map_markers/army.png";

    public static void generate(BufferedImage image, SCMap map) {
        Graphics2D graphics = image.createGraphics();
        TerrainMaterials materials = map.getBiome().getTerrainMaterials();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            if (!materials.getTexturePaths()[i].isEmpty()) {
                BufferedImage layer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D layerGraphics = layer.createGraphics();
                layerGraphics.setColor(materials.getPreviewColors()[i]);
                layerGraphics.fillRect(0, 0, 256, 256);
                BufferedImage shadedLayer = getShadedImage(layer, map, i, true);
                TexturePaint layerPaint = new TexturePaint(shadedLayer, new Rectangle2D.Float(0, 0, 256, 256));
                graphics.setPaint(layerPaint);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
        }
        BufferedImage waterLayer = getWaterLayer(map);
        TexturePaint layerPaint = new TexturePaint(waterLayer, new Rectangle2D.Float(0, 0, 256, 256));
        graphics.setPaint(layerPaint);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    public static BufferedImage addMarkers(BufferedImage image, SCMap map) throws IOException {
        int resourceImageSize = 5;
        BufferedImage massImage = scaleImage(readImage(MASS_IMAGE), resourceImageSize, resourceImageSize);
        BufferedImage hydroImage = scaleImage(readImage(HYDRO_IMAGE), resourceImageSize, resourceImageSize);
        BufferedImage armyImage = scaleImage(readImage(ARMY_IMAGE), resourceImageSize, resourceImageSize);
        for (Mex mex : map.getMexes()) {
            if (mex != null) {
                int x = (int) (mex.getPosition().x / map.getSize() * 256 - massImage.getWidth(null) / 2);
                int y = (int) (mex.getPosition().z / map.getSize() * 256 - massImage.getHeight(null) / 2);
                x = StrictMath.min(Math.max(0, x), image.getWidth() - massImage.getWidth(null));
                y = StrictMath.min(Math.max(0, y), image.getHeight() - massImage.getHeight(null));
                image.getGraphics().drawImage(massImage, x, y, null);
            }
        }
        for (Hydro hydro : map.getHydros()) {
            if (hydro != null) {
                int x = (int) (hydro.getPosition().x / map.getSize() * 256 - hydroImage.getWidth(null) / 2);
                int y = (int) (hydro.getPosition().z / map.getSize() * 256 - hydroImage.getHeight(null) / 2);
                x = StrictMath.min(Math.max(0, x), image.getWidth() - hydroImage.getWidth(null));
                y = StrictMath.min(Math.max(0, y), image.getHeight() - hydroImage.getHeight(null));
                image.getGraphics().drawImage(hydroImage, x, y, null);
            }
        }
        for (Spawn spawn : map.getSpawns()) {
            if (spawn != null) {
                int x = (int) (spawn.getPosition().x / map.getSize() * 256 - armyImage.getWidth(null) / 2);
                int y = (int) (spawn.getPosition().z / map.getSize() * 256 - armyImage.getHeight(null) / 2);
                x = StrictMath.min(Math.max(0, x), image.getWidth() - armyImage.getWidth(null));
                y = StrictMath.min(Math.max(0, y), image.getHeight() - armyImage.getHeight(null));
                image.getGraphics().drawImage(armyImage, x, y, null);
            }
        }
        return image;
    }

    static BufferedImage getShadedImage(BufferedImage image, SCMap map, int layerIndex, boolean useAlpha) {
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = scaleImage(heightMap, 256, 256);

        BufferedImage textureLowMap = map.getTextureMasksLow();
        BufferedImage textureLowScaled = scaleImage(textureLowMap, 256, 256);

        BufferedImage textureHighMap = map.getTextureMasksHigh();
        BufferedImage textureHighScaled = scaleImage(textureHighMap, 256, 256);

        float ambientCoefficient = .5f;
        float landDiffuseCoefficient = .5f;
        float landSpecularCoefficient = .5f;
        float landShininess = 1f;
        float azimuth = lightingSettings.getSunDirection().getAzimuth() + 90f;
        float elevation = lightingSettings.getSunDirection().getElevation();
        int imageHeight = heightMapScaled.getHeight();
        int imageWidth = heightMapScaled.getWidth();
        int xOffset = (int) StrictMath.round(StrictMath.sin(StrictMath.toRadians(azimuth)));
        int yOffset = (int) StrictMath.round(StrictMath.cos(StrictMath.toRadians(azimuth)));

        int[] textureAlphas = new int[4];
        int[] origRGBA = new int[4];
        int[] newRGBA = new int[4];
        int relativeLayerIndex;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (layerIndex < 5) {
                    textureLowScaled.getRaster().getPixel(x, y, textureAlphas);
                    relativeLayerIndex = layerIndex;
                } else {
                    textureHighScaled.getRaster().getPixel(x, y, textureAlphas);
                    relativeLayerIndex = layerIndex - 4;
                }
                image.getRaster().getPixel(x, y, origRGBA);
                if (x - xOffset >= 0
                        && x - xOffset < imageWidth
                        && y - yOffset >= 0
                        && y - yOffset < imageHeight) {

                    int[] heightArray1 = new int[1];
                    int[] heightArray2 = new int[1];

                    heightMapScaled.getRaster().getPixel(x, y, heightArray1);
                    heightMapScaled.getRaster().getPixel(x - xOffset, y - yOffset, heightArray2);

                    float slope = (heightArray1[0] - heightArray2[0]) * map.getHeightMapScale();
                    float slopeAngle = (float) (180f - StrictMath.toDegrees(StrictMath.atan2(slope, StrictMath.sqrt(xOffset * xOffset + yOffset * yOffset))));
                    float normalAngle = slopeAngle - 90;
                    float reflectedAngle = normalAngle * 2 - elevation;
                    float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(normalAngle - elevation)) * landDiffuseCoefficient, 0));
                    float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(90 - reflectedAngle)), landShininess) * landSpecularCoefficient, 0));

                    newRGBA[0] = (int) (origRGBA[0] * (lightingSettings.getSunColor().x * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().x);
                    newRGBA[1] = (int) (origRGBA[1] * (lightingSettings.getSunColor().y * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().y);
                    newRGBA[2] = (int) (origRGBA[2] * (lightingSettings.getSunColor().z * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().z);
                } else {
                    newRGBA = origRGBA.clone();
                }

                newRGBA[0] = StrictMath.max(StrictMath.min(newRGBA[0], 255), 0);
                newRGBA[1] = StrictMath.max(StrictMath.min(newRGBA[1], 255), 0);
                newRGBA[2] = StrictMath.max(StrictMath.min(newRGBA[2], 255), 0);
                if (relativeLayerIndex > 0 && useAlpha) {
                    newRGBA[3] = StrictMath.max(StrictMath.min((int) ((textureAlphas[relativeLayerIndex - 1] - 128) / 127f * 255f), 255), 0);
                } else {
                    newRGBA[3] = 255;
                }
                image.getRaster().setPixel(x, y, newRGBA);
            }
        }
        return image;
    }

    static BufferedImage getWaterLayer(SCMap map) {
        Color shallowColor = new Color(134, 233, 233);
        Color abyssColor = new Color(35, 49, 162);
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        WaterSettings waterSettings = map.getBiome().getWaterSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = scaleImage(heightMap, 256, 256);

        BufferedImage waterLayer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D waterLayerGraphics = waterLayer.createGraphics();

        float elevation = lightingSettings.getSunDirection().getElevation();
        float[] mapElevations = {map.getBiome().getWaterSettings().getElevation(), map.getBiome().getWaterSettings().getElevationAbyss()};
        float ambientCoefficient = .5f;
        float waterDiffuseCoefficient = .25f;
        float waterSpecularCoefficient = .25f;
        float waterShininess = 1f;
        int[] heightArray = new int[1];
        int[] newRGBA = new int[4];
        BufferedImage layer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < waterLayer.getHeight(); y++) {
            for (int x = 0; x < waterLayer.getWidth(); x++) {
                heightMapScaled.getRaster().getPixel(x, y, heightArray);

                float mapElevation = heightArray[0] * map.getHeightMapScale();
                float weight = StrictMath.min(StrictMath.max((mapElevations[0] - mapElevation) / (mapElevations[0] - mapElevations[1]), 0), 1);
                float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(elevation)) * waterDiffuseCoefficient, 0));
                float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(180 - elevation)), waterShininess) * waterSpecularCoefficient, 0));

                newRGBA[0] = (int) (shallowColor.getRed() * (1 - weight) + abyssColor.getRed() * weight);
                newRGBA[1] = (int) (shallowColor.getGreen() * (1 - weight) + abyssColor.getGreen() * weight);
                newRGBA[2] = (int) (shallowColor.getBlue() * (1 - weight) + abyssColor.getBlue() * weight);
                newRGBA[0] *= (lightingSettings.getSunColor().x + waterSettings.getSurfaceColor().x) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[1] *= (lightingSettings.getSunColor().y + waterSettings.getSurfaceColor().y) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[2] *= (lightingSettings.getSunColor().z + waterSettings.getSurfaceColor().z) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[0] = StrictMath.min(255, newRGBA[0]);
                newRGBA[1] = StrictMath.min(255, newRGBA[1]);
                newRGBA[2] = StrictMath.min(255, newRGBA[2]);
                newRGBA[3] = (int) StrictMath.min(255 * weight, 255);

                layer.getRaster().setPixel(x, y, newRGBA);
            }
        }

        TexturePaint layerPaint = new TexturePaint(layer, new Rectangle2D.Float(0, 0, 256, 256));
        waterLayerGraphics.setPaint(layerPaint);
        waterLayerGraphics.fillRect(0, 0, 256, 256);
        return waterLayer;
    }
}
