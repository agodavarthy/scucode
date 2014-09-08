package OldCode;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
 
public class TestSort {
 
	public static void main(String[] args) {
 
		Map<String, Double> unsortMap = new HashMap<String, Double>();
		unsortMap.put("2", 0.234);
		unsortMap.put("1", 0.255);
		unsortMap.put("4", 0.57);
		unsortMap.put("3", 0.1254);
		unsortMap.put("7", 0.1254);
		unsortMap.put("5", 0.1254);
		unsortMap.put("6", 0.1254);
		unsortMap.put("8", 0.1254);
 
		System.out.println("Unsort Map......");
		printMap(unsortMap);
 
		System.out.println("Sorted Map......");
		Map<String, Double> sortedMap = sortByComparator(unsortMap);
		printMap(sortedMap);
 
	}
 
	private static Map sortByComparator(Map unsortMap) {
 
		List list = new LinkedList(unsortMap.entrySet());
 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
                                       .compareTo(((Map.Entry) (o1)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
 
	public static void printMap(Map<String, Double> sortedMap){
		for (Map.Entry entry : sortedMap.entrySet()) {
			System.out.println("Key : " + entry.getKey() 
                                   + " Value : " + entry.getValue());
		}
	}
}