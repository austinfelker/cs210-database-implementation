package drivers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Response;

/*
 * gets the table from the db
 *
 * ACCESSOR QUERY
 * Examples:
 *  DUMP TABLE
 *
 *  response:
 *  success flag
 * 	"Table name: tableName. # of rows: #
 * 		prints _________Table_____________
 *
 */
public class DumpTable implements Driver {
	static final Pattern pattern = Pattern.compile(
			// w/o java format
			//DUMP\s+TABLE\s+([a-z][a-z0-9_]*)
			"DUMP\\s+TABLE\\s+([a-z][a-z0-9_]*)",
			Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		String tableName = matcher.group(1);

		//fail checks
		if (!db.tables().contains(tableName)) return new Response(query, false, "There does not exist a table with that name", null);

		int rows = db.tables().get(tableName).state().size();

		return new Response(query, true, "Table name: " +tableName+ ". # of rows: " +rows, db.tables().get(tableName));
	}
}
