package types;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Implements a hash table-based map
 * using an array data structure.
 */
public class HashArray<K, V> implements HashMap<K, V> {
	

	private Object[][] array;
	private int size;
	/*
	 * TODO: For Module 6, implement the stubs.
	 *
	 * Until then, this class is unused.
	 */
	public HashArray() {
		array = new Object[1000][5];
		size = 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		Arrays.fill(array, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		int hash = hashFunction(key);
		int flagIndex = -1;
		
		for (int i = 0; i < array[hash].length; i++) { //for each col in the hashCode row
			//if the slot is null, save the first index to the flagindex
			if (array[hash][i] == null && flagIndex == -1) {
				flagIndex = i;
			}
			
			Entry<K, V> entry = (Entry<K, V>) array[hash][i];

			//if key name same. UPDATE
			if (entry != null && key.equals(entry.key())) {
				array[hash][i] = new Entry<>(key, value);
				return entry.value();
			}
			
			//else the key not match and we move to the next index to check
		}
		
		array[hash][flagIndex] = new Entry<>(key, value);
		size++;
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(K key) {
		int hash = hashFunction(key);
		
		for (int i = 0; i < array[hash].length; i++) { //for each col in the hashCode row
			Entry<K, V> entry = (Entry<K, V>) array[hash][i];

			//if key name same. HIT
			if (entry != null && key.equals(entry.key())) 
				return entry.value();
		}
		//else there was not a match to the key
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(K key) {
		int hash = hashFunction(key);
		for (int i = 0; i < array[hash].length; i++) {
			Entry<K, V> entry = (Entry<K, V>) array[hash][i];
			
			//if key name same. REMOVE
			if (entry != null && key.equals(entry.key())) {
				array[hash][i] = null;
				size--;
				return entry.value();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(K key) {
		int hash = hashFunction(key);
		
		for (int i = 0; i < array[hash].length; i++) { //for each col in the hashCode row
			Entry<K, V> entry = (Entry<K, V>) array[hash][i];

			//if key name same. HIT
			if (entry != null && key.equals(entry.key())) 
				return true;
		}
		//else there was not a match to the key
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return (size == 0);
	}

	@Override
	public double loadFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}


	@SuppressWarnings("unchecked")
	@Override
	public int hashCode() {
		int total = 0;
		//for each index in the 2d array do the hashFunction method and add it to the total
		for (Object[] innerArray: array) {
			for (Object object :  innerArray) { 
				if (object != null) {
					Entry<K, V> entry = (Entry<K, V>) object;
					total += entry.hashCode();
				}
			}
		}
		return total;
	}
	
	@Override
	public int hashFunction(String key) {
		int sum = 0;
		char[] charArray = key.toCharArray();
		
		for (int i = 0; i < charArray.length; i++) {
			int charNumericLetter = charArray[i] + 32; //find the number value of the char
			
			//example ((((1*26)*17+2*26)*17+3*26)*17+4*26)*17
			charNumericLetter *= i+1; //multiply by each letter position
			sum += charNumericLetter; //
			sum *= 17;
		}
		
		sum %= 1000;
		return sum;
	}

	@Override
	public int hashFunction(Object key) {
		return hashFunction((String) key);
	}
	
	@Override
	public String toString() {
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		return false;
	}
}
