package org.moon.figura.avatars.model.rendering.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.lwjgl.BufferUtils;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatars.Avatar;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaFunctionOverload;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.FiguraVec2;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.math.vector.FiguraVec4;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.FiguraIdentifier;
import org.moon.figura.utils.LuaUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LuaWhitelist
@LuaTypeDoc(
        name = "Texture",
        value = "texture"
)
public class FiguraTexture extends AbstractTexture implements Closeable {

    /**
     * The ID of the texture, used to register to Minecraft.
     */
    public final ResourceLocation textureID;
    private boolean registered = false;
    private boolean dirty = true;
    private final String name;

    /**
     * Native image holding the texture data for this texture.
     */
    private final NativeImage texture;
    private boolean isClosed = false;

    public FiguraTexture(String name, byte[] data) {
        //Read image from wrapper
        NativeImage image;
        try {
            ByteBuffer wrapper = BufferUtils.createByteBuffer(data.length);
            wrapper.put(data);
            wrapper.rewind();
            image = NativeImage.read(wrapper);
        } catch (IOException e) {
            FiguraMod.LOGGER.error("", e);
            image = new NativeImage(1, 1, true);
        }

        this.texture = image;
        this.textureID = new FiguraIdentifier("avatar_tex/" + UUID.randomUUID());
        this.name = name;
    }

    public FiguraTexture(UUID owner, NativeImage image, String name) {
        this.texture = image;
        this.textureID = new FiguraIdentifier("avatar_tex/" + owner + "/" + name);
        this.name = name;
    }

    @Override
    public void load(ResourceManager manager) throws IOException {}

    //Called when a texture is first created and when it reloads
    //Registers the texture to minecraft, and uploads it to GPU.
    public void registerAndUpload() {
        if (!registered) {
            //Register texture under the ID, so Minecraft's rendering can use it.
            Minecraft.getInstance().getTextureManager().register(textureID, this);
            registered = true;
        }

        if (dirty) {
            //Upload texture to GPU.
            TextureUtil.prepareImage(this.getId(), texture.getWidth(), texture.getHeight());
            texture.upload(0, 0, 0, false);
            dirty = false;
        }
    }

    public int getWidth() {
        return texture.getWidth();
    }

    public int getHeight() {
        return texture.getHeight();
    }

    @Override
    public void close() {
        //Make sure it doesn't close twice (minecraft tries to close the texture when reloading textures
        if (isClosed) return;

        isClosed = true;

        //Close native image
        texture.close();

        //Cache GLID and then release it on GPU
        RenderSystem.recordRenderCall(() -> TextureUtil.releaseTextureId(this.id));
    }

    public void dirty() {
        this.dirty = true;
    }


    // -- lua stuff -- //


