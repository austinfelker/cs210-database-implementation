package apps;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import types.Entry;
import types.Response;
import types.Table;

/** 
 * Implements a user console for
 * interacting with a database.
 * <p>
 * Additional protocols may be added.
 */
public class Console {
	/**
	 * The entry point for execution
	 * with user input/output.
	 */
	public static void main(String[] args) {
		try (
			final Database db = new Database();
			final Scanner in = new Scanner(System.in);
			final PrintStream out = System.out;
		) 
		{
			while (true) {
				out.print(">> ");

				//store the user's input line into a String text. breaks the loop if EXIT
				String text = in.nextLine(); 
				if (text.equalsIgnoreCase("exit")) {
					break;
				}

				//split the text with ; and store individually into queries List
				List<String> queries = Arrays.asList(text.split(";"));

				//checks the queries before adding them to the response list
				List<Response> responses = db.interpret(queries); 

				for (int i = 0; i < responses.size(); i++) {
					out.println("Query:   " + responses.get(i).query());
					out.println("Success: " + responses.get(i).success());
					out.println("Message: " + responses.get(i).message());

					if (responses.get(i).table() != null)
						out.println(prettyPrintTable(responses.get(i).table()));
				}

			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Module 4: pretty-print table
	 * @param table
	 * @return the string containing the whole table
	 */
	@SuppressWarnings("unchecked")
	private static String prettyPrintTable(Table table) {
		List<String> listOfColNames = (List<String>) table.schema().get("column_names");
		List<String> listOfColTypes = (List<String>) table.schema().get("column_types");
		int colPadding = 16;
		int widthOfTable = colPadding*listOfColNames.size();
		
		
		StringBuilder sb = new StringBuilder(); // use a StringBuilder object to concatenate because it saves space/time complexity
		
		String header = "(" + table.schema().get("table_name") + ")";
		int primaryIndex = (int) table.schema().get("primary_index");
		
		sb.append(header + "_".repeat(widthOfTable - header.length()+1) + "\n"); ///m2_table1\_________________________________________
		
		/*
		 * 	for each column name in the table, \
		 * 		check the column for if it is the primary column or not
		 * 			the primary column needs to have an * 
		 * 			add the table to the StringBuilder
		 */
		for (int i = 0; i < listOfColNames.size(); i++) {
			if (i == primaryIndex && listOfColTypes.get(i).equals("integer")) {
				sb.append("|");
				//this edge case for right allignment of integer requires 
				// the pipe to be displayed first, and the colpadding has -1 to compensate
				sb.append(String.format("%" + (colPadding-1) + "s", listOfColNames.get(i)+ "*"));
			}
			else if (listOfColTypes.get(i).equals("integer")) {
				sb.append("|");
				//this edge case for right allignment of integer requires 
				// the pipe to be displayed first, and the colpadding has -1 to compensate
				sb.append(String.format("%" + (colPadding-1) + "s", listOfColNames.get(i)));
			}
			else if (i == primaryIndex) {
				sb.append(String.format("%" + -colPadding + "s", "|"+listOfColNames.get(i)+ "*") );
			}
			else {
				sb.append(String.format("%" + -colPadding + "s", "|"+listOfColNames.get(i)));
			}
			
		}
		sb.append("|\n");
		
		//headers seperated with |--------|
		sb.append("|" + "-".repeat(widthOfTable-1)+ "|" + "\n");
		
		/*
		 * 	for each row in the table, 
		 * 		check the type of each index and add the correct element to the StringBuilder if 
		 * 			(nulls need to be blank and strings need to have quotations)
		 */
		for (Entry<Object, List<Object>> entry : table.state()) { 
			// create the row in a list form
			List<Object> row = entry.value(); 
			for (int i = 0; i < row.size(); i++) {
				if (listOfColTypes.get(i).equals("string") && row.get(i) != null) {
					if ( ((String) row.get(i)).length() <= colPadding-3 ) {
						sb.append(String.format("%" + -colPadding + "s", "|\""+row.get(i)+ "\"" ) );
					}
					else {
						String truncated = ( (String) row.get(i)).substring( 0, colPadding-5);
						sb.append(String.format("%" + -colPadding + "s", "|\""+truncated+ "...") );
					}
				}
				else if (listOfColTypes.get(i).equals("integer") && row.get(i) != null) {
					sb.append("|");
					//this edge case for right allignment of integer requires 
					// the pipe to be displayed first, and the colpadding has -1 to compensate
					sb.append(String.format("%" + (colPadding-1) + "s", row.get(i)));
				}
				else if (row.get(i) == null) {
					sb.append(String.format("%" + -colPadding + "s", "|"));
				}
				else
					sb.append(String.format("%" + -colPadding + "s", "|" + row.get(i)));

			}
			sb.append("|" + "\n");
		}
		
		//bottom of table marked with |__________|
		sb.append("|" + "_".repeat(widthOfTable-1) + "|");
		
		return sb.toString();
	}
}

















