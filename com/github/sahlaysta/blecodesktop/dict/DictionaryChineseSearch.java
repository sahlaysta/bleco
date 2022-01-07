package com.github.sahlaysta.bleco.dict;

import java.util.Map;

/* Bleco dictionary indexed searching
 * for searches in Chinese */
final class DictionaryChineseSearch
		extends DictionaryAbstractSearch<Byte> {
	
	//Constructor
	DictionaryChineseSearch(Map<Byte, Entry[]> indexMap) {
		super(indexMap);
	}
	
	
	//Chinese index map
	
	/* Chinese search indexing HashMap,
	 * indexed by the lowest byte of the Unicode
	 * code point of Chinese characters.
	 * No odd numbers as keys, so if the lowest byte
	 * is odd, subtract 1 */
	Entry[] getWithCodePoint(int stringCodePoint) {
		if (stringCodePoint % 2 != 0)
			stringCodePoint--;
		return indexMap.get((byte)((stringCodePoint)&0xFF));
	}
	@Override
	Entry[] getFromIndexMap(String search) {
		/* get the Entry[] from indexMap
		 * with the smallest candidate length */
		Entry[] result = null;
		int lowestSize = Integer.MAX_VALUE;
		
		//iterate string codepoints
		for (int i = 0; i < search.length(); ) {
			int codePoint = search.codePointAt(i);
			Entry[] entries = getWithCodePoint(codePoint);
			if (codePoint <= 9000) { //default / non-chinese letter
				if (entries != null && lowestSize == Integer.MAX_VALUE) {
					result = entries;
					lowestSize--;
				}
			}
			else if (entries != null && entries.length <= lowestSize) {
				result = entries;
				lowestSize = entries.length;
			}
			i += Character.charCount(codePoint);
		}
		return result;
	}
	

	
	//Chinese indexed searching
	@Override
	SearchResult searchMatches(Entry entry, String search) {
		/* Compare the Entry's simplified and traditional
		 * Chinese to the search  */
		
		int len = search.length(),
			entrylen = entry.simplified
				.codePointCount(0, entry.simplified.length()),
		
			searchI = 0, searchCodePoint,
			smplI = 0, smplCodePoint,
			tradI = 0, tradCodePoint,
			syllableCount = 0;
		
		while (searchI < len) {
			if (smplI >= entrylen)
				return null;
			searchCodePoint = search.codePointAt(searchI);
			smplCodePoint = entry.simplified.codePointAt(smplI);
			tradCodePoint = entry.traditional.codePointAt(tradI);
			
			//wildcard
			if (searchCodePoint == '*') {
				searchI++;
				smplI += Character.charCount(smplCodePoint);
				tradI += Character.charCount(tradCodePoint);
				syllableCount++;
				continue;
			//skip apostrophe
			} else if (searchCodePoint == '\'') {
				searchI++;
				continue;
			}
			
			/* do 'pinyin between chinese' searching
			 * e.g. 安人 (anren) matches the search term "安ren" */
			if (searchCodePoint <= 9000 //not Chinese character
				&& smplCodePoint > 9000
				&& tradCodePoint > 9000) {
				String pinyin = entry.pinyin;
				
				/* get nth syllable range in entry pinyin
				 * 
				 * e.g. the pinyin string "min2 fa3 dian3"
				 * lets say the 2nd syllable, "fa3" */
				int pinyinRangeStart = 0,
					pinyinRangeEnd = pinyin.length(),
					spaceCount = 0;
				for (int i = 0; i < pinyinRangeEnd; i++) {
					if (pinyin.charAt(i) == ' ') {
						spaceCount++;
						if (spaceCount == syllableCount) {
							pinyinRangeStart = i + 1;
						} else if (spaceCount == syllableCount + 1) {
							pinyinRangeEnd = i;
							break;
						}
					}
				}

				//match pinyin with search (tone optional)
				e: for (int i = 0;
						i < pinyinRangeEnd - pinyinRangeStart;
						i++) {
					if (len <= searchI + i)
						break;
					char searchC = search.charAt(searchI + i),
						pinyinC = pinyin.charAt(i + pinyinRangeStart);
					
					//ending space optional
					if (searchC == '\'') {
						switch (pinyinC) {
						case '1': case '2': case '3':
						case '4': case '5':
							break e;
						}
					}
					
					//check if last char in range is a tone
					if (i == pinyinRangeEnd - pinyinRangeStart - 1) {
						switch (pinyinC) {
						case '1': case '2': case '3':
						case '4': case '5':
							if (searchC == pinyinC)
								break e;
							searchI--;
							break e;
						}
					}
					
					if (!charsEqual(searchC, pinyinC))
						return null;
				}
				
				searchI += pinyinRangeEnd - pinyinRangeStart;
				smplI += Character.charCount(smplCodePoint);
				tradI += Character.charCount(tradCodePoint);
				syllableCount++;
				continue;
				
			/* Check non-chinese characters */
			} else if (searchCodePoint <= 9000) {
				char searchC = (char)searchCodePoint,
					smplC = (char)smplCodePoint;
				if (!charsEqual(searchC, smplC))
					return null;
				searchI++;
				smplI++;
				tradI++;
				syllableCount++;
				continue;
			}
			
			
			if (searchCodePoint != smplCodePoint
				&& searchCodePoint != tradCodePoint)
				return null;
			searchI += Character.charCount(searchCodePoint);
			smplI += Character.charCount(smplCodePoint);
			tradI += Character.charCount(tradCodePoint);
			syllableCount++;
		}
		
		return
			new SearchResult(
				entry,
				SearchResult.CHINESE_SEARCH,
				false,
				false,
				-1,
				-1,
				-1);
	}
}