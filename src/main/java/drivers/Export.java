package drivers;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import apps.Database;
import types.Entry;
import types.Response;
import types.Table;

/*
 * ACCESSOR QUERY
 * 

 * export m2_table1 to file_name.json
 * export m2_table1 as json
 * export m2_table1 to file_name.xml
 * export m2_table1 as xml
 * export m2_table1 to C:\Users\Austin\Desktop\jsonp4.json
 * 
 * Response:
 *  
 */
public class Export implements Driver {
	static final Pattern pattern = Pattern.compile(
		//w/o java formatting 
		//EXPORT\s+([a-z][a-z0-9_]*)\s+(?:TO\s+(\S+.(json|xml))|AS\s+(JSON|XML))
		"EXPORT\\s+([a-z][a-z0-9_]*)\\s+(?:TO\\s+(\\S+.(json|xml))|AS\\s+(JSON|XML))", //regular expression - sub language (generic) - use regexr.com
		Pattern.CASE_INSENSITIVE
	);

	@SuppressWarnings("unchecked")
	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		String inputTableName = matcher.group(1);
		String inputFileName = null;
		String inputFileType = null;
		
		if (!db.tables().contains(inputTableName)) return new Response(query, false, "There does not exist a table with that name", null);
		List<String> listOfColTypes = (List<String>) db.tables().get(inputTableName).schema().get("column_types");
		
		//if the TO clause was used
		if (matcher.group(4) == null) {
			inputFileName = matcher.group(2).toLowerCase();
			if (matcher.group(3).equalsIgnoreCase("json"))
				inputFileType = "json";
			else if (matcher.group(3).equalsIgnoreCase("xml"))
				inputFileType = "xml";
		}
		else if (matcher.group(2) == null) { // if the AS clause was used
			inputFileName = inputTableName.toLowerCase() + "." + matcher.group(4).toLowerCase();
			inputFileType = matcher.group(4).toLowerCase();
		}
		
		//table: has a schema and state (json1)
		
		//schema: map of string to an object (json2)
		//state : map of object to list of objects\
		
		try {
			if (inputFileType.equals("json")) {
				//outer level schema and state
				JsonObjectBuilder root_object_builder = Json.createObjectBuilder();

				//BUILD THE SCHEMA ---------------------------------------------------------
				JsonObjectBuilder schema_object_builder = Json.createObjectBuilder();

				//table_name
				schema_object_builder.add("table_name", (String) db.tables().get(inputTableName).schema().get("table_name"));
				
				//column_names
				JsonArrayBuilder column_names_builder = Json.createArrayBuilder();
				for (String colName: (List<String>) db.tables().get(inputTableName).schema().get("column_names"))
					column_names_builder.add(colName);
				schema_object_builder.add("column_names", column_names_builder.build());
				
				//column_types
				JsonArrayBuilder column_types_builder = Json.createArrayBuilder();
				for (String colType: (List<String>) db.tables().get(inputTableName).schema().get("column_types"))
					column_types_builder.add(colType);
				schema_object_builder.add("column_types", column_types_builder.build());
				
				//primary_index
				schema_object_builder.add("primary_index", (Integer) db.tables().get(inputTableName).schema().get("primary_index"));

				root_object_builder.add("schema", schema_object_builder); // adds all schema subsections into the root json object(aka map)

				//BUILD THE STATE ---------------------------------------------------------
				

				JsonArrayBuilder state_array_builder = Json.createArrayBuilder();


				for (Entry<Object, List<Object>> entry: db.tables().get(inputTableName).state()) {
					JsonArrayBuilder row_builder = Json.createArrayBuilder();
					List<Object> row = entry.value();
					int count = 0;
					for (Object element: row) {
						if (listOfColTypes.get(count).equals("string") && element != null)
							row_builder.add((String) element);
						else if (listOfColTypes.get(count).equals("integer") && element != null)
							row_builder.add((Integer) element);
						else if (listOfColTypes.get(count).equals("boolean") && element != null)
							row_builder.add((Boolean) element);
						else
							row_builder.addNull();
						count++;
					}
					state_array_builder.add(row_builder.build());

				}
				root_object_builder.add("state", state_array_builder);



				JsonObject root_object = root_object_builder.build();

				//boilerplate
				java.util.Map<String, Boolean> props = new java.util.HashMap<>();
				props.put(JsonGenerator.PRETTY_PRINTING, true);
				JsonWriter writer = Json.createWriterFactory(props).createWriter(new FileOutputStream(inputFileName));
				writer.writeObject(root_object);
				writer.close();
			}
			else if (inputFileType.equals("xml")) {
				
				StringWriter output = new StringWriter();
				XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(output);

				
				writer.writeStartDocument();
				writer.writeStartElement("table");
				
				//BUILD THE SCHEMA ---------------------------------------------------------
				writer.writeStartElement("schema");
				
				//table_name
				writer.writeStartElement("table_name");
				writer.writeCharacters((String) db.tables().get(inputTableName).schema().get("table_name"));
				writer.writeEndElement(); //end table_name
				
				//column_names
				writer.writeStartElement("column_names");
				for (String colName: (List<String>) db.tables().get(inputTableName).schema().get("column_names")) {
					writer.writeStartElement("name");
					writer.writeCharacters(colName);
					writer.writeEndElement();
			    }
				writer.writeEndElement(); //end column_names
				
				//column_types
				writer.writeStartElement("column_types");
				for (String colType: (List<String>) db.tables().get(inputTableName).schema().get("column_types")) {
					writer.writeStartElement("type");
					writer.writeCharacters(colType);
					writer.writeEndElement();
			    }
				writer.writeEndElement(); //end column_types
				
				//primary_index
				writer.writeStartElement("primary_index");
				writer.writeCharacters(Integer.toString((int) db.tables().get(inputTableName).schema().get("primary_index")));
				writer.writeEndElement(); //end primary_index
				
				writer.writeEndElement(); //end schema
				
				//BUILD THE STATE -----------------------------------------------------------
				writer.writeStartElement("state");
				
				//for each row
				for (Entry<Object, List<Object>> entry: db.tables().get(inputTableName).state()) {
					List<Object> row = entry.value();
					writer.writeStartElement("row");
					int count = 0;
					for (Object element: row) {
						writer.writeStartElement("element");
						if (listOfColTypes.get(count).equals("string") && element != null) {
							writer.writeCharacters(element.toString());
						}
						else if (listOfColTypes.get(count).equals("integer") && element != null) {
							writer.writeCharacters(Integer.toString((int) element));
						}
						else if (listOfColTypes.get(count).equals("boolean") && element != null) {
							writer.writeCharacters(Boolean.toString((boolean) element));
						}
						else
							writer.writeAttribute("null", "yes");
						writer.writeEndElement(); //end element
						count++;
					}
					writer.writeEndElement();
				}
				
				writer.writeEndElement(); //end state
				
				writer.writeEndElement(); //end table
				writer.writeEndDocument();
				writer.close();

				//all the below is for formatting in XML
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
			    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			    Source from = new StreamSource(new StringReader(output.toString()));
			    Result to = new StreamResult(new FileWriter(inputFileName));
			    transformer.transform(from, to);
				
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		Table _export = new Table(
				db.tables().get(inputTableName).schema(),
				db.tables().get(inputTableName).state()
			);
		_export.schema().put("table_name", "_export");
		
		return new Response(query, true, null, _export);
	}
}
