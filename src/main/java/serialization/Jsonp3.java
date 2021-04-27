package serialization;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

/**
 * puts the
 * @author Austin
 *
 */
public class Jsonp3 {
	public static void main(String[] args) {
		List<List<String>> a = new LinkedList<>();
		a.add(List.of("beta", "zeta", "eta", "theta"));
		a.add(List.of("epsilon", "upsilon"));
		a.add(List.of("phi", "psi", "chi"));
		write(a);
		System.out.println(a);

		List<List<String>> b = read();
		System.out.println(b);

		System.out.println(a.equals(b));
		System.out.println(a == b);
	}

	// Using JSON-P (JSON Processing API)

	public static void write(List<List<String>> lists) {
		try {
			JsonArrayBuilder root_array_builder = Json.createArrayBuilder();
		    for (List<String> inner_list: lists) {
		    	JsonArrayBuilder inner_array_builder = Json.createArrayBuilder();
		    	for (String element: inner_list) {
		    		inner_array_builder.add(element);
		    	}
		    	JsonArray inner_array = inner_array_builder.build();

		    	root_array_builder.add(inner_array);
		    }
		    JsonArray root_array = root_array_builder.build();

		    String filename = "C:\\Users\\Austin\\Desktop\\jsonp3.json";
			java.util.Map<String, Boolean> props = new java.util.HashMap<>();
			props.put(JsonGenerator.PRETTY_PRINTING, true);
		    JsonWriter writer = Json.createWriterFactory(props).createWriter(new FileOutputStream(filename));
		    writer.writeArray(root_array);
		    writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<List<String>> read() {
		List<List<String>> lists = null;
		try {
			String filename = "C:\\Users\\Austin\\Desktop\\jsonp3.json";
			JsonReader reader = Json.createReader(new FileInputStream(filename));
			JsonArray root_array = reader.readArray();
		    reader.close();

		    lists = new LinkedList<>();
		    for (int i = 0; i < root_array.size(); i++) {
		    	JsonArray inner_array = root_array.getJsonArray(i);
		    	List<String> inner_list = new LinkedList<>();
		    	for (int j = 0; j < inner_array.size(); j++) {
		    		inner_list.add(inner_array.getString(j));
		    	}
		    	lists.add(inner_list);
		    }
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return lists;
	}
}
