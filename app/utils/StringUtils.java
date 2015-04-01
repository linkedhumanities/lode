package utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import models.entity.Value;

import org.apache.commons.lang.StringEscapeUtils;


public class StringUtils
{


	public static Set<String> generateNames(Set<String> data)
	{
		// adding data 'as it is' into the result set
		Set<String> temp = new HashSet<>();
		temp.addAll(data);
		//System.out.println("data!------------------------"+data+ "size : "+data.size());


		// computing searchterm combinations
		for(String name : data) 
		{
			name = name.trim();
			int commaIndex = name.indexOf(',');

			if(commaIndex != -1)	// if name contains comma
			{
				String[] subNames = name.split(",");    //subNames will always be of the form [lastName, firstName and middleName]

				subNames[0] = subNames[0].trim();
				subNames[1] = subNames[1].trim();

				// Rule : 'X Y Z, A B C' ==>  add 'X Y Z, A' and 'X Y Z'

				if(!subNames[0].equals(""))					//adding 'X Y Z'
				{	
					temp.add(subNames[0]);			

					if(!subNames[1].equals(""))					
						if(subNames[1].trim().split(" ").length >= 1 )			//adding 'X Y Z, A'
						{
							temp.add(subNames[0]+", "+subNames[1].trim().split(" ")[0]); //added 'X Y Z, A'
							temp.add(subNames[1].trim().split(" ")[0]+" "+subNames[0]);  //added 'A X Y Z'
						}
				}
				
		//		System.out.println("temp comma !------------------------"+temp);


			}
			else // if name does not contain comma
			{
				// Rule : 'X Y Z' ==> add 'Y Z' and 'Z'
				String[] nameArray = name.split(" ");

				if(nameArray.length == 3)     // if name contains atleast 3 words
				{
					temp.add(nameArray[1]+" "+nameArray[2]);	// add 'Y Z'
					temp.add(nameArray[2]);						// add 'Z'
				}	
				else
				{
					//System.out.println("nameArray no comma!------------------------"+nameArray);

				}

				//System.out.println("temp no comma!------------------------"+temp);

			}
		}

		// validating contents of result set before returning result set
		Set<String> result = new HashSet<>();
		for(String name : temp) {
			if(validName(name)) {
				result.add(name.toLowerCase().trim());
			}
		}

		//System.out.println("result!------------------------"+result);
		return result;


	}




	public static Set<String> generateNames1(Set<String> data)
	{
		Set<String> tmp = new HashSet<>();
		tmp.addAll(data);

		for(String name : data) {
			if(name.split(" ").length > 2) {
				String[] raw = name.trim().split(" ");
				int end = raw.length - 2;
				for(int i = 0; i <= end; i++) {
					if(raw[end + 1 - i].trim().length() > 3) {
						name = name.substring(0, name.lastIndexOf(raw[end + 1 - i].trim())).trim() + " " + raw[end + 1 - i].trim();
						tmp.add(name);
					}
				}
			} else if(name.split(" ").length == 2) {
				tmp.add(name.split(" ")[1]);
			}
		}

		Set<String> result = new HashSet<>();
		for(String name : tmp) {
			if(validName(name)) {
				result.add(name.toLowerCase().trim());
			}
		}

		return result;
	}


	public static boolean validName(String name)
	{
		String tmp = name.replaceAll("(!|\"|§|[$]|%|&|\\/|\\(|\\)|[\\[]|[\\]]|[\\{]|[\\}]|=|[?]|<|>|[|]|[\\^]|°|'|#|[*]|~|,|[+]|´|`|[.]|_| )", "");
		if(tmp.trim().length() == 0) { return false; }

		return true;
	}


	public static String deleteSC(String s)
	{
		s = s.replaceAll("(!|\"|§|[$]|%|&|\\/|\\(|\\)|[\\[]|[\\]]|[\\{]|[\\}]|=|[?]|<|>|[|]|[\\^]|°|'|#|[*]|~|,|[+]|´|`)", "");
		return s.replaceAll(" ", "_");
	}


	public static String decode(String s)
	{
		String result = new String(s);

		try {
			result = URLDecoder.decode(s, "UTF8");
		} catch(UnsupportedEncodingException | IllegalArgumentException e) {
			// e.printStackTrace();
		}

		result = StringEscapeUtils.unescapeHtml(result);

		return result;
	}


	public static List<String> stringToList(String s, List<String> fixedValues)
	{
		s = s.replaceAll("(\\[|\\])", "");
		s = s.replaceAll(",", ";");

		List<String> tempList = new ArrayList<>();
		tempList.addAll(fixedValues);

		for(String temp : s.split(";")) {
			if(!fixedValues.contains(temp.trim()) && temp.trim().length() > 0) {
				tempList.add(temp.trim());
			}
		}

		return tempList;
	}


	public static List<Value> stringToValueList(String s)
	{
		s = s.replaceAll("(\\[|\\])", "");
		s = s.replaceAll(",", ";");

		List<Value> tempList = new ArrayList<>();
		for(String temp : s.split(";")) {
			if(temp.trim().length() > 0) {
				String[] temp2 = temp.split("\\|");
				if(temp2.length == 3) {
					tempList.add(new Value(temp2[0].trim(), temp2[1].trim(), Float.valueOf(temp2[2].trim())));
				}
			}
		}
		return tempList;
	}


	public static String valueListToString(List<Value> list)
	{
		StringBuilder sb = new StringBuilder();
		for(Value v : list) {
			sb.append(v.getValue() + "|" + v.getDescription() + "|" + String.valueOf(v.getWeight()) + ";");
		}
		return sb.toString();
	}


	public static String[] splitURI(String uri)
	{
		String[] res = new String[2];

		int splitIndex = Math.max(uri.lastIndexOf("#"), uri.lastIndexOf("/")) + 1;
		res[0] = uri.substring(0, splitIndex);
		res[1] = splitIndex < uri.length() ? uri.substring(splitIndex) : "";
		res[1] = decode(res[1]);
		res[1] = deleteSC(res[1]);

		return res;
	}


	public static String getStackTrace(Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}


	public static URL toURL(String uri)
	{
		try {
			return new URL(uri);
		} catch(MalformedURLException e) {
			return null;
		}
	}

}