package types;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Implements a hash table-based map
 * using a random access file structure.
 */
public class HashFile implements HashMap<Object, List<Object>> {
	/*
	 * TODO: For Module 7, implement the stubs.
	 *
	 * Until then, this class is unused.
	 */
	public HashFile(Path path, Entry<Integer, List<String>> descriptor) {

	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Object> put(Object key, List<Object> value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double loadFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Entry<Object, List<Object>>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashFunction(String key) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int hashFunction(Object key) {
		// TODO Auto-generated method stub
		return 0;
	}
}
