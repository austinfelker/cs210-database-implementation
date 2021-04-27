package drivers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Response;
import types.SearchList;
import types.Table;

/*
 * MUTATOR QUERY
 * Examples:
 *  CREATE TABLE m1_table01 (id INTEGER)
 *  CREATE TABLE m1_table01 (id INTEGER PRIMARY)
 * 	CREATE TABLE m1_table01 (id INTEGER PRIMARY, name STRING, flag BOOLEAN)
 *
 * Response 1:
 * 	success flag
 * 	Table name: m1_table01. # of columns: 1
 *
 * Response 2:
 * 	success flag
 * 	Table name: m1_table01. # of columns: 2
 *
 * Response 3:
 * 	success flag
 * 	Table name: m1_table01. # of columns: 3
 */
public class CreateTable implements Driver {
	static final Pattern pattern = Pattern.compile(
			//CREATE\s+TABLE\s+([a-z][a-z0-9_]*)\s+\((\s*[a-z][a-z0-9_]*\s+(?:STRING|INTEGER|BOOLEAN)(?:\s+PRIMARY)?\s*(?:,(?:\s*[a-z][a-z0-9_]*\s+(?:STRING|INTEGER|BOOLEAN)(?:\s+PRIMARY)?)\s*)*)\)
			// without java formatting^
			"CREATE\\s+TABLE\\s+([a-z][a-z0-9_]*)\\s+\\((\\s*[a-z][a-z0-9_]*\\s+(?:STRING|INTEGER|BOOLEAN)(?:\\s+PRIMARY)?\\s*(?:,(?:\\s*[a-z][a-z0-9_]*\\s+(?:STRING|INTEGER|BOOLEAN)(?:\\s+PRIMARY)?)\\s*)*)\\)",
			Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		String tableName = matcher.group(1);
		String[] group2 = matcher.group(2).split("\s*,\s*"); //split group 2 by commas [col_name TYPE, col_name TYPE PRIMARY]

		//fail checks
		if (tableName.length() > 15) return new Response(query, false, "Table names can be no more than 15 characters", null);
		if (group2.length > 15) return new Response(query, false, "Group 2 (columns) cannot be larger than 15", null);
		if (db.tables().contains(tableName)) return new Response(query, false, "Table already exists with that name", null);

		//empty lists to eventually be added to the table
		Set<Object> checkColNames = new HashSet<>();
		List<Object> colNames = new LinkedList<>();
		List<Object> colTypes = new LinkedList<>();
		int primary = -1; //flag used for telling if primary has been set already

		for (int i = 0; i < group2.length; i++) {    //array looks: [id INTEGER,flag BOOLEAN PRIMARY]
			String[] split = group2[i].split("\s+"); //makes a new array that looks [id, INTEGER]
			if (split.length == 3){ 		         //checks if the query has the word PRIMARY in it
				if (primary != -1) return new Response(query, false, "Can only have 1 primary index", null);
				primary = i;
			}
			colNames.add(split[0]);
			checkColNames.add(split[0]);
			colTypes.add(split[1].toLowerCase()); //FIX 2
		}

		//more fail checks
		if (primary == -1) return new Response(query, false, "must specify a primary column", null);
		if (colNames.size() != checkColNames.size()) return new Response(query, false, "duplicate table name not allowed", null);

		//all above^ thus completes the SANITIZATION section

		//assigns everything into the schema table
		Table result_table = new Table(
				SearchList.of(
				"table_name", tableName,
				"column_names", colNames,
				"column_types", colTypes,
				"primary_index", primary),
				new SearchList<>()
		);

		//remember to create send the table to the database for MUTATOR queries
		db.tables().put(tableName, result_table);
		return new Response(query, true, "Table name: " +tableName+ ". # of columns: " +group2.length, null);
	}
}
