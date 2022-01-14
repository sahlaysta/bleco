package com.github.sahlaysta.bleco.dict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* Bleco dictionary indexed searching
 * for searches in pinyin */
final class DictionaryPinyinSearch
		extends DictionaryAbstractSearch<String> {
	
	//Pinyin index map
	
	/* Pinyin search indexing map,
	 * indexed by the first two letters
	 * of the pinyin pronunciation
	 * of each entry */
	@Override
	Entry[] getFromIndexMap(String pinyin) {
		return indexMap.get(
				pinyin.substring(0, 2).replace("'", " "));
	}
	DictionaryPinyinSearch(Map<String, Entry[]> indexMap) {
		super(indexMap);
	}
	

	//Pinyin indexed searching
	@Override
	List<SearchResult> search(String search) {
		return
			search.length() == 1
				? singleLetterPinyinSearch(search.charAt(0))
				: super.search(search);
	}
	private List<SearchResult> singleLetterPinyinSearch(char ch) {
		List<String> strs = new ArrayList<>();
		for (String s: indexMap.keySet())
			if (s.charAt(0) == ch)
				strs.add(s);
		if (strs.size() == 0)
			return null;
		Collections.sort(strs);
		List<SearchResult> result = new ArrayList<>();
		for (String s: strs)
			for (Entry e: indexMap.get(s))
				if (e.pinyin.indexOf(' ') == -1) //only single-syllable
					result.add(
						new SearchResult(
							e,
							SearchResult.PINYIN_SEARCH,
							false,
							false,
							-1,
							-1,
							-1));
		return result.size() == 0 ? null : result;
	}
	@Override
	SearchResult searchMatches(Entry entry, String search) {
		String pinyin = entry.pinyin;
		/* Compare the pinyin with the search
		 * return -1: no match
		 * return 0: match
		 * return 1: full-length match */
		
		int pinyinI = 0, pinyinLength = pinyin.length(),
				searchI = 0, searchLength = search.length();
		while (searchI < searchLength) {
			
			if (searchI >= pinyinLength
				|| pinyinI >= pinyinLength)
				return null;
			
			switch (search.charAt(searchI)) {
			
			//pinyin tone match, optional
			case '1': case '2': case '3': case '4': case '5':
				if (search.charAt(searchI) == pinyin.charAt(pinyinI)) {
					searchI++;
					pinyinI++;
					continue;
				}
				break;
				
			//apostrophe matches space in pinyin
			case '\'':
				if ((pinyin.charAt(pinyinI) == ' ' ||
						(pinyinLength > (pinyinI + 1)
						&& pinyin.charAt(pinyinI + 1) == ' '))) {
					pinyinI++;
					searchI++;
					continue;
				}
				break;
				
			//u alone cannot match u:
			case 'u':
				if (pinyinLength > (pinyinI + 1)
					&& pinyin.charAt(pinyinI + 1) == ':'
					&& (searchLength <= (searchI + 1)
					|| search.charAt(searchI + 1) != ':'))
					return null;
			}
			
			
			/* skips spaces / pinyin tones for
			 * toneless / spaceless searching */
			e: while (true) {
				switch (pinyin.charAt(pinyinI)) {
				case ' ': case '1': case '2':
				case '3': case '4': case '5':
					if (++pinyinI >= pinyinLength)
						return null;
					break;
				default: break e;
				}
			}
			
			
			if (!charsEqual(search.charAt(searchI), pinyin.charAt(pinyinI)))
				return null;
			pinyinI++;
			searchI++;
		}
		
		//check full-length pinyin match
		if (pinyinI == pinyinLength) {
			return
				new SearchResult(
					entry,
					SearchResult.PINYIN_SEARCH,
					true,
					false,
					-1,
					-1,
					-1);
		} else if (pinyinI == pinyinLength - 1) {
			switch (pinyin.charAt(pinyinLength - 1)) {
			case '1': case '2': case '3': case '4': case '5':
				return
					new SearchResult(
						entry,
						SearchResult.PINYIN_SEARCH,
						true,
						false,
						-1,
						-1,
						-1);
			}
		}
		return
			new SearchResult(
				entry,
				SearchResult.PINYIN_SEARCH,
				false,
				false,
				-1,
				-1,
				-1);
	}
}