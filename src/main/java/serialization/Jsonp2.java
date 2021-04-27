package serialization;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import types.Entry;
import types.Map;
import types.SearchList;

public class Jsonp2 {
	public static void main(String[] args) {
		Map<String, List<String>> a = new SearchList<>();
		a.put("beta", List.of("beta", "zeta", "eta", "theta"));
		a.put("epsilon", List.of("epsilon", "upsilon"));
		a.put("phi", List.of("phi", "psi", "chi"));
		write(a);
		System.out.println(a);

		Map<String, List<String>> b = read();
		System.out.println(b);

		System.out.println(a.equals(b));
		System.out.println(a == b);
	}

	// Using JSON-P (JSON Processing API)

	public static void write(Map<String, List<String>> map) {
		try {
			JsonObjectBuilder root_object_builder = Json.createObjectBuilder();
		    for (Entry<String, List<String>> entry: map) {
		    	JsonArrayBuilder array_builder = Json.createArrayBuilder();
		    	for (String element: entry.value()) {
		    		array_builder.add(element);
		    	}
		    	JsonArray array = array_builder.build();

		    	root_object_builder.add(entry.key(), array);
		    }
		    JsonObject root_object = root_object_builder.build();

		    String filename = "C:\\Users\\Austin\\Desktop\\jsonp2.json";
		    java.util.Map<String, Boolean> props = new java.util.HashMap<>();
			props.put(JsonGenerator.PRETTY_PRINTING, true);
		    JsonWriter writer = Json.createWriterFactory(props).createWriter(new FileOutputStream(filename));
		    writer.writeObject(root_object);
		    writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, List<String>> read() {
		Map<String, List<String>> map = null;
		try {
			String filename = "C:\\Users\\Austin\\Desktop\\jsonp2.json";
			JsonReader reader = Json.createReader(new FileInputStream(filename));
		    JsonObject root_object = reader.readObject();
		    reader.close();

		    map = new SearchList<>();
		    for (String key: root_object.keySet()) {
		    	JsonArray array = root_object.getJsonArray(key);
		    	List<String> list = new LinkedList<>();
		    	for (int i = 0; i < array.size(); i++) {
		    		list.add(array.getString(i));
		    	}
		    	map.put(key, list);
		    }
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
