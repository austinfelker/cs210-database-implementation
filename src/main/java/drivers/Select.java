package drivers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apps.Database;
import types.Entry;
import types.Response;
import types.SearchList;
import types.Table;

/*
 * gets sections of a table from a db based on the parameters.
 *
 * ACCESSOR QUERY
 * Examples:
 *  SELECT p AS s FROM m2_table1 WHERE col_name1 > 4
 *
 *  response:
 *  success flag
 * 	"Table name: tableName. # of rows: #
 * 		prints _________Table_____________
 *
 */
public class Select implements Driver {
	static final Pattern pattern = Pattern.compile(
			// w/o java format
			//SELECT (\*|[a-z][a-z0-9_]*(?:\s*(?:(?:AS)?\s*[a-z][a-z0-9_]*)?\s*(?:,\s*[a-z][a-z0-9_]*(?:\s*(?:AS)?\s*[a-z][a-z0-9_]*\s*)?)*)?)\s+FROM\s+([a-z][a-z0-9_]*)(?:\s+WHERE\s+([a-z][a-z0-9_]*)\s*(=|<>|<|>|<=|>=)\s*("[^"]*"|[0-9]+|true|false|null))?
			"SELECT (\\*|[a-z][a-z0-9_]*(?:\\s*(?:(?:AS)?\\s*[a-z][a-z0-9_]*)?\\s*(?:,\\s*[a-z][a-z0-9_]*(?:\\s*(?:AS)?\\s*[a-z][a-z0-9_]*\\s*)?)*)?)\\s+FROM\\s+([a-z][a-z0-9_]*)(?:\\s+WHERE\\s+([a-z][a-z0-9_]*)\\s*(=|<>|<|>|<=|>=)\\s*(\"[^\"]*\"|[0-9]+|true|false|null))?",
			Pattern.CASE_INSENSITIVE
	);

