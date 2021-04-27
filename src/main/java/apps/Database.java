package apps;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import drivers.CreateTable;
import drivers.Driver;
import drivers.DropTable;
import drivers.DumpTable;
import drivers.Echo;
import drivers.Export;
import drivers.Import;
import drivers.InsertInto;
import drivers.Range;
import drivers.Select;
import drivers.ShowTables;
import drivers.SquaresBelow;
import drivers.Unrecognized;
import types.Map;
import types.Response;
import types.SearchList;
import types.Table;

/**
 * This class implements a
 * database management system.
 * <p>
 * Additional protocols may be added.
 */
public class Database implements Closeable {
	private final Map<String, Table> tables;
	private final List<Driver> drivers;

	/**
	 * Initialize the tables and drivers.
	 * <p>
	 * Do not modify the protocol.
	 */
	public Database() {
		this.tables = new SearchList<>();

		this.drivers = List.of( //factory method that creates and adds to a list
			new Echo(),
			new Range(),
			new SquaresBelow(),
			new CreateTable(),
			new DropTable(),
			new ShowTables(),
			new DumpTable(),
			new InsertInto(),
			new Select(),
			new Export(),
			new Import(),
			new Unrecognized()
		);
	}

	/**
	 * Returns the tables of this database.
	 * <p>
	 * Do not modify the protocol.
	 *
	 * @return the tables.
	 */
	public Map<String, Table> tables() {
		return tables;
	}

	/**
	 * Interprets a list of queries and returns a list
	 * of responses to each query in sequence.
	 * <p>
	 * Do not modify the protocol.
	 *
	 * @param queries the list of queries.
	 * @return the list of responses.
	 */
	public List<Response> interpret(List<String> queries) {
		List<Response> responses = new LinkedList<>(); //initialize empty list of responses

		for (int i = 0; i < queries.size(); i++) {
			for (int j = 0; j < drivers.size(); j++) {
				Response r = drivers.get(j).execute(queries.get(i), this);
				if (r != null) {
					responses.add(r);
					break;
				}
			}
		}

		return responses;
	}

	/**
	 * Execute any required tasks when
	 * the database is closed.
	 * <p>
	 * Do not modify the protocol.
	 */
	@Override
	public void close() throws IOException {

	}
}
