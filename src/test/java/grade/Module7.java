package grade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

import types.Entry;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Module7 {
	public static final int MAP_OPERATIONS_PER_DESCRIPTOR = 500;
	public static final int RANDOM_SEED = 2020_08;

	private static int passed;
	private static int hash;
	private static types.HashMap<Object, List<Object>> subject;
	private static java.util.AbstractMap<Object, List<Object>> exemplar;
	private static List<Path> paths;
	private static java.util.Random RNG;

	@BeforeAll
	public static final void setup() {
		passed = 0;

		hash = 0;
		subject = null;
		exemplar = null;

		RNG = new Random(RANDOM_SEED);

		paths = new LinkedList<>();
	}

	@TestFactory
    @DisplayName("Audits [Required]")
    @Order(0)
    public final Stream<DynamicTest> audits() throws IllegalArgumentException, IllegalAccessException {
		return Stream.of(
			dynamicTest("Data Folder", () -> {
				try {
					if (Files.notExists(Paths.get("data")))
						Files.createDirectory(Paths.get("data"));
					if (Files.notExists(Paths.get("data", "grade")))
						Files.createDirectory(Paths.get("data", "grade"));
					else
						Files.walk(Paths.get("data", "grade"))
							.skip(1)
							.sorted(Comparator.reverseOrder())
							.forEach(path -> {
								try {
									Files.delete(path);
								}
								catch (IOException e) {
									subject = null;
									fail("Unable to empty data folder", e);
								}
							});

					var path = Paths.get("data", "grade", "m7_audit.bin");
					subject = new types.HashFile(path, descriptors.get(0));
				}
				catch (Exception e) {
					subject = null;
					fail("Inaccessible data folder", e);
				}
			})
    	);
	}

	private static final List<Entry<Integer, List<String>>> descriptors = List.of(
		new Entry<>(
			0,
			List.of("string", "integer", "boolean")
		),
		new Entry<>(
			4,
			List.of("integer", "boolean", "boolean", "integer", "integer", "boolean")
		),
		new Entry<>(
			8,
			List.of("string", "integer", "integer", "boolean", "boolean", "string", "integer", "boolean", "string")
		)
	);
	@TestFactory
	@DisplayName("Create Map [descriptor=Sib]")
	@Order(1)
	public final Stream<DynamicTest> create0() {
		return testDescriptor(0, descriptors.get(0), false);
	}

	@TestFactory
	@DisplayName("Reopen Map [descriptor=Sib]")
	@Order(2)
	public final Stream<DynamicTest> reopen0() {
		return testDescriptor(0, descriptors.get(0), true);
	}

	@TestFactory
	@DisplayName("Create Map [descriptor=ibbiIb]")
	@Order(3)
	public final Stream<DynamicTest> create1() {
		return testDescriptor(1, descriptors.get(1), false);
	}

	@TestFactory
	@DisplayName("Reopen Map [descriptor=ibbiIb]")
	@Order(4)
	public final Stream<DynamicTest> reopen1() {
		return testDescriptor(1, descriptors.get(1), true);
	}

	@TestFactory
	@DisplayName("Create Map [descriptor=siibbsibS]")
	@Order(5)
	public final Stream<DynamicTest> create2() {
		return testDescriptor(2, descriptors.get(2), false);
	}

	@TestFactory
	@DisplayName("Reopen Map [descriptor=siibbsibS]")
	@Order(6)
	public final Stream<DynamicTest> reopen2() {
		return testDescriptor(2, descriptors.get(2), true);
	}

	public final Stream<DynamicTest> testDescriptor(int number, Entry<Integer, List<String>> descriptor, boolean reopen) {
		if (subject == null)
			fail("Must pass audits before descriptor");

		var path = Paths.get("data", "grade", "m7_map" + number + ".bin");
		paths.add(path);

		try {
			subject = new types.HashFile(path, descriptor);
		}
		catch (Exception e) {
			fail("Unexpected constructor exception", e);
		}
		if (!reopen)
			exemplar = new java.util.HashMap<>();

		hash = 0;
		return RNG.doubles(MAP_OPERATIONS_PER_DESCRIPTOR/2).mapToObj(p -> {
			if (!reopen && p < 0.70)		return testPut(descriptor);
			else if (!reopen && p < 0.90)	return testRemove(descriptor);
			else  			   				return testGet(descriptor);
		});
	}

	private static final DynamicTest testPut(Entry<Integer, List<String>> descriptor) {
		final var record = record(descriptor);
		final var call = String.format("put(\"%s\", %s)", record.key(), factory(record.value()));

		return dynamicTest(title(call, record.key()), () -> {
			if (exemplar.containsKey(record.key()))
				hash -= new Entry<>(record.key(), exemplar.get(record.key())).hashCode();
			hash += new Entry<>(record.key(), record.value()).hashCode();

			assertEquals(
				exemplar.put(record.key(), record.value()),
				subject.put(record.key(), record.value()),
				String.format("%s must yield correct result", call)
			);

			thenTestSize(call);
			thenTestHashCode(call);

			passed++;
		});
	}

	private static final DynamicTest testRemove(Entry<Integer, List<String>> descriptor) {
		final var record = record(descriptor);
		final var call = String.format("remove(\"%s\")", record.key());

		return dynamicTest(title(call, record.key()), () -> {
			if (exemplar.containsKey(record.key()))
				hash -= new Entry<>(record.key(), exemplar.get(record.key())).hashCode();

			assertEquals(
				exemplar.remove(record.key()),
				subject.remove(record.key()),
				String.format("%s must yield correct result", call)
			);

			thenTestSize(call);
			thenTestHashCode(call);

			passed++;
		});
	}

	private static final DynamicTest testGet(Entry<Integer, List<String>> descriptor) {
		final var record = record(descriptor);
		final var call = String.format("get(\"%s\")", record.key());

		return dynamicTest(title(call, record.key()), () -> {
			assertEquals(
				exemplar.get(record.key()),
				subject.get(record.key()),
				String.format("%s must yield correct result", call)
			);

			thenTestSize(call);
			thenTestContains(record.key(), call);

			passed++;
		});
	}

	private static final void thenTestContains(Object key, String after) {
		final var call = String.format("contains(\"%s\")", key);

		assertEquals(
			exemplar.containsKey(key),
			subject.contains(key),
			String.format("after %s, %s must yield correct result", after, call)
		);
	}

	private static final void thenTestSize(String after) {
		final var call = "size()";

		assertEquals(
			exemplar.size(),
			subject.size(),
			String.format("after %s, %s must yield correct result", after, call)
		);

		thenTestIsEmpty(call);
	}

	private static final void thenTestIsEmpty(String after) {
		final var call = "isEmpty()";

		assertEquals(
			exemplar.isEmpty(),
			subject.isEmpty(),
			String.format("after %s, %s must yield correct result", after, call)
		);
	}

	private static final void thenTestHashCode(String after) {
		final var call = "hashCode()";

		assertEquals(
			hash,
			subject.hashCode(),
			String.format("after %s, %s must yield correct result", after, call)
		);
	}

	@AfterAll
	public static final void report() {
		System.out.printf(
			"[M7 PASSED %d%% OF BATTERY TESTS]\n",
			(int) Math.ceil(passed / (double) (MAP_OPERATIONS_PER_DESCRIPTOR * 3) * 100)
		);
	}

	private static final Entry<Object, List<Object>> record(Entry<Integer, List<String>> descriptor) {
		final var list = new LinkedList<>();
		for (var i = 0; i < descriptor.value().size(); i++) {
			if (i != descriptor.key() && RNG.nextDouble() < 0.1)
				list.add(null);
			else
				list.add(switch (descriptor.value().get(i)) {
					case "string" -> Integer.toString((int) (Math.pow(RNG.nextGaussian(), 2) * Math.pow(16, 2)), 16);
					case "integer" -> (int) (Math.pow(RNG.nextGaussian(), 2) * 1000);
					case "boolean" -> RNG.nextBoolean();
					default -> null;
				});
		}
		return new Entry<>(list.get(descriptor.key()), list);
	}

	private static final String factory(List<Object> list) {
		final var sb = new StringBuilder("[");
		for (var i = 0; i < list.size(); i++) {
			var elem = list.get(i);
			if (i > 0)
				sb.append(", ");
			sb.append(elem instanceof String ? "\"" + elem + "\"" : elem);
		}
		sb.append("]");
		return sb.toString();
	}

	private static final String title(String call, Object key) {
		return String.format(
			"%s [%s, m=%d, \u03B1=%.3f]",
			call,
			exemplar.containsKey(key) ? "hit" : "miss",
			subject.size(),
			subject.loadFactor()
		);
	}
}