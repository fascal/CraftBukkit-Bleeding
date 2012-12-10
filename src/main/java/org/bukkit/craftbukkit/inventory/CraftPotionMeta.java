package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.craftbukkit.inventory.CraftItemMeta.SerializableMeta;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;

@DelegateDeserialization(SerializableMeta.class)
class CraftPotionMeta extends CraftItemMeta implements PotionMeta {
    static final ItemMetaKey AMPLIFIER = new ItemMetaKey("Amplifier", "amplifier");
    static final ItemMetaKey AMBIENT = new ItemMetaKey("Ambient", "ambient");
    static final ItemMetaKey DURATION = new ItemMetaKey("Duration", "duration");
    static final ItemMetaKey POTION_EFFECTS = new ItemMetaKey("CustomPotionEffects", "custom-effects");
    static final ItemMetaKey ID = new ItemMetaKey("ID", "potion-id");
    private List<PotionEffect> customEffects;

    CraftPotionMeta(CraftItemMeta meta) {
        super(meta);
        if (!(meta instanceof CraftPotionMeta)) {
            return;
        }
        CraftPotionMeta potionMeta = (CraftPotionMeta) meta;
        if (potionMeta.hasCustomEffects()) {
            this.customEffects = new ArrayList<PotionEffect>(potionMeta.customEffects);
        }
    }

    CraftPotionMeta(NBTTagCompound tag) {
        super(tag);

        if (tag.hasKey(POTION_EFFECTS.NBT)) {
            NBTTagList list = tag.getList(POTION_EFFECTS.NBT);
            int length = list.size();
            if (length > 0) {
                customEffects = new ArrayList<PotionEffect>(length);

                for (int i = 0; i < length; i++) {
                    NBTTagCompound effect = (NBTTagCompound) list.get(i);
                    PotionEffectType type = PotionEffectType.getById(effect.getByte(ID.NBT));
                    int amp = effect.getByte(AMPLIFIER.NBT);
                    int duration = effect.getInt(DURATION.NBT);
                    boolean ambient = effect.getBoolean(AMBIENT.NBT);
                    customEffects.add(new PotionEffect(type, amp, duration, ambient));
                }
            }
        }
    }

