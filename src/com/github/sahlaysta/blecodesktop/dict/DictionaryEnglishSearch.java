package com.github.sahlaysta.bleco.dict;

import java.util.List;
import java.util.Map;

final class DictionaryEnglishSearch
		extends DictionaryAbstractSearch<String> {

	//Constructor
	DictionaryEnglishSearch(Map<String, Entry[]> indexMap) {
		super(indexMap);
	}

	
	//English index map
	@Override
	Entry[] getFromIndexMap(String str) {
		int spaceIndex = str.indexOf(' ');
		if (spaceIndex != -1)
			str = str.substring(0, spaceIndex);
		if (str.isEmpty())
			return null;
		return
			str.length() > 3
				? indexMap.get(str.substring(0, 3))
				: indexMap.get(str);
	}
	
	
	//Override for search format
	@Override
	List<SearchResult> search(String search) {
		return super.search(formatEngSearch(search));
	}
	private static String formatEngSearch(String search) {
		String str = removeDiacritics(search)
			.toLowerCase();
		
		/* remove all symbols and
		 * remove adjacent spaces */
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0, len = str.length(); i < len; i++) {
			int codePoint = str.codePointAt(i);
			int charCount = Character.charCount(codePoint);
			int sblen = sb.length();
			
			//remove symbols
			if (codePoint >= 127) {
				i += charCount - 1;
				continue;
			}
			char ch = (char)codePoint;
			if ((ch >= 'a' && ch <= 'z')
					|| (ch >= '0' && ch <= '9')) {
				sb.append(ch);
				continue;
			}

			//no leading / adjacent spaces
			if (ch == ' ') {
				if (sblen == 0 || sb.charAt(sblen - 1) == ' ')
					continue;
				sb.append(' ');
			}
		}
		
		//remove trailing spaces
		while (sb.length() > 0 &&
				sb.charAt(sb.length() - 1) == ' ')
			sb.setLength(sb.length() - 1);
		
		return sb.toString();
	}
	private static String removeDiacritics(String str) {
		if (str == null)
			return null;
		if (str.isEmpty())
			return "";
		
		int len = str.length();
		StringBuilder sb
			= new StringBuilder(len);
		
		//iterate string codepoints
		for (int i = 0; i < len; ) {
			int codePoint = str.codePointAt(i);
			int charCount
				= Character.charCount(codePoint);
			
			if (charCount > 1) {
				for (int j = 0; j < charCount; j++)
					sb.append(str.charAt(i + j));
				i += charCount;
				continue;
			}
			else if (codePoint <= 127) {
				sb.append((char)codePoint);
				i++;
				continue;
			}
			
			sb.append(
				java.text.Normalizer
					.normalize(
						Character.toString((char)codePoint),
						java.text.Normalizer.Form.NFD)
							.charAt(0));
			i++;
		}
		
		return sb.toString();
	}
	

	//English searching
	@Override
	SearchResult searchMatches(Entry entry, String search) {
		SearchResult result = null;
		String[] defs = entry.normalizedDefinitions;
		e: for (int i = 0; i < defs.length; i++) {
			String def = defs[i];
			
			/* Check for the contained word(s)
			 * of the search
			 * inside the definition string
			 * starting at the space ' ' of
			 * every word in the definition */
			
			int defLen = def.length(), defI = 0,
				searchLen = search.length(), searchI = 0,
				beginIndex = 0;
			boolean prevCharIsSpace = false;
			boolean prevIsBegin = true;
			e2: while (true) {
				if (searchI >= searchLen)
					break;
				if (defI < 0 || defI >= defLen)
					continue e;//no match
				
				char defC = def.charAt(defI);
				char searchC = search.charAt(searchI);
				
				//skip symbols, match search space to symbol
				if (defC == '.') {
					if (searchC == ' ') {
						prevCharIsSpace = true;
						searchI++;
					}
					if (prevIsBegin)
						beginIndex++;
					defI++;
					continue;
				}
				
				//skip consecutive spaces
				if (prevCharIsSpace && defC == ' ') {
					defI++;
					continue;
				}
				prevCharIsSpace = defC == ' ';
				
				//compare chars
				if (defC != searchC) {
					//go to next word
					for (int j = defI + 1; j < defLen; j++) {
						char c = def.charAt(j);
						if (c == ' ' || c == '.') {
							defI = j + 1;
							searchI = 0;
							beginIndex = j + 1;
							prevIsBegin = true;
							continue e2;
						}
					}
					continue e;
				}
				
				prevIsBegin = false;
				defI++;
				searchI++;
			}

			if (prevIsBegin)
				beginIndex++;
				
			//determine full match
			boolean fullMatch = false;
			if (defI >= defLen) {
				fullMatch = true;
				defI--;//offset end index
			} else {
				char lastChar = def.charAt(defI);
				if (lastChar == ' ' || lastChar == '.')
					fullMatch = true;
				defI--;
			}
			
			result
				= new SearchResult(
					entry,
					SearchResult.ENGLISH_SEARCH,
					fullMatch,
					false,
					i,
					beginIndex,
					defI);
			if (fullMatch)
				return result;
		}
		return result;
	}
}