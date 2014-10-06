package utils;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;

import settings.Settings;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;


public class ViewUtils
{
    public static String buildEnhancerBox(String id, String position, EntityContainer ec)
    {
        List<PropertyContainer> properties = ec.getProperties();
        String htmlID = id + "-" + position;

        // build head of box
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"en-box\">");
        sb.append("<tr>");
        sb.append("<td class=\"en-content\">");
        sb.append("<div id=\"" + htmlID + "-content\" class=\"scroll-pane\"><div style=\"padding: 13px;\">");    // area of the content

        int cou = 0;
        int couValue = 0;

        // build content
        for(PropertyContainer property : properties) {
            if(property.getPropertyURI().toLowerCase().contains("owl#sameas")) {
                continue;
            }

            StringBuilder tmp = new StringBuilder();
            int couValueTmp = 0;

            // build head of entry
            tmp.append("<dl id=\"" + htmlID + "-" + ResUtils.createShortURI(property.getPropertyURI()).replace(":", "-") + "\"");
            if((cou + 1) > Settings.E_PREVIEW) {
                tmp.append(" style=\"display:none;\"");
            }
            tmp.append(">");

            // add headline of entry (property)
            tmp.append("<dt>");
            if(!id.equals("concept")) {
                tmp.append("<span id=\"" + htmlID + "-" + ResUtils.createShortURI(property.getPropertyURI()).replace(":", "-") + "-property\" class=\"drag drop\" data-type=\"property\" data-uri=\"" + ResUtils.createShortURI(property.getPropertyURI()) + "\" data-uri-long=\"" + property.getPropertyURI() + "\">");
                tmp.append("<a data-toggle=\"tooltip\" data-placement=\"right\" data-container=\"body\" title=\"" + toHtmlLink(property.getPropertyURI()) + "\" style=\"color: #424242; cursor: pointer; text-decoration: none;\">" + property.getPropertyName() + "</a>");
                tmp.append("</span>");
            } else {
                tmp.append("<span data-uri=\"" + ResUtils.createShortURI(property.getPropertyURI()) + "\"></span>");
            }
            tmp.append("</dt>");

            // add new value
            if(position.equals("local") && !id.equals("concept")) {
                tmp.append("<dd><div id=\"" + htmlID + "-" + ResUtils.createShortURI(property.getPropertyURI()).replace(":", "-") + "-addValue\" class=\"en-box drop\" style=\"display:none;\" data-category=\"" + id + "\" data-field=\"value\">\u2295 add new value</div></dd>");
            }

            // add content of entry (value)
            tmp.append("<dd>");
            for(String lang : property.getLanguages()) {
                for(Value data : property.getValuesByLanguage(lang)) {
                    if((data.isResource() && id.equals("dataproperties")) || ((!data.isResource() || property.getPropertyURI().endsWith("type")) && id.equals("objectproperties")) || (!property.getPropertyURI().endsWith("type") && id.equals("concept"))) {
                        continue;
                    }

                    // special case for concepts
                    if(id.equals("concept")) {
                        tmp.append("<dl id=\"" + htmlID + "-" + ResUtils.createShortURI(property.getPropertyURI()).replace(":", "-") + "-" + data.hashCode() + "-value\" style=\"margin-bottom: 0px; margin-top: 0px;");
                        if((cou + 1) > Settings.E_PREVIEW) {
                            tmp.append(" display:none;");
                        }
                        tmp.append("\"><dt><span data-uri=\"" + ResUtils.createShortURI(property.getPropertyURI()) + "\"/></dt><dd>");
                        cou++;
                    }

                    // add values
                    if(id.equals("concept")) {
                        tmp.append("<div id=\"hash-" + position + "-" + data.hashCode() + "\" class=\"drag drop\" data-type=\"value\" data-uri=\"" + ResUtils.createShortURI(data.getValue()) + "\" data-uri-long=\"" + data.getValue() + "\" data-category=\"" + id + "\">");
                    } else {
                        tmp.append("<div id=\"hash-" + position + "-" + data.hashCode() + "\" class=\"drag\" data-type=\"value\" data-category=\"" + id + "\">");
                    }
                    if(data.isResource()) {
                        tmp.append("<a href=\"" + data.getValue() + "\" data-toggle=\"tooltip\" data-placement=\"right\" data-container=\"body\" title=\"" + toHtmlLink(data.getValue()) + "\">" + ResUtils.getLocaleName(data.getValue()) + "</a>");
                    } else {
                        tmp.append(ViewUtils.buildEnhancerEntry(property.getPropertyURI(), data, htmlID));
                    }
                    tmp.append("</div>");

                    if(id.equals("concept")) {
                        tmp.append("</dd></dl>");
                    }

                    couValueTmp++;
                }
            }
            tmp.append("</dd>");
            tmp.append("</dl>");

            if(couValueTmp > couValue) {
                couValue = couValueTmp;
            }

            // if this entry has values then add it to the result
            if(tmp.toString().contains("data-type=\"value\"")) {
                sb.append(tmp);
                cou++;
            }
        }

