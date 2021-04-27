package drivers;

import apps.Database;
import types.Response;

/*
 * Example:
 *  NONSENSE QUERY ASDF
 * 
 * Response:
 * 	failure flag
 * 	message "Unrecognized query"
 * 	no result table
 */
public class Unrecognized implements Driver {
	@Override
	public Response execute(String query, Database db) {
		return new Response(query, false, "Unrecognized query", null);
	}
}
