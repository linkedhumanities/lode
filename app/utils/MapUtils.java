package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.atlas.lib.Pair;


public class MapUtils
{
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, final boolean desc)
    {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                if(desc) { return (o1.getValue()).compareTo(o2.getValue()) * -1; }

                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for(Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


    public static <K, V> List<Pair<K, V>> toList(Map<K, V> map)
    {
        List<Pair<K, V>> result = new LinkedList<>();

        for(K k : map.keySet()) {
            Pair<K, V> p = new Pair<K, V>(k, map.get(k));
            result.add(0, p);
        }

        return result;
    }


    @SuppressWarnings ("unchecked")
    public static <K> List<K> deepCopy(List<K> o)
    {
        List<K> deepCopy = new LinkedList<>();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(o);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            for(Object ob : (List<?>) ois.readObject()) {
                deepCopy.add((K) ob);
            }
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return deepCopy;
    }
}