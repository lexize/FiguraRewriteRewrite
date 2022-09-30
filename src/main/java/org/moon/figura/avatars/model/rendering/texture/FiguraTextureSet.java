package org.moon.figura.avatars.model.rendering.texture;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.moon.figura.avatars.Avatar;
import org.moon.figura.mixin.render.layers.elytra.ElytraLayerAccessor;
import org.moon.figura.utils.FiguraIdentifier;

import java.util.UUID;

public class FiguraTextureSet {

    public final String name;
    public final FiguraTexture mainTex, emissiveTex;

    public FiguraTextureSet(String name, byte[] mainData, byte[] emissiveData, Avatar avatar) {
        this.name = name;
        mainTex = mainData == null ? null : new FiguraTexture(name, mainData);
        emissiveTex = emissiveData == null ? null : new FiguraTexture(name, emissiveData);
    }

    public void clean() {
        if (mainTex != null)
            mainTex.close();
        if (emissiveTex != null)
            emissiveTex.close();
    }

    public void uploadIfNeeded() {
        if (mainTex != null)
            mainTex.registerAndUpload();
        if (emissiveTex != null)
            emissiveTex.registerAndUpload();
    }

    public int getWidth() {
        if (mainTex != null)
            return mainTex.getWidth();
        else if (emissiveTex != null)
            return emissiveTex.getWidth();
        else
            return -1;
    }

    public int getHeight() {
        if (mainTex != null)
            return mainTex.getHeight();
        else if (emissiveTex != null)
            return emissiveTex.getHeight();
        else
            return -1;
    }

    public ResourceLocation getOverrideTexture(UUID owner, Pair<OverrideType, Object> pair) {
        OverrideType type;

        if (pair == null || (type = pair.getFirst()) == null)
            return null;

        return switch (type) {
            case SKIN, CAPE, ELYTRA -> {
                if (Minecraft.getInstance().player == null)
                    yield null;

                PlayerInfo info = Minecraft.getInstance().player.connection.getPlayerInfo(owner);
                if (info == null)
                    yield null;

                yield switch (type) {
                    case CAPE -> info.getCapeLocation();
                    case ELYTRA -> info.getElytraLocation() == null ? ElytraLayerAccessor.getWingsLocation() : info.getElytraLocation();
                    default -> info.getSkinLocation();
                };
            }
            case RESOURCE -> {
                try {
                    ResourceLocation resource = new ResourceLocation(String.valueOf(pair.getSecond()));
                    yield Minecraft.getInstance().getResourceManager().getResource(resource).isPresent() ? resource : MissingTextureAtlasSprite.getLocation();
                } catch (Exception ignored) {
                    yield MissingTextureAtlasSprite.getLocation();
                }
            }
            case PRIMARY -> mainTex == null ? null : mainTex.textureID;
            case SECONDARY -> emissiveTex == null ? null : emissiveTex.textureID;
            case CUSTOM -> {
                if (pair.getSecond() instanceof FiguraTexture texture)
                    yield texture.textureID;
                else if (pair.getSecond() instanceof String string)
                    yield new FiguraIdentifier("avatar_tex/" + owner + "/" + string);

                yield MissingTextureAtlasSprite.getLocation();
            }
        };
    }

    public enum OverrideType {
        SKIN,
        CAPE,
        ELYTRA,
        RESOURCE,
        PRIMARY,
        SECONDARY,
        CUSTOM
    }
}