        // add reload button for more properties
        if((cou >= Settings.E_PROPERTIES || couValue >= Settings.E_VALUES) && position.equals("remote")) {
            sb.append("<dl id=\"" + htmlID + "-loadmore\" style=\"display:none;\">");
            sb.append("<dt style=\"text-align:right;\"><button type=\"button\" class=\"btn btn-inverse\" data-loading-text=\"Loading...\" onclick=\"reloadContent('" + ec.getShortURI() + "', '" + id + "', '" + position + "', 'false')\" style=\"width:25%; margin-right: 25px;\">load more</button></dt>");
            sb.append("</dl>");
        }

        // build tail of entry
        sb.append("</div></div>");
        sb.append("</td>");

        // add extend box button
        if(((cou > Settings.E_PREVIEW || couValue >= Settings.E_VALUES) && !id.equals("concept")) || (cou > (Settings.E_PREVIEW + 1) && id.equals("concept"))) {
            int preview = Settings.E_PREVIEW;
            if(id.equals("concept")) {  // necessary because there is one empty "dl"-tag
                preview++;
            }

            sb.append("<td id=\"" + htmlID + "\" onclick=\"expandBox(this.id, '" + preview + "')\" class=\"en-extend\">");
            sb.append("+");
            sb.append("</td>");
        } else {
            sb.append("<td></td>");
        }

        // build tail of box
        sb.append("</tr>");
        sb.append("</table>");

        if(!sb.toString().contains("data-type")) { return new String(""); }

