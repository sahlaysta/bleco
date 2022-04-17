package sahlaysta.bleco.dict;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//Abstract class for dictionary indexing
abstract class DictionaryAbstractSearch<T> {
	
	//unicode range start for Chinese characters
	static final int UNICODE_CHINESE_START_RANGE = 127;
	
	
	//Index map
	final Map<T, Entry[]> indexMap;
	DictionaryAbstractSearch(Map<T, Entry[]> indexMap) {
		this.indexMap = indexMap;
	}
	 
	abstract Entry[] getFromIndexMap(String search);
	
	
	//Indexed searching
	List<SearchResult> search(String search) {
		List<SearchResult> result = new ArrayList<>();
		int top = 0;
		
		Entry[] ind = getFromIndexMap(search);
		if (ind == null)
			return null;
	
		for (Entry entry: ind) {
			SearchResult sr = searchMatches(entry, search);
			if (sr != null) {
				if (sr.isFullMatch)
					result.add(top++, sr);
				else
					result.add(sr);
			}
		}
		
		return result.size() == 0 ? null : result;
	}
	
	abstract SearchResult searchMatches(Entry entry, String search);
	
	
	
	//Formats the input string pre-search
	static String formatSearch(String search) {
		search = search.toLowerCase();
		StringBuilder sb = new StringBuilder(search.length());
		char ch;
		e: for (int i = 0; i < search.length(); i++) {
			switch (ch = search.charAt(i)) {
			
			//v and ü to u:
			case 'v': case 'ü':
				sb.append("u:");
				continue;
				
			//(important)	
			//replaces all spaces with apostrophes
			//and removes any adjacent apostrophes
			//and removes any apostrophes after tones
			case '\'': case ' ':
				if (i == 0)
					break;
				switch (search.charAt(i - 1)) {
					case '1': case '2': case '3': case '4':
					case '5': case '\'': case ' ':
						continue e;
				}
				sb.append('\'');
				continue;
			
			//remove any apostrophe after a tone
			case '1': case '2': case '3':
			case '4': case '5':
				int len = sb.length();
				if (len == 0)
					break;
				switch (sb.charAt(len - 1)) {
				case '\'':
					sb.deleteCharAt(len - 1);
					sb.append(ch);
					continue e;
				}
				
			}
			sb.append(ch);
		}
		
		//final measures
		if (sb.length() == 0) //empty string
			return "";
		if (sb.charAt(0) == ' ' || //remove leading space
			sb.charAt(0) == '\'')
			sb.deleteCharAt(0);
		if (sb.length() > 0 && //remove trailing space
			sb.charAt(sb.length() - 1) == '\'')
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	
	//chars equal case insensitive
	static boolean charsEqual(
			char searchC, char pinyinC) {
		return searchC == pinyinC ||
			searchC == Character.toLowerCase(pinyinC);
	}
}