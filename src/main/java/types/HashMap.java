package types;

/**
 * Defines the protocols for a hash table-based map.
 * <p>
 * Do not modify the protocols.
 */
public interface HashMap<K, V> extends Map<K, V> {
	/**
	 * Returns the load factor (alpha), defined
	 * to be the number of entries in the map
	 * divided by the length of the hash table.
	 *
	 * @return the load factor.
	 */
	double loadFactor();

	/**
	 * Returns a hash code for the given string key.
	 * <p>
	 * The hash code is computed from the characters
	 * of the key string using an original algorithm.
	 *
	 * @param a string key.
	 * @return the hash code for the key string.
	 */
	int hashFunction(String key);

	/**
	 * Returns a hash code for the given non-string key.
	 * <p>
	 * The hash code equals the {@link Object#hashCode()}
	 * for the key object.
	 *
	 * @param a non-string key.
	 * @return the hash code for the key object.
	 */
	int hashFunction(Object key);

	/**
	 * Returns a hash code for the given string key.
	 * <p>
	 * The hash code is computed from the characters
	 * of the key string using an original algorithm.
	 * <p>
	 * It should be unlikely that {@link #hashFunction(String)}
	 * equals {@link #secondHashFunction(String)} for the same key.
	 * <p>
	 * This method is optional. Only override it if
	 * the collision resolution technique uses it.
	 *
	 * @param a string key.
	 * @return the hash code for the key string.
	 */
	default int secondHashFunction(String key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a hash code for the given non-string key.
	 * <p>
	 * The hash code equals <code>p</code> minus the
	 * {@link Object#hashCode()} for the key object modulo
	 * <code>p</code>, where <code>p</code> is some prime
	 * number less than the length of the hash table.
	 * <p>
	 * This method is optional. Only override it if
	 * the collision resolution technique uses it.
	 *
	 * @param a non-string key.
	 * @return the hash code for the key object.
	 */
	default int secondHashFunction(Object key) {
		throw new UnsupportedOperationException();
	}
}