    CraftPotionMeta(Map<String, Object> map) {
        super(map);

        List rawEffectList = SerializableMeta.getObject(List.class, map, POTION_EFFECTS.BUKKIT, true);
        if (rawEffectList == null) {
            return;
        }

        for (Object obj : rawEffectList) {
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException("Object in effect list is not valid. " + obj.getClass());
            }
            Map fieldMap = (Map) obj;
            final PotionEffectType type = PotionEffectType.getById(SerializableMeta.getObject(Integer.class, fieldMap, ID.BUKKIT, false));
            final int amp = SerializableMeta.getObject(Byte.class, fieldMap, AMPLIFIER.BUKKIT, false);
            final int duration = SerializableMeta.getObject(Integer.class, fieldMap, DURATION.BUKKIT, false);
            final boolean ambient = SerializableMeta.getObject(Boolean.class, fieldMap, AMBIENT.BUKKIT, false);
            PotionEffect effect = new PotionEffect(type, amp, duration, ambient);
            addCustomEffect(effect, true);
        }
    }

    @Override
    void applyToItem(NBTTagCompound tag) {
        super.applyToItem(tag);
        if (hasCustomEffects()) {
            NBTTagList effectList = new NBTTagList();
            tag.set(POTION_EFFECTS.NBT, effectList);

            for (PotionEffect effect : customEffects) {
                NBTTagCompound effectData = new NBTTagCompound();
                effectData.setByte(ID.NBT, (byte) effect.getType().getId());
                effectData.setByte(AMPLIFIER.NBT, (byte) effect.getAmplifier());
                effectData.setInt(DURATION.NBT, effect.getDuration());
                effectData.setBoolean(AMBIENT.NBT, effect.isAmbient());
                effectList.add(effectData);
            }
        }
    }

    @Override
    boolean isEmpty() {
        return super.isEmpty() && !hasCustomEffects();
    }

    @Override
    boolean applicableTo(Material type) {
        switch(type) {
            case POTION:
                return true;
            default:
                return false;
        }
    }

    @Override
    public CraftPotionMeta clone() {
        CraftPotionMeta clone = (CraftPotionMeta) super.clone();
        if (hasCustomEffects()) {
            clone.customEffects = new ArrayList<PotionEffect>(customEffects);
        }
        return clone;
    }

    public boolean hasCustomEffects() {
        return !(customEffects == null || customEffects.isEmpty());
    }

    public List<PotionEffect> getCustomEffects() {
        if (hasCustomEffects()) {
            return ImmutableList.copyOf(customEffects);
        }
        return ImmutableList.of();
    }

    public boolean addCustomEffect(PotionEffect effect, boolean overwrite) {
        Validate.notNull(effect, "Potion effect must not be null");

        int index = indexOfEffect(effect.getType());
        if (index != -1) {
            if (overwrite) {
                PotionEffect old = customEffects.get(index);
                if (old.getAmplifier() == effect.getAmplifier() && old.getDuration() == effect.getDuration() && old.isAmbient() == effect.isAmbient()) {
                    return false;
                }
                customEffects.set(index, effect);
                return true;
            } else {
                return false;
            }
        } else {
            if (customEffects == null) {
                customEffects = new ArrayList<PotionEffect>();
            }
            customEffects.add(effect);
            return true;
        }
    }

    public boolean removeCustomEffect(PotionEffectType type) {
        Validate.notNull(type, "Potion effect type must not be null");

        if (!hasCustomEffects()) {
            return false;
        }

        boolean changed = false;
        Iterator<PotionEffect> iterator = customEffects.iterator();
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            if (effect.getType() == type) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public boolean hasCustomEffect(PotionEffectType type) {
        Validate.notNull(type, "Potion effect type must not be null");
        return indexOfEffect(type) != -1;
    }

    public boolean setMainEffect(PotionEffectType type) {
        Validate.notNull(type, "Potion effect type must not be null");
        int index = indexOfEffect(type);
        if (index == -1 || index == 0) {
            return false;
        }

        PotionEffect old = customEffects.get(0);
        customEffects.set(0, customEffects.get(index));
        customEffects.set(index, old);
        return true;
    }

    private int indexOfEffect(PotionEffectType type) {
        if (!hasCustomEffects()) {
            return -1;
        }

        for (int i = 0; i < customEffects.size(); i++) {
            if (customEffects.get(i).getType().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    public boolean clearCustomEffects() {
        boolean changed = hasCustomEffects();
        customEffects = null;
        return changed;
    }

    @Override
    int applyHash() {
        final int original;
        int hash = original = super.applyHash();
        if (hasCustomEffects()) {
            hash = 73 * hash + customEffects.hashCode();
        }
        return original != hash ? CraftPotionMeta.class.hashCode() ^ hash : hash;
    }

    @Override
    public boolean equalsCommon(CraftItemMeta meta) {
        if (!super.equalsCommon(meta)) {
            return false;
        }
        if (meta instanceof CraftPotionMeta) {
            CraftPotionMeta that = (CraftPotionMeta) meta;

            return (this.hasCustomEffects() ? that.hasCustomEffects() && this.customEffects.equals(that.customEffects) : !that.hasCustomEffects());
        }
        return true;
    }

    @Override
    boolean notUncommon(CraftItemMeta meta) {
        return super.notUncommon(meta) && (meta instanceof CraftPotionMeta || !hasCustomEffects());
    }

    @Override
    Builder<String, Object> serialize(Builder<String, Object> builder) {
        super.serialize(builder);

        if (hasCustomEffects()) {
            final List<Map<String, Object>> effectsMap = new ArrayList<Map<String, Object>>(customEffects.size());
            for (int i = 0; i < customEffects.size(); i++) {
                PotionEffect effect = customEffects.get(i);
                Map<String, Object> fieldMap = new HashMap<String, Object>(4);
                fieldMap.put(AMPLIFIER.BUKKIT, effect.getAmplifier());
                fieldMap.put(DURATION.BUKKIT, effect.getDuration());
                fieldMap.put(AMBIENT.BUKKIT, effect.isAmbient());
                fieldMap.put(ID.BUKKIT, effect.getType().getId());
                effectsMap.add(fieldMap);
            }
            builder.put(POTION_EFFECTS.BUKKIT, effectsMap);
        }

        return builder;
    }

    @Override
    SerializableMeta.Deserializers deserializer() {
        return SerializableMeta.Deserializers.POTION;
    }
}