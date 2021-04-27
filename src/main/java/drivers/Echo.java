package drivers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Response;

/*
 * 
 * Example:
 *  ECHO "Hello, world!"
 * 
 * Response:
 *  success flag
 *  message "Hello, world!"
 *  no result table
 */
public class Echo implements Driver {
	static final Pattern pattern = Pattern.compile(
		//ECHO. at least one white space. ". create capture group to receive the word. zero or more chars that isnt ". ".
		//ECHO\s+"([^"]*)" how it looks w/o java formatting
		"ECHO\\s+\"([^\"]*)\"", //regular expression - sub language (generic) - use regexr.com
		Pattern.CASE_INSENSITIVE
	);

	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		String text = matcher.group(1);
		
		return new Response(query, true, text, null);
	}
}
