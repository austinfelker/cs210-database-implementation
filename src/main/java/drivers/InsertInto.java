package drivers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Response;

/*
 * inserts values into the specified columns
 *
 * MUTATOR QUERY
 * Examples:
 *  INSERT INTO table_name (col5,col2,col3) VALUES (val1,val2,val3)
 *
 *  response:
 *  success flag
 *
 */
public class InsertInto implements Driver {
	static final Pattern pattern = Pattern.compile(
			/* 
			 *REGEX SYNTAX:
			 *use normal special normal pattern (col_name is the same as table_name regex)
			 * the values part of the pattern has either
			 *
			 * string|integer|boolean|null
			 * regex for string is described in the doc. one or more characters that doesnt contain "
			 * integer is numbers
			 * boolean is true or false
			 * null is null
			 */
			
			//(INSERT|REPLACE)\s+INTO\s+([a-z][a-z0-9_]*)\s+(?:\((\s*[a-z][a-z0-9_]*\s*(?:,\s*[a-z][a-z0-9_]*\s*)*)\))*\s+VALUES\s+\(\s*((?:"[^"]*"|[0-9]+|true|false|null)\s*(?:,\s*(?:"[^"]*"|[0-9]+|true|false|null)\s*)*)\)
			"(INSERT|REPLACE)\\s+INTO\\s+([a-z][a-z0-9_]*)\\s+(?:\\((\\s*[a-z][a-z0-9_]*\\s*(?:,\\s*[a-z][a-z0-9_]*\\s*)*)\\))?\\s*VALUES\\s+\\(\\s*((?:\"[^\"]*\"|(?:\\+|-)?[0-9]+|true|false|null)\\s*(?:,\\s*(?:\"[^\"]*\"|(?:\\+|-)?[0-9]+|true|false|null)\\s*)*)\\)",
			Pattern.CASE_INSENSITIVE
	);

	@SuppressWarnings("unchecked")
	@Override
	public Response execute(String query, Database db) {	
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;
		
		//separate each regex group with >-------------------------------------------------------------------------------------------------------------------------------
		String tableName = matcher.group(2);
		if (!db.tables().contains(tableName)) return new Response(query, false, "There does not exist a table with that name", null);
		if (tableName.length() > 15) return new Response(query, false, "Table names can be no more than 15 characters", null);
		
		//----------------------------------------------------------------------------------------------------------------------------------------------------------------
		/* group 3 : col names
		 * for each name:
		 * add the name to a set to detect deuplicates
		 * add the corresponding schema index to the pointers list
		 * (find the index by doing a linear search)
		 */
		
		String[] inputColNames = (Objects.isNull(matcher.group(3)) ? null : matcher.group(3).split(","));
		Set<Object> checkColNames = new HashSet<>();
		List<Integer> pointers = new LinkedList<>();
		//list of column names from the db table
		List<String> dbColNames = (List<String>) db.tables().get(tableName).schema().get("column_names");
		
		if (Objects.isNull(matcher.group(3))) {
			for (int i = 0; i < dbColNames.size(); i++)
				pointers.add(i);
		} 
		else {
			for (int i = 0; i < inputColNames.length; i++) { // array looks [queryColName1, queryColName2]
				inputColNames[i] = inputColNames[i].strip();
				checkColNames.add(inputColNames[i]); // add the name to a set to detect duplicates

				// linear search for index of dbColNames
				for (int j = 0; j < dbColNames.size(); j++)
					if (inputColNames[i].equals(dbColNames.get(j)))
						pointers.add(j);
			}
			// fail check
			if (checkColNames.size() != inputColNames.length) return new Response(query, false, "duplicate table name not allowed", null);
		}
		//-------------------------------------------------------------------------------------------------------------------------------------------------------------------
		//group 4 : values
		String[] inputValues = matcher.group(4).split(",");
		if (inputValues.length != pointers.size()) return new Response(query, false, "must have the same number of values as columns", null);
		List<Object> dbColTypes = (List<Object>) db.tables().get(tableName).schema().get("column_types");
		List<Object> row = new LinkedList<>();
		for (int i = 0; i < dbColNames.size(); i++) row.add(null); //need to add nulls to the list as placeholders
		int rowCount = 0;
		
		/*
		 * 
		 */
	
		for (int i=0; i < inputValues.length; i++) { //array looks ["qrst", 4, true]
			inputValues[i] = inputValues[i].strip();
			String type = null;
			if (inputValues[i].equalsIgnoreCase("null")) {
				row.set(pointers.get(i), null);
			}
			if (inputValues[i].charAt(0) == '"') { //if string
				type = "string";
				String string = inputValues[i].substring(1, inputValues[i].length()-1);
				if (string.length() > 127) return new Response(query, false, "Strings can be no more than 127 chars", null);
				if (!type.equals( dbColTypes.get(pointers.get(i)))) return new Response(query, false, "value type doesn't match with the inputColName", null);
				row.set( pointers.get(i), string); //put the inputValue in the correct location in the row
			} 
			else if (Character.isDigit(inputValues[i].charAt(0)) || Character.isDigit(inputValues[i].charAt(1)) ) { //if integer
				type = "integer";
				String string = new String(inputValues[i]);
				if (string.startsWith("0") && string.length() > 1) return new Response(query, false, "leading zeros not suports", null);
				if (!isParsable(inputValues[i])) return new Response(query, false, "Integer out of bounds", null);
				if (!type.equals( dbColTypes.get(pointers.get(i)))) return new Response(query, false, "value type doesn't match with the inputColName", null);
				row.set(pointers.get(i), Integer.parseInt(inputValues[i]));
			} 
			else if (inputValues[i].equalsIgnoreCase("true") || inputValues[i].equalsIgnoreCase("false")) { //if boolean
				type = "boolean";
				if (!type.equals( dbColTypes.get(pointers.get(i)))) return new Response(query, false, "value type doesn't match with the inputColName", null);
				row.set(pointers.get(i), Boolean.parseBoolean(inputValues[i]));
			}
		}

		//-------------------------------------------------------------------------------------------------------------------------------------------------------------
		//group 1 for whether the keyword is insert or replace.
		int primaryIndexOfCol = (int) db.tables().get(tableName).schema().get("primary_index");
		if (row.get(primaryIndexOfCol) == null) return new Response(query, false, "row's primary index is null", null);
		Object key =  row.get(primaryIndexOfCol);
		// if keyword is insert and the key is already contained: reject, else put in db
		if (matcher.group(1).strip().equalsIgnoreCase("insert") && db.tables().get(tableName).state().contains(key))
			return new Response(query, false, "insert keyword only works if the table doesn't contain a duplicate key", null);
		else {
			db.tables().get(tableName).state().put(key, row);
			rowCount++;
		}

		
		return new Response(query, true, "Table name: " +tableName+ ". # of rows inserted/replaced: " +rowCount, null);
	}
	
	boolean isParsable(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}
}
