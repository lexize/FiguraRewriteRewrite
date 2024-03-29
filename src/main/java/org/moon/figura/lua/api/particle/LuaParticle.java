package org.moon.figura.lua.api.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.WakeParticle;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaMethodOverload;
import org.moon.figura.lua.docs.LuaMethodShadow;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.math.vector.FiguraVec4;
import org.moon.figura.mixin.particle.ParticleAccessor;
import org.moon.figura.trust.Trust;
import org.moon.figura.utils.LuaUtils;

@LuaWhitelist
@LuaTypeDoc(
        name = "Particle",
        value = "particle"
)
public class LuaParticle {

    private final String name;
    private final Avatar owner;
    private final Particle particle;

    private FiguraVec3 pos = FiguraVec3.of();
    private FiguraVec3 vel = FiguraVec3.of();
    private FiguraVec4 color = FiguraVec4.of(1, 1, 1, 1);
    private float power, scale = 1f;

    public LuaParticle(String name, Particle particle, Avatar owner) {
        this.name = name;
        this.particle = particle;
        this.owner = owner;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.spawn")
    public LuaParticle spawn() {
        if (!Minecraft.getInstance().isPaused()) {
            if (owner.particlesRemaining.use()) {
                ParticleAPI.getParticleEngine().figura$spawnParticle(particle, owner.owner);
                owner.trustIssues.remove(Trust.PARTICLES);
            } else {
                owner.trustIssues.add(Trust.PARTICLES);
            }
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.remove")
    public LuaParticle remove() {
        particle.remove();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.is_alive")
    public boolean isAlive() {
        return particle.isAlive();
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_pos")
    public FiguraVec3 getPos() {
        return pos.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "particle.set_pos")
    public void setPos(Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setPos", x, y, z);
        particle.setPos(vec.x, vec.y, vec.z);

        ParticleAccessor p = (ParticleAccessor) particle;
        p.setXo(vec.x);
        p.setYo(vec.y);
        p.setZo(vec.z);
        this.pos = vec;
    }

    @LuaWhitelist
    @LuaMethodShadow("setPos")
    public LuaParticle pos(Object x, Double y, Double z) {
        setPos(x, y, z);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_velocity")
    public FiguraVec3 getVelocity() {
        return vel.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "velocity"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "particle.set_velocity")
    public void setVelocity(Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setVelocity", x, y, z);
        particle.setParticleSpeed(vec.x, vec.y, vec.z);
        this.vel = vec;
    }

    @LuaWhitelist
    @LuaMethodShadow("setVelocity")
    public LuaParticle velocity(Object x, Double y, Double z) {
        setVelocity(x, y, z);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_color")
    public FiguraVec4 getColor() {
        return color.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rgb"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec4.class,
                            argumentNames = "rgba"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b", "a"}
                    )
            },
            value = "particle.set_color")
    public void setColor(Object r, Double g, Double b, Double a) {
        FiguraVec4 vec = LuaUtils.parseVec4("setColor", r, g, b, a, 1, 1, 1, 1);
        particle.setColor((float) vec.x, (float) vec.y, (float) vec.z);
        ((ParticleAccessor) particle).setParticleAlpha((float) vec.w);
        this.color = vec;
    }

    @LuaWhitelist
    @LuaMethodShadow("setColor")
    public LuaParticle color(Object r, Double g, Double b, Double a) {
        setColor(r, g, b, a);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_lifetime")
    public int getLifetime() {
        return particle.getLifetime();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Integer.class,
                    argumentNames = "lifetime"
            ),
            value = "particle.set_lifetime")
    public void setLifetime(int age) {
        particle.setLifetime(Math.max(particle instanceof WakeParticle ? Math.min(age, 60) : age, 0));
    }

    @LuaWhitelist
    @LuaMethodShadow("setLifetime")
    public LuaParticle lifetime(int age) {
        setLifetime(age);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_power")
    public float getPower() {
        return power;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "power"
            ),
            value = "particle.set_power")
    public void setPower(float power) {
        particle.setPower(power);
        this.power = power;
    }

    @LuaWhitelist
    @LuaMethodShadow("setPower")
    public LuaParticle power(float power) {
        setPower(power);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_scale")
    public float getScale() {
        return scale;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "scale"
            ),
            value = "particle.set_scale")
    public void setScale(float scale) {
        particle.scale(scale);
        this.scale = scale;
    }

    @LuaWhitelist
    @LuaMethodShadow("setScale")
    public LuaParticle scale(float scale) {
        setScale(scale);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.get_gravity")
    public float getGravity() {
        return ((ParticleAccessor) particle).getGravity();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Float.class,
                    argumentNames = "gravity"
            ),
            value = "particle.set_gravity")
    public void setGravity(float gravity) {
        ((ParticleAccessor) particle).setGravity(gravity);
    }

    @LuaWhitelist
    @LuaMethodShadow("setGravity")
    public LuaParticle gravity(float gravity) {
        setGravity(gravity);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("particle.has_physics")
    public boolean hasPhysics() {
        return ((ParticleAccessor) particle).getHasPhysics();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "physics"
            ),
            value = "particle.set_physics")
    public void setPhysics(boolean physics) {
        ((ParticleAccessor) particle).setHasPhysics(physics);
    }

    @LuaWhitelist
    @LuaMethodShadow("setPhysics")
    public LuaParticle physics(boolean physics) {
        setPhysics(physics);
        return this;
    }

    public String toString() {
        return name + " (Particle)";
    }
}