	@SuppressWarnings({ "unchecked", "unused" })
	@Override
	public Response execute(String query, Database db) {
		Matcher matcher = pattern.matcher(query.strip());
		if (!matcher.matches()) return null;

		
		String inputTableName = matcher.group(2);
		if (!db.tables().contains(inputTableName)) return new Response(query, false, "There does not exist a table with that name", null);
		
		List<String> resultColNames = new LinkedList<>();
		List<String> resultColTypes = new LinkedList<>();
		List<Integer> pointers = new LinkedList<>();
		int rowCount = 0;
		
		List<String> dbColNames = (List<String>) db.tables().get(inputTableName).schema().get("column_names");//list of column names from the db table
		List<String> dbColTypes = (List<String>) db.tables().get(inputTableName).schema().get("column_types");
		Object dbPrimaryIndex = db.tables().get(inputTableName).schema().get("primary_index");
		String colNameOfPrimaryIndex = (dbColNames.get((int) dbPrimaryIndex));
		
		//adds the inputed names to the list and points them to the associated schema in the 
		String[] inputColNames = matcher.group(1).split("\\s*,\\s*"); //looks {dbColName1 AS name1,dbColName2 AS name2}
		Set<Object> setColNames = new HashSet<>();
		int resultPrimaryIndex = -1;
		
		if (inputColNames[0].equals("*")) {
			resultColNames = dbColNames;
			resultColTypes = dbColTypes;
			resultPrimaryIndex = (int) dbPrimaryIndex;
			for (int j = 0; j < dbColNames.size(); j++) {
				pointers.add(j);
			}
		}
		else {
			for (int i = 0; i < inputColNames.length; i++) { 
				String[] split = inputColNames[i].split("\s+"); //makes a new array that looks [dbColName1,AS,name1]
				String colName = split[0];
				
				
				//chooses the primary index
				if (colName.equals(colNameOfPrimaryIndex) && resultPrimaryIndex == -1) {
					resultPrimaryIndex = i;
				}
				
				if (split.length == 3) { //if AS clause
					String name = split[2];
					setColNames.add(name);
					resultColNames.add(name);
					
					for (int j = 0; j < dbColNames.size(); j++) {
						if (colName.equals(dbColNames.get(j))) {
							pointers.add(j);
							resultColTypes.add(dbColTypes.get(j));
						}
					}
					
				}
				else {
					setColNames.add(colName);
					resultColNames.add(colName);
					for (int j = 0; j < dbColNames.size(); j++) {
						if (colName.equals(dbColNames.get(j))) {
							pointers.add(j);
							resultColTypes.add(dbColTypes.get(j));
						}
					}
				}
				
			}
			// fail check
			if (setColNames.size() != inputColNames.length) return new Response(query, false, "duplicate table name not allowed", null);
			if (resultPrimaryIndex == Integer.valueOf(-1)) return new Response(query, false, "must provide a primary index", null);
		}

		//short version for if there is no where clause
		if (Objects.isNull(matcher.group(3))){
			
			Table _select = new Table(
					SearchList.of(
							"table_name", "_select",
							"column_names", resultColNames,
							"column_types", resultColTypes,
							"primary_index", resultPrimaryIndex),
					new SearchList<>()
					);
			
			for (Entry<Object, List<Object>> entry: db.tables().get(inputTableName).state()) {// looks like {xyz, -1, null}
				List<Object> row = new LinkedList<Object>();
				for (int i = 0; i < pointers.size(); i++) {
					row.add(entry.value().get(pointers.get(i)));
				}
				_select.state().put(row.get(resultPrimaryIndex), row);
			}
			
			return new Response(query, true, "Table name: " +inputTableName, _select);
		}

		Table _select = new Table(
				SearchList.of(
						"table_name", "_select",
						"column_names", resultColNames,
						"column_types", resultColTypes,
						"primary_index", resultPrimaryIndex),
				new SearchList<>()
				);

		//------------------------------------------------------------------------------------------------------------------------------------
		//group 3 string looks like:  colname <= null
		if (!Objects.isNull(matcher.group(3))) { // where clause look lhs(col_name) operator rhs(new Name)
			Object lhs = null;
			String operator = matcher.group(4);
			Object rhs = null;
			String lhsType = null;
			String rhsType = null;
			
			int indexOfColName = 0;
			//find type of left-hand-side (col_name)
			//find what index lhs is
			for (int i = 0; i < dbColNames.size(); i++ ) {
				if (dbColNames.get(i).equals(matcher.group(3))) {
					lhsType = dbColTypes.get(i);
					lhs = dbColTypes.get(i);
					indexOfColName = i;
					break;
				}
			}
			if (Objects.isNull(lhsType)) return new Response(query, false, "where clause column not contained in the database table", null);
			
			//find the type of right-hand-side (Object) and convert to literal type
			if (matcher.group(5).equalsIgnoreCase("null")) {
				rhsType = "null";
				rhs = null;
			}
			if (matcher.group(5).charAt(0) == '"') { //if string
				rhsType = "string";
				rhs = matcher.group(5).substring(1, matcher.group(5).length()-1);
			} 
			else if (Character.isDigit(matcher.group(5).charAt(0)) || Character.isDigit(matcher.group(5).charAt(1)) ) {
				rhsType = "integer";
				rhs = Integer.parseInt(matcher.group(5));
			} 
			else if (matcher.group(5).equalsIgnoreCase("true") || matcher.group(5).equalsIgnoreCase("false")) {
				rhsType = "boolean";
				rhs = Boolean.parseBoolean(matcher.group(5));
			}
			
			
			//for each state (follows table in project doc)
			for (Entry<Object, List<Object>> entry: db.tables().get(inputTableName).state()) {
				List<Object> row = new LinkedList<>();
				if ( lhsType.equals("integer") && rhsType.equals("integer") ) {
					if (operator.equals("=")) {
						if (entry.value().get(indexOfColName) == rhs) { 
							for (int i = 0; i < pointers.size(); i++) { //build each row in a double for loop
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<>")) {
						if (entry.value().get(indexOfColName) != rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<") && !Objects.isNull(entry.value().get(indexOfColName)) && !Objects.isNull(rhs)) {
						if ((Integer)entry.value().get(indexOfColName) < (Integer)rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<=") && !Objects.isNull(entry.value().get(indexOfColName)) && !Objects.isNull(rhs)) {
						if ((Integer)entry.value().get(indexOfColName) <= (Integer)rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals(">") && !Objects.isNull(entry.value().get(indexOfColName)) && !Objects.isNull(rhs)) {
						if ((Integer)entry.value().get(indexOfColName) > (Integer)rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals(">=") && !Objects.isNull(entry.value().get(indexOfColName)) && !Objects.isNull(rhs)) {
						if ((Integer)entry.value().get(indexOfColName) >= (Integer)rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
				}
				
				else if ( lhsType.equals("string") && rhsType.equals("string") ) {
					if (operator.equals("=")) {
						if (entry.value().get(indexOfColName).equals(rhs)) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<>")) {
						if (!entry.value().get(indexOfColName).equals(rhs)) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<")) {
						if (((String) entry.value().get(indexOfColName)).compareTo((String) rhs) < 0) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<=")) {
						if (((String) entry.value().get(indexOfColName)).compareTo((String) rhs) <= 0) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals(">")) {
						if (((String) entry.value().get(indexOfColName)).compareTo((String) rhs) > 0) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals(">=")) {
						if (((String) entry.value().get(indexOfColName)).compareTo((String) rhs) >= 0) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
				}
				
				else if (lhsType.equals("boolean") && rhsType.equals("boolean")) {
					if (operator.equals("=")) {
						if (entry.value().get(indexOfColName) == rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else if (operator.equals("<>")) {
						if (entry.value().get(indexOfColName) != rhs) {
							for (int i = 0; i < pointers.size(); i++) {
								row.add(entry.value().get(pointers.get(i)) );
							}
							_select.state().put(row.get(resultPrimaryIndex), row);
							rowCount++;
						}
					}
					else return new Response(query, false, "invalid operation on types: boolean and boolean", null);
				}
				else if ((lhsType.equals("string") && rhsType.equals("integer")) || (lhsType.equals("integer") && rhsType.equals("string"))) {
					return new Response(query, false, "invalid condition/operation on types: string and integer", null);
				}
				else if ((lhsType.equals("integer") && rhsType.equals("boolean")) || (lhsType.equals("boolean") && rhsType.equals("integer"))) {
					return new Response(query, false, "invalid condition/operation on types: integer and boolean", null);
				}
				else if ((lhsType.equals("boolean") && rhsType.equals("string")) || (lhsType.equals("string") && rhsType.equals("boolean"))) {
					return new Response(query, false, "invalid condition/operation on types: boolean and string", null);
				}
				else if ((lhsType.equals("string") && rhsType.equals("null")) || (lhsType.equals("null") && rhsType.equals("string"))) {
					if (operator.equals("<>")) {
						for (int i = 0; i < pointers.size(); i++) {
							row.add(entry.value().get(pointers.get(i)) );
						}
						_select.state().put(row.get(resultPrimaryIndex), row);
						rowCount++;
					}
				}
				else if ((lhsType.equals("integer") && rhsType.equals("null")) || (lhsType.equals("null") && rhsType.equals("integer"))) {
					if (operator.equals("<>")) {
						for (int i = 0; i < pointers.size(); i++) {
							row.add(entry.value().get(pointers.get(i)) );
						}
						_select.state().put(row.get(resultPrimaryIndex), row);
						rowCount++;
					}
				}
				else if ((lhsType.equals("boolean") && rhsType.equals("null")) || (lhsType.equals("null") && rhsType.equals("boolean"))) {
					if (operator.equals("<>")) {
						for (int i = 0; i < pointers.size(); i++) {
							row.add(entry.value().get(pointers.get(i)) );
						}
						_select.state().put(row.get(resultPrimaryIndex), row);
						rowCount++;
					}
					else if (operator.equals("<") || operator.equals("<=") || operator.equals(">") || operator.equals(">=")) {
						return new Response(query, false, "invalid condition/operation on types: boolean and null and operator: <,<=,>,>=", null);
					}
				}
				else if (lhsType.equals("null") && rhsType.equals("null")) {
					if (operator.equals("<>")) {
						for (int i = 0; i < pointers.size(); i++) {
							row.add(entry.value().get(pointers.get(i)) );
						}
						_select.state().put(row.get(resultPrimaryIndex), row);
						rowCount++;
					}
				}
				
			}
		}
		
		
		return new Response(query, true, "Source table name: " +inputTableName+ ". # of rows selected: " +rowCount, _select);
	}
}
