package dev.nova.gameapi.game.map.options.instance;

import dev.nova.gameapi.game.map.options.OptionType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class GameOptionInfo {

    private final OptionSpecificInfo optionSpecificInfo;
    private final OptionType optionType;
    private final boolean required;
    private final String displayName;
    private final String originalKey;
    private String key;

    public boolean validateFormatting = true;
    private GameOptionInfo parent;

    public GameOptionInfo getParent() {
        return parent;
    }

    public boolean isValidateFormatting() {
        return validateFormatting;
    }

    public GameOptionInfo(String key, String displayName, boolean required, OptionType type) {
        this(key, displayName, required, type, null);
    }

    public GameOptionInfo(String key, String displayName, boolean required, OptionType optionType, OptionSpecificInfo optionSpecificInfo) {

        if (optionSpecificInfo == null && optionType.infoClass != null) {
            throw new IllegalArgumentException("This option type needs OptionSpecificInfo to be provided with it");
        }

        if (optionSpecificInfo != null) {
            if (!optionSpecificInfo.getOptionType().equals(optionType)) {
                throw new IllegalArgumentException("Option Specific Info type does not match the option type provided!");
            }
        }
        this.key = key;
        this.originalKey = key;
        this.displayName = displayName;
        this.required = required;
        this.optionType = optionType;
        this.optionSpecificInfo = optionSpecificInfo;

    }

    public String getOriginalKey() {
        return originalKey;
    }

    public String getRealDisplayName() {
        return displayName;
    }

    public String getKey() {
        return key+(validateFormatting ? ".value" : "");
    }

    public String getRealKey(){
        return key;
    }

    public boolean isRequired() {
        return required;
    }

    private String getDisplayName(int depth, ConfigurationSection section, boolean lookOnOriginal) {
        return optionType.equals(OptionType.ARRAY) ? makeArrayDisplayName() : optionType.equals(OptionType.CONFIGURATION_SECTION) ? makeConfigDisplayName(depth, section, lookOnOriginal) : displayName + " (Key: " + key + ")" + (section != null ? " (" + isValid(section, lookOnOriginal) + ")" : "");
    }

    public String getDisplayName(int depth, boolean lookOnOriginal) {
        return getDisplayName(depth, null, lookOnOriginal);
    }

    private String makeConfigDisplayName(int depth, ConfigurationSection sectionA, boolean lookOnOriginal) {
        OptionSpecificInfo.ConfigurationSection section = (OptionSpecificInfo.ConfigurationSection) optionSpecificInfo;
        StringBuilder builder = new StringBuilder();
        builder.append(displayName).append("\n\t").append("     ".repeat(Math.max(0, depth))).append("Needed values: ");
        for (GameOptionInfo optionInfo : section.optionInfos) {
            builder.append("\n\t").append("     ".repeat(Math.max(0, depth))).append("  -  ").append(optionInfo.getDisplayName(depth + 1, sectionA, lookOnOriginal));
        }

        return builder.toString();
    }

    private String makeArrayDisplayName() {
        OptionSpecificInfo.Array array = (OptionSpecificInfo.Array) optionSpecificInfo;
        return displayName + " (Key: " + key + ")" + " (Type: " + array.type.displayName + ", Length: " + (array.length == 0 ? "Unlimited" : array.length) + ")";
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public OptionSpecificInfo getOptionSpecificInfo() {
        return optionSpecificInfo;
    }

    public String getMissingDisplayName(int depth, ConfigurationSection section, boolean lookOnOriginal) {
        return getDisplayName(depth, section, lookOnOriginal);
    }

    public enum ValidationState {
        VALID,
        PARTIALLY_VALID,
        INVALID,
        INVALID_FROM_CONFIG
    }

    public ValidationState isValid(ConfigurationSection section, boolean lookOnOriginal) {

        if (validateFormatting) {
            if (!section.contains(key + ".type") || !section.contains(key + ".value")) {
                return ValidationState.INVALID;
            }
        }

        if (optionSpecificInfo != null) {
            if (validateFormatting) {
                return optionSpecificInfo.isValid(section.getConfigurationSection(key + ".value"),lookOnOriginal);
            }

            return section == null ? ValidationState.INVALID : optionSpecificInfo.isValid(section,true);
        }

        return validateFormatting ? section.contains((lookOnOriginal ? originalKey : key) + ".value") ? ValidationState.VALID : ValidationState.INVALID : section.contains((lookOnOriginal ? originalKey : key)) ? ValidationState.VALID : ValidationState.INVALID;
    }

    public static abstract class OptionSpecificInfo {

        private final OptionType optionType;

        public OptionType getOptionType() {
            return optionType;
        }

        public abstract ValidationState isValid(org.bukkit.configuration.ConfigurationSection section,boolean lookOnOriginal);

        private OptionSpecificInfo(OptionType type) {
            this.optionType = type;
        }

        public static class Array extends OptionSpecificInfo {

            private final int length;
            private final OptionType type;

            /**
             * @param type   Type of the array
             * @param length The needed length (0 for unlimited)
             */
            public Array(OptionType type, int length) {
                super(OptionType.ARRAY);
                this.type = type;
                if(type.equals(OptionType.ARRAY)){
                    throw new IllegalArgumentException("Multi dimensional arrays are not supported yet.");
                }
                this.length = length;
            }

            public OptionType getType() {
                return type;
            }

            public int getLength() {
                return length;
            }

            @Override
            public ValidationState isValid(org.bukkit.configuration.ConfigurationSection section,boolean lookOnOriginal) {

                int values = section.getKeys(false).size();

                if (length == 0 && values != 0) {
                    return ValidationState.VALID;
                }

                if (length != 0 && values != length) {
                    return ValidationState.PARTIALLY_VALID;
                }

                if (length != 0) {
                    return ValidationState.VALID;
                }

                return ValidationState.INVALID;
            }
        }

        public static class ConfigurationSection extends OptionSpecificInfo {

            private final GameOptionInfo[] optionInfos;
            private final String path;
            private boolean made = false;

            /**
             * @param optionInfos What the section should contain.
             */
            public ConfigurationSection(String path,GameOptionInfo[] optionInfos) {
                super(OptionType.CONFIGURATION_SECTION);
                this.path = path;
                this.optionInfos = optionInfos;

            }

            public void make(GameOptionInfo parent) {
                if (made) return;
                made = true;
                for (int i = optionInfos.length - 1; i >= 0; i--) {
                    GameOptionInfo info = optionInfos[i];

                    info.parent = parent;

                    info.key = inheritKey(parent) + info.key;
                    info.validateFormatting = false;
                    if (info.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.ConfigurationSection configurationSection) {
                        configurationSection.make(info);
                    }

                }

            }

            private String inheritKey(GameOptionInfo parent) {
                StringBuilder path = new StringBuilder();

                ArrayList<GameOptionInfo> parents = new ArrayList<>();
                GameOptionInfo lastParent = parent;
                while (lastParent.parent != null) {
                    parents.add(lastParent.parent);
                    lastParent = lastParent.parent;
                }

                parents.add(parent);

                for (int i = 0; i < parents.size(); i++) {
                    path.append(parents.get(i).originalKey).append(i == 0 ? ".value." : ".");
                }

                return path.toString();
            }

            public GameOptionInfo[] getOptionInfos() {
                return optionInfos;
            }

            @Override
            public ValidationState isValid(org.bukkit.configuration.ConfigurationSection section,boolean lookOnOriginal) {

                boolean invalidBefore = false;
                boolean validBefore = false;
                boolean PvalidBefore = false;
                boolean invalidFromConfig = false;

                for (GameOptionInfo optionInfo : optionInfos) {
                    org.bukkit.configuration.ConfigurationSection sectionK = section.getConfigurationSection(optionInfo.originalKey);

                    ValidationState valid;

                    if(sectionK != null){
                        valid = optionInfo.isValid(sectionK,true);
                    }else{
                        valid = optionInfo.isValid(section, true);
                    }

                    System.out.println(optionInfo.key+": "+valid);
                    System.out.println("looking in: "+section.getCurrentPath());


                    if(valid.equals(ValidationState.INVALID_FROM_CONFIG)){
                        invalidFromConfig = true;
                    }

                    if(valid.equals(ValidationState.INVALID)){
                        invalidBefore = true;
                    }

                    if(valid.equals(ValidationState.VALID)){

                        validBefore = true;

                        if(invalidBefore){
                            PvalidBefore = true;
                        }
                    }

                    if(valid.equals(ValidationState.PARTIALLY_VALID)) {
                        PvalidBefore = true;
                    }
                }

                System.out.println();
                System.out.println();

                return invalidFromConfig ? ValidationState.INVALID_FROM_CONFIG : PvalidBefore ? ValidationState.PARTIALLY_VALID : invalidBefore ? ValidationState.INVALID : ValidationState.VALID;
            }

            public ArrayList<GameOptionInfo> getAllOptionInfos() {
                ArrayList<GameOptionInfo> infos = new ArrayList<>(Arrays.asList(optionInfos));

                for(GameOptionInfo info : optionInfos) {
                    if(info.getOptionSpecificInfo() instanceof ConfigurationSection section){
                        infos.addAll(section.getAllOptionInfos());
                    }
                }

                return infos;
            }
        }
    }

}
