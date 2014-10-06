package models.entity;

import java.io.Serializable;

import org.jsoup.Jsoup;

import utils.ResUtils;
import utils.StringUtils;


public class Value implements Comparable<Value>, Serializable
{
    private String            rawValue;
    private String            description;
    private float             weight;

    private static final long serialVersionUID = 1L;


    public Value(String value)
    {
        // this.rawValue = StringUtils.decode(value.trim());
        this.rawValue = value.trim();

        if(!this.rawValue.startsWith("http://") && this.rawValue.contains(":") && ResUtils.toLongURI(this.rawValue) != null) {
            this.rawValue = ResUtils.toLongURI(this.rawValue);
        }

        if(this.isLiteral()) {
            this.rawValue = StringUtils.decode(this.rawValue);
            this.rawValue = rawValue.replace("'", "\u2019");
        }
    }


    public Value(String value, String description, float weight)
    {
        // this.rawValue = StringUtils.decode(value.trim());
        this.rawValue = value.trim();

        if(!this.rawValue.startsWith("http://") && this.rawValue.contains(":") && ResUtils.toLongURI(this.rawValue) != null) {
            this.rawValue = ResUtils.toLongURI(this.rawValue);
        }

        if(this.isLiteral()) {
            this.rawValue = StringUtils.decode(this.rawValue);
            this.rawValue = rawValue.replace("'", "\u2019");
        }

        this.weight = weight;
        this.description = description;
    }


    public String getValue()
    {
        if(this.hasLanguage()) { return this.rawValue.substring(0, this.rawValue.lastIndexOf("@")); }
        if(this.hasDataType()) { return this.rawValue.substring(0, this.rawValue.lastIndexOf("^^http")); }

        return rawValue;
    }


    public String getLanguage()
    {
        if(this.hasLanguage()) { return this.rawValue.substring(this.rawValue.lastIndexOf("@") + 1); }

        return "";
    }


    public String getDataType()
    {
        if(this.hasDataType()) { return this.rawValue.substring(this.rawValue.lastIndexOf("^^http") + 2); }

        return "";
    }


    public String getShortDataType()
    {
        if(this.hasDataType()) { return this.rawValue.substring(this.rawValue.lastIndexOf("#") + 1); }

        return "";
    }


    public String getPropertyTyp()
    {
        if(this.isLiteral()) { return "dataproperty"; }

        return "objectproperty";
    }


    public String getDescription()
    {
        return Jsoup.parse(description).text();
    }


    public void setDescription(String description)
    {
        this.description = description;
    }


    public float getWeight()
    {
        return weight;
    }


    public boolean hasLanguage()
    {
        if(this.rawValue.contains("@") && this.rawValue.substring(this.rawValue.lastIndexOf("@")).length() == 3) { return true; }

        return false;
    }


    public boolean hasDataType()
    {
        if(this.rawValue.contains("^^http")) { return true; }

        return false;
    }


    public boolean isLiteral()
    {
        return !ResUtils.isURI(this.getValue());
    }


    public boolean isResource()
    {
        return ResUtils.isURI(this.getValue());
    }


    public String toSparql()
    {
        StringBuilder sb = new StringBuilder();

        if(this.rawValue.length() == 0) { return "?data"; }

        String value = this.getValue();

        if(this.isLiteral()) {
            sb.append("'" + value + "'");

            if(this.hasLanguage()) {
                sb.append("@" + this.getLanguage());
            }
            if(this.hasDataType()) {
                sb.append("^^<" + this.getDataType() + ">");
            }
        }

        if(this.isResource()) {
            // try {
            // String localname = ResUtils.getLocaleName(value);
            // String localNameEn = URLEncoder.encode(localname, "UTF-8");
            // value = value.replace(localname, localNameEn);
            // } catch(UnsupportedEncodingException e) {
            // // should be no problem
            // }

            sb.append("<" + value.replace(">", "").replace("<", "") + ">");
        }

        return sb.toString();
    }


    public String toShortURI()
    {
        return ResUtils.createShortURI(this.getValue());
    }


    @Override
    public String toString()
    {
        return this.toSparql();
    }


    @Override
    public int compareTo(Value v)
    {
        return this.getValue().compareTo(v.getValue());
    }


    @Override
    public int hashCode()
    {
        return this.toString().toLowerCase().hashCode();
    }


    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Value) {
            Value v = (Value) obj;
            if(this.toString().toLowerCase().equals(v.toString().toLowerCase())) { return true; }
        }

        return false;
    }
}