        // return result
        return sb.toString();
    }


    public static String buildEnhancerEntry(String name, Value value, String htmlID)
    {
        // prepare data
        StringBuilder sb = new StringBuilder();
        name = ResUtils.createShortURI(name).replace(":", "-");

        // add value
        sb.append("<span data-lang=\"" + value.getLanguage() + "\" data-format=\"" + value.getDataType() + "\" style=\"cursor: pointer;");
        if(value.getValue().length() > Settings.I_VALUE_LENGTH) {
            sb.append(" display:none;");
        }
        sb.append("\">" + value.getValue() + "</span>");

        // add short value
        if(value.getValue().length() > Settings.I_VALUE_LENGTH) {
            sb.append("<span data-lang=\"" + value.getLanguage() + "\" data-format=\"" + value.getDataType() + "\" style=\"cursor: pointer;\">");
            sb.append(value.getValue().substring(0, Settings.I_VALUE_LENGTH) + " ...");
            sb.append("</span>");
        }

        // add language tag
        if(value.hasLanguage()) {
            sb.append(" (" + value.getLanguage() + ")<br/>");
        }

        // add show more button
        if(value.getValue().length() > Settings.I_VALUE_LENGTH) {
            sb.append("<span class=\"more text-info\" style=\"color: #2E2E2E; font-weight:bold; cursor:pointer\" onclick=\"showMoreText(this, '" + htmlID + "')\">");
            sb.append("show more");
            sb.append("</span>");
        }

        return sb.toString();
    }


    public static String compareDescriptionDialog(EntityContainer ecLocal, EntityContainer ecRemote)
    {
        StringBuilder sb = new StringBuilder();
        String descRemote = ecRemote.getDescription();
        String descLocal = ecLocal.getDescription();

        if(descLocal.length() == 0) {
            descLocal = "Describtion not available";
        }

        if(descRemote.length() == 0) {
            sb.append("No Description Available");
        } else {
            sb.append("<div style=\"text-align: justify\">");
            if(descRemote.length() > Settings.I_DESC_LENGTH) {
                sb.append(descRemote.substring(0, Settings.I_DESC_LENGTH));
                sb.append("...");
            } else {
                sb.append(descRemote);
            }
            sb.append("</div>");
            sb.append("<span style=\"float: right; margin-bottom:1em; margin-top:0.5em;\">");
            sb.append("<button class=\"btn btn-mini\" type=\"button\" onclick=\"showDescription('" + ecLocal.getShortURI() + "', '" + descLocal.replace("'", "\\x27").replace("\"", "\\x22") + "', '" + ecRemote.getShortURI() + "', '" + descRemote.replace("'", "\\x27").replace("\"", "\\x22") + "')\">more</button>");
            sb.append("</span>");
        }

        return sb.toString();
    }


    public static String buildPropertyEntries(String title, PropertyContainer pc)
    {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < pc.getLanguages().size(); i++) {
            String lang = pc.getLanguages().get(i);
            for(int j = 0; j < pc.getValuesByLanguage(lang).size(); j++) {
                Value value = (new LinkedList<Value>(pc.getValuesByLanguage(lang))).get(j);
                String data = value.getValue();

                StringBuilder url = new StringBuilder();
                if(value.isResource()) {
                    url.append("<a href=\"" + value.getValue() + "\" data-toggle=\"tooltip\" data-placement=\"right\" title=\"" + toHtmlLink(value.getValue()) + "\" target=\"blank\">");
                    if(value.getValue().toLowerCase().endsWith(".jpg") || value.getValue().toLowerCase().endsWith(".jpeg") || value.getValue().toLowerCase().endsWith(".png") || value.getValue().toLowerCase().endsWith(".gif")) {
                        url.append("<img src=\"" + value.getValue() + "\" alt=\"title\" style=\"max-width:300px; height:auto;\"/>");
                    } else {
                        if(data.contains("/")) {
                            url.append(data.split("/")[data.split("/").length - 1].replace("_", " "));
                        } else {
                            url.append(data);
                        }
                    }
                    url.append("</a>");

                    data = url.toString();
                } else if(value.hasDataType()) {
                    data = value.getValue() + " (" + value.getShortDataType() + ")";
                }

                if(data.length() > Settings.I_VALUE_LENGTH && url.length() == 0) {
                    sb.append("<span>" + Jsoup.parse(data.substring(0, Settings.I_VALUE_LENGTH)) + "...");
                    if(value.hasLanguage()) {
                        sb.append(" (" + value.getLanguage() + ")");
                    }
                    sb.append("</span>");

                    sb.append("<span style=\"display:none;\">" + Jsoup.parse(value.getValue()) + "");
                    if(value.hasLanguage()) {
                        sb.append(" (" + value.getLanguage() + ")");
                    }
                    sb.append("</span><br/>");

                    sb.append("<span class=\"more text-info\" style=\"font-weight:bold; cursor:pointer\" onclick=\"showMoreText(this)\">show more</span><br/>");
                } else {
                    sb.append(data);
                    if(value.hasLanguage()) {
                        sb.append(" (" + value.getLanguage() + ")");
                    }
                    sb.append("<br/>");
                }
            }
        }

        return sb.toString();
    }


    public static String toHtmlLink(String uri)
    {
        return "<b><a href='" + uri + "' style='color: #FFFFFF; text-decoration:none;' target='_blank'>" + uri + "</a></b>";
    }
}