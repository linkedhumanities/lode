package models.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;


public class PropertyContainer implements Comparable<PropertyContainer>, Serializable
{
    public PropertyModel               property;
    public Map<String, TreeSet<Value>> values;               // key = language
    public List<String>                order;                // workaround for java<->scala

    private static final long          serialVersionUID = 1L;


    public PropertyContainer(PropertyModel property)
    {
        this.property = property;
        this.values = new LinkedHashMap<>();
        this.order = new ArrayList<String>();
    }


    public void addValue(String language, Value value)
    {
        if(!this.values.containsKey(language)) {
            this.values.put(language, new TreeSet<Value>());
            this.order.add(language);
        }

        this.values.get(language).add(value);
    }


    public void setPropertyName(String name)
    {
        this.property.setName(name);
    }


    public void setValues(Map<String, TreeSet<Value>> values)
    {
        this.values = values;
        this.order = new LinkedList<>(this.values.keySet());
    }


    public Map<String, TreeSet<Value>> getValues()
    {
        return this.values;
    }


    public NavigableSet<Value> getValuesByLanguage(String lang)
    {
        if(values.containsKey(lang)) { return values.get(lang); }

        return new TreeSet<>();
    }


    public List<String> getLanguages()
    {
        return order;
    }


    public int getSize()
    {
        return values.size();
    }


    public String getPropertyURI()
    {
        return property.getURI();
    }


    public String getPropertyName()
    {
        return property.getName();
    }


    public int getPropertyWeight()
    {
        return property.getWeight();
    }


    @Override
    public int compareTo(PropertyContainer p)
    {
        return this.property.compareTo(p.property);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof PropertyContainer) {
            PropertyContainer pc = (PropertyContainer) obj;
            System.out.println("JA");
            if(this.getPropertyURI().equals(pc.getPropertyURI())) { return true; }
        }

        return false;
    }


    @Override
    public String toString()
    {
        String s = property.getURI();
        s += " [";
        for(String temp : values.keySet()) {
            s += " " + values.get(temp) + "(" + temp + ")";
        }
        s += "]";
        return s;
    }
}