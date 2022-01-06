package dev.nova.gameapi.utils.api.general;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

public class Pages<Type> implements Iterable<List<Type>> {

    private final List<Type> rawObjectsList;
    private final List<Type> sortedDone = new ArrayList<>();
    private final int split;
    private final HashMap<Integer, List<Type>> sorted;
    private int globalIndex = 0;


    /**
     *
     * Creates a hashmap of sorted things, usually takes about 1 ms to create.
     *
     * @param objects List of things to sort
     */
    public Pages(List<Type> objects, int split){

        this.rawObjectsList = objects;
        this.sorted = new HashMap<>();
        this.split = split;
        if(rawObjectsList.size() == 0){
            throw new IllegalStateException("Size of the array cannot be 0");
        }

        loop();
    }

    private void loop() {
        List<Type> current = new ArrayList<>();

        int index = 1;
        for(Type value : rawObjectsList) {

            if(!sortedDone.contains(value)) {

                sortedDone.add(value);

                current.add(value);

                if(index == split){
                    sorted.put(globalIndex,current);
                    globalIndex++;

                    if(sortedDone.size() != rawObjectsList.size()) {
                        loop();
                    }
                    return;
                }

                sorted.put(globalIndex,current);

                index++;
            }

        }

    }

    public HashMap<Integer, List<Type>> getSorted() {
        return sorted;
    }

    public List<Type> getRawObjectsList() {
        return rawObjectsList;
    }

    public int getSplit() {
        return split;
    }

    @Override
    public Iterator<List<Type>> iterator() {
        return getSorted().values().iterator();
    }
}
