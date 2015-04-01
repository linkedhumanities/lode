package models.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import models.states.LinkingState;
import settings.Settings;
import sparql.query.Content;
import utils.ResUtils;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class EntityContainer implements Comparable<EntityContainer>
{
	private Value                                   res;
	private NavigableMap<String, PropertyContainer> properties;
	private Set<LinkingState>                       state;
	private String                                  description;


	public EntityContainer(RDFNode res)
	{
		this.properties = new TreeMap<>();
		this.state = new HashSet<>();
		this.description = new String();

		this.res = new Value(res.toString());

		this.state.add(LinkingState.NOT_LINKED);
	}


	public EntityContainer(Value value)
	{
		this.properties = new TreeMap<>();
		this.state = new HashSet<>();
		this.description = new String();

		this.res = value;

		this.state.add(LinkingState.NOT_LINKED);
	}


	public EntityContainer(Value res, NavigableMap<String, PropertyContainer> properties, LinkingState ls, String description)
	{
		this.state = new HashSet<LinkingState>();

		this.res = res;
		this.properties = properties;
		this.description = description;

		this.state.add(ls);
	}


	public Value getURI()
	{
		return this.res;
	}


	public Value getRedirectURI()
	{
		return ResUtils.getRedirectURI(this.res);
	}


	public String getShortURI()
	{
		return ResUtils.createShortURI(this.res.getValue());
	}


	public List<PropertyContainer> getProperties()
	{
		return new LinkedList<>(new TreeSet<>(this.properties.values()).descendingSet());
	}


	public String getDescription()
	{
		return this.description;
	}


	public PropertyContainer getProperty(String propertyURI)
	{
		return this.properties.get(propertyURI);
	}


	public void addProperty(PropertyContainer pc)
	{
		PropertyContainer property = this.properties.get(pc.getPropertyURI());

		if(property == null) {
			this.properties.put(pc.getPropertyURI(), pc);
			return;
		}

		for(String lang : pc.getLanguages()) {
			for(Value value : pc.getValues().get(lang)) {
				property.addValue(lang, value);
			}
		}
	}


	public void delProperty(String propertyURI)
	{
		this.properties.remove(propertyURI);
	}


	public void addState(LinkingState ls)
	{
		if(ls.equals(LinkingState.NOT_LINKED)) {
			state.clear();
		} else if(state.contains(LinkingState.NOT_LINKED)) {
			state.clear();
		}
		state.add(ls);
	}


	public void addStates(Set<LinkingState> lss)
	{
		for(LinkingState ls : lss) {
			this.addState(ls);
		}
	}


	public Set<LinkingState> getStates()
	{
		return state;
	}


	public void setDescription(String desc)
	{
		this.description = desc;
	}


	public void setProperties(NavigableMap<String, PropertyContainer> properties)
	{
		this.properties = properties;
	}


	public void loadBasicProperties() throws QueryExceptionHTTP
	{
		List<String> uris = new LinkedList<String>();

		uris.add("http://dbpedia.org/property/name");
		uris.add("http://www.w3.org/2000/01/rdf-schema#label");
		uris.add("http://xmlns.com/foaf/0.1/name");
		uris.add("http://dbpedia.org/property/alternativeNames");
		uris.add("http://dbpedia.org/property/id");
		uris.add("http://dbpedia.org/property/commonName");
		uris.add("http://www.w3.org/2004/02/skos/core#prefLabel");
		uris.add("http://www.w3.org/2002/07/owl#sameAs");
		uris.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

		Content.loadProperties(this, uris);
	}


	public Set<String> getReadableNames()
	{
		Set<String> set = new HashSet<>();
		for(Set<String> temp : getReadableNamesMap().values()) {
			set.addAll(temp);
		}
		return set;
	}


	public String getInstanceName()
	{
		return (new ArrayList<String>(this.getReadableNames())).get(0);
	}


	public Map<String, Set<String>> getReadableNamesMap()
	{
		Map<String, Set<String>> result = new HashMap<>();

		List<String> uris = Settings.EC_READABLE_NAMES;
		
		
		for(int i = 0; i < uris.size(); i++) {
			PropertyContainer pc = this.getProperty(uris.get(i));
		
			if(pc == null) {
				continue;
			}
		
			Set<String> values = new HashSet<>();
			values.addAll(ResUtils.getLocalNameSet(pc.getValuesByLanguage("en")));
			values.addAll(ResUtils.getLocalNameSet(pc.getValuesByLanguage("")));
			result.put(ResUtils.createShortURI(uris.get(i)), values);
			
		}

		if(result.size() == 0) {
			Set<String> tmp = new HashSet<String>();
			
			tmp.add(ResUtils.getLocaleName(this.res.getValue()).replace("_", " "));
			result.put("", tmp);
		}
		
		return result;
	}


	public Set<String> getBasicConcepts()
	{
		return new HashSet<>(this.getBasicConceptMap().values());
	}


	public Map<String, String> getBasicConceptMap()
	{
		Map<String, String> result = new HashMap<>();

		PropertyContainer pc = this.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		if(pc == null) { return result; }

		Set<Value> values = new HashSet<>();
		values.addAll(pc.getValuesByLanguage("en"));
		values.addAll(pc.getValuesByLanguage(""));
		for(Value value : values) {
			if(!value.isResource()) {
				continue;
			}
			result.put(ResUtils.createShortURI(value.getValue()), ResUtils.getLocaleName(value.getValue()));
		}

		return result;
	}


	public Set<String> getSameAsRelation()
	{
		Set<String> result = new HashSet<>();

		PropertyContainer pc = this.getProperty("http://www.w3.org/2002/07/owl#sameAs");

		if(pc == null) {
			result.add(LinkingState.NOT_LINKED.toString());
			return result;
		}

		for(Value value : pc.getValuesByLanguage("")) {
			if(value.isResource()) {
				result.add(value.getValue());
			}
		}

		return result;
	}


	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof EntityContainer) {
			EntityContainer ec = (EntityContainer) obj;
			if(this.getURI().equals(ec.getURI())) { return true; }
		}

		return false;
	}


	@Override
	public int hashCode()
	{
		return this.res.hashCode();
	}


	@Override
	public int compareTo(EntityContainer e)
	{
		if(this.getURI().compareTo(e.getURI()) != 0) {
			return this.getURI().compareTo(e.getURI());
		} else {
			return 1;
		}
	}
}