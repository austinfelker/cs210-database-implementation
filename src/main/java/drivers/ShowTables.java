package drivers;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Entry;
import types.Response;
import types.SearchList;
import types.Table;

/*
 * ACCESSOR QUERY
 *
 * Examples:
 * 	SHOW TABLES
 *
 * response:
 *  success flag
 * 	Number of tables: number of rows
 *
 */
public class ShowTables implements Driver {
	static final Pattern pattern = Pattern.compile(
	//	 SHOW\s+TABLES without java formatting
		"SHOW\\s+TABLES",
		Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;


		Table result_table = new Table(
				SearchList.of(
					"table_name", "_tables",
					"column_names", List.of("table_name", "row_count"),
					"column_types", List.of("string", "integer"),
					"primary_index", 0
				),
				new SearchList<>()
			);

		//TODO: figure out how this works exactly
		//for each (loop) map entry (table) in db.tables()
		for (Entry<String, Table> entry: db.tables()) {

			//assign the table name string and assign the table
			String tableName = entry.key();
			Table table = entry.value();

			List<Object> row = new LinkedList<>();
			row.add(tableName);
			row.add(table.state().size());
			result_table.state().put(tableName, row);
		}

		return new Response(query, true, "Number of tables: " +result_table.state().size(), result_table);
	}
}
