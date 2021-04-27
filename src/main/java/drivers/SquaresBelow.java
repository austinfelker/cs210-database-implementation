package drivers;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Response;
import types.SearchList;
import types.Table;

/*
 * For Module 0, implement this driver.
 * 
 * Examples:
 * 	SQUARES BELOW 20
 * 	SQUARES BELOW 30 AS a
 * 	SQUARES BELOW 15 AS a, b
 * 
 * 	// pseudocode for the above match
 *  // code from range regex. required portion (optional portion for the AS clause (optional comma second name)
 * 	//(?:code) creates a group without capturing
 * 	//code? on the end makes the preceding thing optional
 * 
 * Response 1:
 *  success flag
 *  message "There were 5 results."
 * 	result table
 * 		primary integer column "x", integer column "x_squared"
 *		rows [0, 0]; [1, 1]; [2, 4]; [3, 9]; [4, 16]
 *
 * Response 2:
 *  success flag
 *  message "There were 6 results."
 * 	result table
 * 		primary integer column "a", integer column "a_squared"
 *		rows [0, 0]; [1, 1]; [2, 4]; [3, 9]; [4, 16]; [5, 25]
 *
 * Response 3:
 *  success flag
 *  message "There were 4 results."
 * 	result table
 * 		primary integer column "a", integer column "b"
 *		rows [0, 0]; [1, 1]; [2, 4]; [3, 9]
 */
public class SquaresBelow implements Driver {
	static final Pattern pattern = Pattern.compile(
	//	 SQUARES\s+BELOW\s+([0-9]+)(?:\s+AS\s+(\w+)\s*(?:,\s*(\w+))?)?
		"SQUARES\\s+BELOW\\s+([0-9]+)(?:\\s+AS\\s+(\\w+)\\s*(?:,\\s*(\\w+))?)?",
		Pattern.CASE_INSENSITIVE
	);
	
	@Override
	public Response execute(String query, Database db) {

		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		//assigns the groups made from the regex pattern
		int upper = Integer.parseInt(matcher.group(1));
		String bname = matcher.group(2) != null ? matcher.group(2) : "x"; //if group 2 not null, bname=group 2. else bname=default
		String sname = matcher.group(3) != null ? matcher.group(3) : bname+"_squared";
		
		if (bname.equals(sname))
			return new Response(query, false, "Column names must be distinct", null);
		
		Table result_table = new Table(
			SearchList.of(
				"table_name", "_squares",
				"column_names", List.of(bname,sname),
				"column_types", List.of("integer","integer"),
				"primary_index", 0
			),
			new SearchList<>()
		);
		
		for (int i = 0; i*i < upper; i++) {
			List<Object> row = new LinkedList<>();
			row.add(i);
			row.add(i*i);
			result_table.state().put(i, row);
		}
		
		return new Response(query, true, null, result_table);
	}
}