    private FiguraVec4 parseColor(String method, Object r, Double g, Double b, Double a) {
        return r instanceof FiguraVec3 vec3 ? vec3.augmented() : LuaUtils.parseVec4(method, r, g, b, a, 0, 0, 0, 1);
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "texture.get_name")
    public String getName() {
        return name;
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "texture.get_dimensions")
    public FiguraVec2 getDimensions() {
        return FiguraVec2.of(getWidth(), getHeight());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Integer.class, Integer.class},
                    argumentNames = {"x", "y"}
            ),
            value = "texture.get_pixel")
    public FiguraVec4 getPixel(int x, int y) {
        try {
            return ColorUtils.abgrToRGBA(texture.getPixelRGBA(x, y));
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "rgb"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "rgba"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "r", "g", "b", "a"}
                    )
            },
            value = "texture.set_pixel")
    public void setPixel(int x, int y, Object r, Double g, Double b, Double a) {
        try {
            texture.setPixelRGBA(x, y, ColorUtils.rgbaToIntABGR(parseColor("setPixel", r, g, b, a)));
            dirty();
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "width", "height", "rgb"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "width", "height", "rgba"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "width", "height", "r", "g", "b", "a"}
                    )
            },
            value = "texture.fill")
    public void fill(int x, int y, int width, int height, Object r, Double g, Double b, Double a) {
        try {
            texture.fillRect(x, y, width, height, ColorUtils.rgbaToIntABGR(parseColor("fill", r, g, b, a)));
            dirty();
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return name.isBlank() ? "Texture" : name + " (Texture)";
    }


    @LuaWhitelist
    @LuaMethodDoc("texture.create_context")
    public TextureEditContext createContext() {
        return new TextureEditContext(this);
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.apply")
    public void apply(TextureEditContext context) {
        context.changedPixels.forEach((pos, col)
                -> texture.setPixelRGBA(pos.x, pos.y, ColorUtils
                .rgbaToIntABGR(parseColor("setPixel", col.x,col.y,col.z,col.w))));
        dirty();
        registerAndUpload();
    }

    @LuaWhitelist
    @LuaTypeDoc(
            name = "TextureEditContext",
            value = "texture_edit_context"
    )
    public static class TextureEditContext{
        private final FiguraTexture textureToEdit;
        private final Map<PixelPos, FiguraVec4> changedPixels = new HashMap<>();
        public record PixelPos(int x, int y){}
        public TextureEditContext(FiguraTexture textureToEdit) {
            this.textureToEdit = textureToEdit;
        }

        @LuaWhitelist
        @LuaMethodDoc("texture_edit_context.get_pixel_at")
        public FiguraVec4 getPixelAt(int x, int y, Boolean unchanged) {
            if (unchanged == null) unchanged = true;
            PixelPos pixelPos = new PixelPos(x,y);
            FiguraVec4 data = changedPixels.get(pixelPos);
            if (data != null && !unchanged) return data;
            else return textureToEdit.getPixel(x,y);
        }

        @LuaWhitelist
        @LuaMethodDoc("texture_edit_context.set_pixel_at")
        public void setPixelAt(int x, int y, FiguraVec4 color) {
            changedPixels.put(new PixelPos(x,y), color);
        }

        @LuaWhitelist
        @LuaMethodDoc("texture_edit_context.cancel_change_at")
        public void cancelChangeAt(int x, int y) {
            PixelPos pos = new PixelPos(x,y);
            changedPixels.remove(pos);
        }

        @LuaWhitelist
        @LuaMethodDoc(value = "texture_edit_context.cancel_changes_in", overloads = {
                @LuaFunctionOverload(
                        argumentTypes = {
                            Integer.class,Integer.class,Integer.class,Integer.class
                        },
                        argumentNames = {"x1","y1","x2","y2"}
                ),
                @LuaFunctionOverload(
                        argumentTypes = {FiguraVec4.class},
                        argumentNames = {"rect"}
                )
        })
        public void cancelChangesIn(Object x1, int y1, int x2, int y2) {
            int uX, uY, dX, dY;
            if (x1 instanceof FiguraVec4 vec4) {
                uX = Math.max((int) vec4.x, (int) vec4.z);
                uY = Math.max((int) vec4.y, (int) vec4.w);
                dX = Math.min((int) vec4.x, (int) vec4.z);
                dY = Math.min((int) vec4.y, (int) vec4.w);
            }
            else {
                try {
                    uX = Math.max((int)x1,x2);
                    uY = Math.max(y1,y2);
                    dX = Math.min((int)x1,x2);
                    dY = Math.min(y1,y2);
                } catch (ClassCastException e) {
                    throw new LuaError("First argument need to be FiguraVec4 or int");
                }
            }

            changedPixels.forEach((pos, pix) -> {
                if (pos.x >= dX && pos.x <= uX
                        && pos.y >= dY && pos.y <= uY)
                {
                    changedPixels.remove(pos);
                }
            });
        }

        @LuaWhitelist
        @LuaMethodDoc(value = "texture_edit_context.fill_rect", overloads = {
                @LuaFunctionOverload(
                        argumentTypes = {
                                FiguraVec4.class,Integer.class,Integer.class,Integer.class,Integer.class
                        },
                        argumentNames = {"color","x1","y1","x2","y2"}
                ),
                @LuaFunctionOverload(
                        argumentTypes = {FiguraVec4.class,FiguraVec4.class},
                        argumentNames = {"color","rect"}
                )
        })
        public void fillRect(FiguraVec4 color, Object x1, int y1, int x2, int y2) {
            int uX, uY, dX, dY;
            if (x1 instanceof FiguraVec4 vec4) {
                uX = Math.max((int) vec4.x, (int) vec4.z);
                uY = Math.max((int) vec4.y, (int) vec4.w);
                dX = Math.min((int) vec4.x, (int) vec4.z);
                dY = Math.min((int) vec4.y, (int) vec4.w);
            }
            else {
                try {
                    uX = Math.max((int)x1,x2);
                    uY = Math.max(y1,y2);
                    dX = Math.min((int)x1,x2);
                    dY = Math.min(y1,y2);
                } catch (ClassCastException e) {
                    throw new LuaError("First argument need to be FiguraVec4 or int");
                }
            }

            for (int x = dX; x <= uX; x++) {
                for (int y = dY; y <= uY; y++) {
                    setPixelAt(x,y,color);
                }
            }
        }

        @LuaWhitelist
        @LuaMethodDoc(value = "texture_edit_context.fill_ellipse", overloads = {
                @LuaFunctionOverload(
                        argumentTypes = {
                                FiguraVec4.class,Integer.class,Integer.class,Integer.class,Integer.class
                        },
                        argumentNames = {"color","x1","y1","x2","y2"}
                ),
                @LuaFunctionOverload(
                        argumentTypes = {FiguraVec4.class,FiguraVec4.class},
                        argumentNames = {"color","rect"}
                )
        })
        public void fillEllipse(FiguraVec4 color, Object x1, int y1, int x2, int y2) {
            int uX, uY, dX, dY;
            if (x1 instanceof FiguraVec4 vec4) {
                uX = Math.max((int) vec4.x, (int) vec4.z);
                uY = Math.max((int) vec4.y, (int) vec4.w);
                dX = Math.min((int) vec4.x, (int) vec4.z);
                dY = Math.min((int) vec4.y, (int) vec4.w);
            }
            else {
                try {
                    uX = Math.max((int)x1,x2);
                    uY = Math.max(y1,y2);
                    dX = Math.min((int)x1,x2);
                    dY = Math.min(y1,y2);
                } catch (ClassCastException e) {
                    throw new LuaError("First argument need to be FiguraVec4 or int");
                }
            }

            float cX = (uX + dX) / 2f;
            float cY = (uY + dY) / 2f;
            float rX = (uX - cX);
            float rY = (uY - cY);
            float aspectRatio = rY / rX;

            for (int x = dX; x <= uX; x++) {
                for (int y = dY; y <= uY; y++) {
                    float distX = (x - cX);
                    float distY = (y - cY) / aspectRatio;

                    float dist = (float) Math.sqrt(Math.pow(distX, 2.0f) + Math.pow(distY, 2.0f)) ;
                    if (dist <= Math.ceil(rX)) setPixelAt(x,y,color);
                }
            }

        }

        @LuaWhitelist
        public Object __index(String value) {
            return switch (value) {
                case "width" -> textureToEdit.getWidth();
                case "height" -> textureToEdit.getHeight();
                default -> null;
            };
        }
    }
}