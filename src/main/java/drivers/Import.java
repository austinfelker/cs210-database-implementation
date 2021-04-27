package drivers;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import apps.Database;
import types.Map;
import types.Response;
import types.SearchList;
import types.Table;

/*
 * ACCESSOR QUERY
 * 
 * import 412yujj.xml
 * import C:\oiawdjiajawd.xml to m2_table2
 * import C:\oiawdjiajawd.json to m2_table2
 * import 412yujj.json
 * 
 * 
 * 
 * Response:
 *  
 */
public class Import implements Driver {
	static final Pattern pattern = Pattern.compile(
		//w/o java formatting 
		//IMPORT\s+(\S+.(json|xml))(?:\s+TO\s+([a-z][a-z0-9_]*))?
		"IMPORT\\s+(\\S+.(json|xml))(?:\\s+TO\\s+([a-z][a-z0-9_]*))?", //regular expression - sub language (generic) - use regexr.com
		Pattern.CASE_INSENSITIVE
	);

	@SuppressWarnings("unchecked")
	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		String inputFileName = matcher.group(1);
		String inputFileType = matcher.group(2).toLowerCase();
		String resultTableName = null;
		
		Map<String, Object> schema = new SearchList<>();
	    Map<Object, List<Object>> state = new SearchList<>();
		
