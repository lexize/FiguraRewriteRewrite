package org.moon.figura.trust;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

public abstract class TrustContainer {

    //fields :p
    public String name; //uuid
    private boolean visible = true; //used on UI

    //trust -> value map
    public final Map<Trust, Integer> trustSettings = new HashMap<>();
    public final Map<String, Map<Trust, Integer>> customTrusts = new HashMap<>();

    // constructors //

    public TrustContainer(String name) {
        this.name = name;
    }

    // functions //

    public abstract MutableComponent getGroupName();
    public abstract int getColor();
    public abstract Trust.Group getGroup();
    public abstract void setParent(GroupContainer newParent);

    //read nbt
    public void loadNbt(CompoundTag nbt) {
        //default trust
        for (Trust setting : Trust.DEFAULT) {
            if (nbt.contains(setting.name))
                trustSettings.put(setting, nbt.getInt(setting.name));
        }

        //custom trust
        if (!nbt.contains("custom"))
            return;

        CompoundTag custom = nbt.getCompound("custom");
        for (FiguraTrust figuraTrust : TrustManager.CUSTOM_TRUST) {
            String key = figuraTrust.getTitle();

            Map<Trust, Integer> map = new HashMap<>();
            CompoundTag customNbt = custom.getCompound(key);

            for (Trust trust : figuraTrust.getTrusts()) {
                if (customNbt.contains(trust.name))
                    map.put(trust, nbt.getInt(trust.name));
            }

            customTrusts.put(key, map);
        }
    }

    //write nbt
    public void writeNbt(CompoundTag nbt) {
        //name
        nbt.putString("name", this.name);

        //trust values
        CompoundTag trust = new CompoundTag();
        for (Map.Entry<Trust, Integer> entry : this.trustSettings.entrySet())
            trust.putInt(entry.getKey().name, entry.getValue());

        //custom trust
        CompoundTag custom = new CompoundTag();
        for (Map.Entry<String, Map<Trust, Integer>> entry : this.customTrusts.entrySet()) {
            CompoundTag customNbt = new CompoundTag();

            for (Map.Entry<Trust, Integer> entry2 : entry.getValue().entrySet())
                trust.putInt(entry2.getKey().name, entry2.getValue());

            custom.put(entry.getKey(), customNbt);
        }

        trust.put("custom", custom);

        //add to nbt
        nbt.put("trust", trust);
    }

    //get value from trust
    public int get(Trust trust) {
        //get setting
        Integer setting = this.trustSettings.get(trust);
        if (setting != null)
            return setting;

        for (Map<Trust, Integer> value : this.customTrusts.values()) {
            setting = value.get(trust);
            if (setting != null)
                return setting;
        }

        //if no trust found, return -1
        return -1;
    }

    public boolean hasChanges() {
        boolean bool = !trustSettings.isEmpty();

        if (!bool) {
            for (Map<Trust, Integer> value : customTrusts.values()) {
                if (!value.isEmpty())
                    return true;
            }
        }

        return bool;
    }

    public boolean isChanged(Trust trust) {
        if (trustSettings.containsKey(trust))
            return true;

        for (Map<Trust, Integer> map : customTrusts.values()) {
            if (map.containsKey(trust))
                return true;
        }

        return false;
    }

    public void reset(Trust trust) {
        trustSettings.remove(trust);
        for (Map<Trust, Integer> map : customTrusts.values())
            map.remove(trust);
    }

    //clear trust settings
    public void clear() {
        trustSettings.clear();
        customTrusts.clear();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // -- types -- //

    public static class GroupContainer extends TrustContainer {

        public final Trust.Group group;

        public GroupContainer(Trust.Group group) {
            super(group.name());
            this.group = group;
        }

        @Override
        public MutableComponent getGroupName() {
            return group.text.copy();
        }

        @Override
        public int getColor() {
            return group.color;
        }

        @Override
        public Trust.Group getGroup() {
            return group;
        }

        @Override
        public void setParent(GroupContainer newParent) {
            //do nothing
        }

        @Override
        public int get(Trust trust) {
            int result = super.get(trust);
            return result != -1 ? result : trust.getDefault(getGroup());
        }
    }

    public static class PlayerContainer extends TrustContainer {

        public GroupContainer parent;

        public PlayerContainer(GroupContainer parent, String name) {
            super(name);
            this.parent = parent;
        }

        @Override
        public MutableComponent getGroupName() {
            return parent.getGroupName();
        }

        @Override
        public int getColor() {
            return parent.getColor();
        }

        @Override
        public Trust.Group getGroup() {
            return parent.getGroup();
        }

        @Override
        public void setParent(GroupContainer newParent) {
            this.parent = newParent;
        }

        @Override
        public void writeNbt(CompoundTag nbt) {
            if (this.getGroup() != Trust.Group.BLOCKED) {
                super.writeNbt(nbt);
            } else {
                nbt.putString("name", this.name);
            }

            //parent
            nbt.putString("parent", parent.name);
        }

        @Override
        public int get(Trust trust) {
            int result = super.get(trust);
            return result != -1 ? result : parent.get(trust);
        }

        @Override
        public boolean isVisible() {
            return parent.isVisible();
        }
    }
}
