package com.github.sahlaysta.bleco.dict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Helper class for chinese sentence splitting
final class DictionarySentenceHelper {
	
	//Constructor
	final Dictionary dict;
	DictionarySentenceHelper(Dictionary dict) {
		this.dict = dict;
	}
	
	//split chinese sentence into words
	List<SearchResult> splitChineseSentence(String search) {
		String str = search.toLowerCase();
		/* Returns the entries of all the words
		 * in the Chinese sentence
		 * e.g. the sentence "我们会法语" 
		 * split into the words "我们", "会", "法语"
		 * */
		
		List<SearchResult> result = new ArrayList<>();
		//iterate string codepoints
		for (int i = 0, len = str.length(); i < len; ) {
			int codePoint = str.codePointAt(i);
			int charCount = Character.charCount(codePoint);
			String codePointStr = str.substring(i, i + charCount);
			
			//Create possible matches from string index
			Entry[] indexedChinese
				= dict.chineseSearch.getWithCodePoint(codePoint);
			List<PossibleMatch> possibleMatches = new ArrayList<>();
			for (Entry entry: indexedChinese) {
				for (int index: indexesOf(entry.simplified, codePointStr))
					if (i - index >= i) //no lookbehind
						possibleMatches.add(
							new PossibleMatch(
								entry.simplified,
								entry,
								i - index));
				for (int index: indexesOf(entry.traditional, codePointStr))
					if (i - index >= i) //no lookbehind
						possibleMatches.add(
							new PossibleMatch(
								entry.traditional,
								entry,
								i - index));
			}
			
			//sort by length, longest first
			Collections.sort(
				possibleMatches,
				(a, b) -> b.str.length() - a.str.length());
			
			//find the match
			PossibleMatch match = null;
			for (PossibleMatch pm: possibleMatches) {
				if (pm.matches(str)) {
					match = pm;
					break;
				}
			}
			
			//continue loop if not match
			if (match == null) {
				i += charCount;
				continue;
			}

			//add match to result
			result.add(
				new SearchResult(
					match.entry,
					SearchResult.SENTENCE_SPLIT,
					false,
					true,
					-1,
					-1,
					-1));
			i += match.str.length() + (match.indexFound - i);
			
			//add equivalent matches to result
			List<Entry> added = new ArrayList<>();//avoid duplicates
			for (PossibleMatch pm: possibleMatches) {
				if (!(pm.indexFound == match.indexFound
					&& pm.entry != match.entry
					&& pm.str.equals(match.str)
					&& !added.contains(pm.entry))) {
					continue;
				}
				added.add(pm.entry);
				result.add(
					new SearchResult(
						pm.entry,
						SearchResult.SENTENCE_SPLIT,
						false,
						false,
						-1,
						-1,
						-1));
			}
		}
		
		return result.size() == 0 ? null : result;
	}
	
	//node class for splitChineseSentence method
	private static final class PossibleMatch {
		final String str;
		final Entry entry;
		final int indexFound;
		PossibleMatch(String str, Entry entry, int indexFound) {
			this.str = str;
			this.entry = entry;
			this.indexFound = indexFound;
		}
		boolean matches(String search) {
			/* check contained substring of str at the index
			 * indexFound within the string search */
			if (indexFound < 0)
				return false;
			for (int i = indexFound; i < indexFound + str.length(); i++) {
				if (
					(i - indexFound >= str.length() || i >= search.length())
					|| !charsEqual(
						search.charAt(i),
						str.charAt(i - indexFound)))
					return false;
			}
			return true;
		}
	}
	private static boolean charsEqual(char c1, char c2) {
		return c1 == c2
			|| Character.toLowerCase(c1)
			== Character.toLowerCase(c2);
	}
	
	
	//returns all indexes of the substring in the string
	private static final List<Integer>
	indexesOf(String str, String substr) {
		List<Integer> result = new ArrayList<>();
		int index = 0;
		while (true) {
			int i = str.indexOf(substr, index);
			index = i + substr.length();
			if (i == -1)
				return result;
			else
				result.add(index - substr.length());
		}
	}
	
	
	//identifies the chinese word at the index in a string
	Match findChineseWord(String chineseSentence, int index) {
		
		//Dictionary indexing
		int codePoint = chineseSentence.codePointAt(index);
		Entry[] indexedChinese
			= dict.chineseSearch.getWithCodePoint(codePoint);
		if (indexedChinese == null)
			return null;
		
		//Create possible matches from string index
		String codePointStr
			= chineseSentence
				.substring(
					index,
					index + Character.charCount(codePoint));
		List<PossibleMatch> possibleMatches = new ArrayList<>();
		for (Entry entry: indexedChinese) {
			for (int smplIndex: indexesOf(entry.simplified, codePointStr))
				possibleMatches.add(
					new PossibleMatch(
						entry.simplified,
						entry,
						index - smplIndex));
			for (int tradIndex: indexesOf(entry.traditional, codePointStr))
				possibleMatches.add(
					new PossibleMatch(
						entry.traditional,
						entry,
						index - tradIndex));
		}
		
		//sort by length, longest first
		Collections.sort(
			possibleMatches,
			(a, b) -> b.str.length() - a.str.length());
		
		//find the match
		for (PossibleMatch pm: possibleMatches)
			if (pm.matches(chineseSentence))
				return new Match(pm.str, pm.entry, pm.indexFound);
		
		//no match
		return null;
	}
}