package dev.nova.gameapi.utils.api.general;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * A utility class that has the function to sort a map based on its values unlike TreeMap on its keys
 *
 */
public class MapSorterValue {

    public static <K, V extends Comparable<V>> Map<K, V> sort(Map<K, V> values) {
        List<Map.Entry<K, V>> entriesA = new ArrayList<>(values.entrySet());
        entriesA.sort((Comparator<Map.Entry<K, V>> & Serializable)
                (B, K) -> -B.getValue().compareTo(K.getValue()));

        Map<K, V> r = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : entriesA) {
            r.put(e.getKey(), e.getValue());
        }

        return r;
    }

}
