package me.darknet.assembler.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {

    public static <V, K> Map<V, K> invert(Map<K, V> map) {

        Map<V, K> inv = new HashMap<V, K>();

        for (Map.Entry<K, V> entry : map.entrySet())
            inv.put(entry.getValue(), entry.getKey());

        return inv;
    }

}
