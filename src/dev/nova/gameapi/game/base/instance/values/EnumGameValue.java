package dev.nova.gameapi.game.base.instance.values;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.values.base.GameValue;

public class EnumGameValue<E extends Enum<E>> extends GameValue<E> {

    int inOrdinal = 0;

    public EnumGameValue(GameInstance instance, String id, String displayName, E defaultValue) {
        super(instance, id, displayName, defaultValue);
    }

    @Override
    public boolean autoSetValue() {
        int length = getValues().length;

        if(getValues()[length+1] != null){
            inOrdinal++;
        }else{
            inOrdinal = 0;
        }

        setValue(getValues()[inOrdinal]);
        return true;
    }

    public E[] getValues(){
            return getDefaultValue().getDeclaringClass().getEnumConstants();
        }
}