		try {
			if (inputFileType.equals("json")) {
				
				JsonReader reader = Json.createReader(new FileInputStream(inputFileName));
			    JsonObject root_object = reader.readObject();
			    reader.close();
			    
			    for (String key: root_object.keySet()) {
			    	if (key.equals("schema")) { //READ THE SCHEMA -------------------------------------------------------------------
			    		JsonObject schema_object = root_object.getJsonObject("schema");
			    		
			    		//table_name
						if (matcher.group(3) != null) { //if the TO table clause was used, use that table name as the table name
							schema.put("table_name", matcher.group(3));
							resultTableName = matcher.group(3);
						}
						else { //else use the table name that is stored in the file
							schema.put("table_name", schema_object.getString("table_name"));
							resultTableName = schema_object.getString("table_name");
						}
			    		
			    		//column_names
			    		List<String> listColNames = new LinkedList<>();
			    		JsonArray column_names = schema_object.getJsonArray("column_names");
			    		for (int i = 0; i < column_names.size(); i++) {
			    			listColNames.add(column_names.getString(i));
			    		}
			    		schema.put("column_names", listColNames);
			    		
			    		//column_types
			    		List<String> listColTypes = new LinkedList<>();
			    		JsonArray column_types = schema_object.getJsonArray("column_types");
			    		for (int i = 0; i < column_types.size(); i++) {
			    			listColTypes.add(column_types.getString(i));
			    		}
			    		schema.put("column_types", listColTypes);
			    		
			    		//primary_index
			    		schema.put("primary_index", schema_object.getInt("primary_index"));
			    	}
			    	else if (key.equals("state")) { //READ THE STATE ---------------------------------------------------------------
			    		
			    		List<String> listOfColTypes = (List<String>) schema.get("column_types");
						int primaryIndex = (int) schema.get("primary_index");
			    		
			    		JsonArray state_array = root_object.getJsonArray("state"); //makes the variable state_object the array that contains the other arrays
			    		
			    		
					    for (int i = 0; i < state_array.size(); i++) { //for each list
					    	JsonArray inner_array = state_array.getJsonArray(i); //put the list into java readable format
					    	
					    	List<Object> elements = new LinkedList<>(); //create new empty list of elements
					    	for (int j = 0; j < inner_array.size(); j++) { //for each element, add it to the list
					    		
								if (listOfColTypes.get(j).equals("string") && inner_array.get(j) != JsonValue.NULL)
									elements.add(inner_array.getString(j));
								else if (listOfColTypes.get(j).equals("integer") && inner_array.get(j) != JsonValue.NULL)
									elements.add(inner_array.getInt(j));
								else if (listOfColTypes.get(j).equals("boolean") && inner_array.get(j) != JsonValue.NULL)
									elements.add(inner_array.getBoolean(j));
								else
									elements.add(null);
					    		
					    	}
					    	
					    	state.put(elements.get(primaryIndex), elements); //add that elements list into the state map using the primary index as the key
					    }
					    
			    	}
			    	
			    	
			    }
			}
			else if (inputFileType.equals("xml")) {
				
				XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileReader(inputFileName));

				
				List<Object> list = null;
				Object element = null;
				
				List<String> listOfColTypes = null;
				int primaryIndex = 0;
				int count = 0; 
				
				while (reader.hasNext()) {
					reader.next();

					if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
						if (reader.getLocalName().equalsIgnoreCase("table")) {
							//do nothing
						}
						else if (reader.getLocalName().equalsIgnoreCase("schema")) {
							//do nothing
						}
						else if (reader.getLocalName().equalsIgnoreCase("table_name")) {
							element = reader.getElementText();
							resultTableName = (String) element;
						}
						else if (reader.getLocalName().equalsIgnoreCase("column_names")) {
							list = new LinkedList<>();
						}
						else if (reader.getLocalName().equalsIgnoreCase("name")) {
							element = reader.getElementText();
						}
						else if (reader.getLocalName().equalsIgnoreCase("column_types")) {
							list = new LinkedList<>();
						}
						else if (reader.getLocalName().equalsIgnoreCase("type")) {
							element = reader.getElementText();
						}
						else if (reader.getLocalName().equalsIgnoreCase("primary_index")) {
							element = reader.getElementText() ;
						}
						else if (reader.getLocalName().equalsIgnoreCase("state")) {
							primaryIndex = (int) schema.get("primary_index");
							listOfColTypes = (List<String>) schema.get("column_types");
						}
						else if (reader.getLocalName().equalsIgnoreCase("row")) {
							list = new LinkedList<>();
						}
						else if (reader.getLocalName().equalsIgnoreCase("element")) {
							//check if there is a specified null in that xml element
							if (reader.getAttributeValue(null, "null") != null && reader.getAttributeValue(null, "null").equals("yes"))
								element = null;
							else 
								element = reader.getElementText();
						}
						
					}

					if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
						if (reader.getLocalName().equalsIgnoreCase("table")) {
							break;
						}
						else if (reader.getLocalName().equalsIgnoreCase("schema")) {
							//do nothing
						}
						else if (reader.getLocalName().equalsIgnoreCase("table_name")) {
							schema.put("table_name", element);
							element = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("column_names")) {
							schema.put("column_names", list);
							list = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("name")) {
							list.add(element);
							element = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("column_types")) {
							schema.put("column_types", list);
							list = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("type")) {
							list.add(element);
							element = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("primary_index")) {
							schema.put("primary_index", Integer.valueOf((String) element));
							element = null;
						}
						else if (reader.getLocalName().equalsIgnoreCase("state")) {
							//do nothing
						}
						else if (reader.getLocalName().equalsIgnoreCase("row")) {
							state.put(list.get(primaryIndex), list);
							list = null;
							count = 0;
						}
						else if (reader.getLocalName().equalsIgnoreCase("element")) {
							//check for all types before adding to the list
							if (listOfColTypes.get(count).equals("string") && element != null) 
								list.add(element);
							else if (listOfColTypes.get(count).equals("integer") && element != null) 
								list.add(Integer.valueOf((String) element));
							else if (listOfColTypes.get(count).equals("boolean") && element != null) 
								list.add(Boolean.valueOf((String) element) );
							else 
								list.add(null);
							
							count++;
							element = null;
						}
						
					}
				}

				reader.close();
			}
		}
		catch (IOException e) {
			return new Response(query, false, "That file name doesn't exist", null);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		
		
		//avoid name conflicts by adding _x as the suffix to the resultTableName until it works
		if (db.tables().contains(resultTableName)) {
			resultTableName += "_";
			String copy = resultTableName;
			for (int i = 1;; i++) {
				if (!db.tables().contains(copy + i)) {
					resultTableName = copy + i;
					break;
				}
			}
		}
		

		//put the state and schema into a new Table
		Table result_table = new Table(
				schema,
				state
			);
		
		//store the table in the database
		db.tables().put(resultTableName, result_table);
		
		//copy table to be able to return the table with the name _import in the response
		Table _import = new Table(
				schema,
				state
			);
		_import.schema().put("table_name", "_import");
		
		return new Response(query, true, null, _import);
	}
}
