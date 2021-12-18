package com.github.sahlaysta.bleco;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Chinese-English Dictionary class that holds
 * {@link Entry} objects read from .bleco file
 * searchable by {@link #search(String)} */
public final class Dictionary {
	
	//No constructor
	private Dictionary() {}
	
	//Dictionary search
	
	/* Array of the Chinese-English example sentences */
	private static ExampleSentence[] exampleSentences;
	
	/* Array of the Chinese-English dictionary entries */
	private static Entry[] entries;
	
	/* Chinese search indexing HashMap,
	 * indexed by the lowest byte of the Unicode
	 * code point of Chinese characters.
	 * No odd numbers as keys, so if the lowest byte
	 * is odd, subtract 1 */
	private static Map<Byte, Entry[]> chineseIndexMap;
	private static final Entry[]
	getFromChineseIndexMap(int stringCodePoint) {
		if (stringCodePoint % 2 != 0)
			stringCodePoint--;
		return chineseIndexMap.get((byte)((stringCodePoint)&0xFF));
	}
	
	/* Pinyin search indexing HashMap,
	 * indexed by the first two letters
	 * of the pinyin pronunciation
	 * of each entry */
	private static Map<String, Entry[]> pinyinIndexMap;
	private static final Entry[]
	getFromPinyinIndexMap(String search) {
		return pinyinIndexMap.get(
				search.substring(0, 2).replace("'", " "));
	}
	
	
	/** Returns an {@link #ArrayList} of
	 * {@link Dictionary.Entry} objects found with the specified
	 * string search
	 * @param search the string to search
	 * Dictionary entries
	 * @return {@link #ArrayList} of
	 * {@link #Entry} objects found with the specified
	 * string search, or {@code null} if there is
	 * nothing found with the specified search
	 * @throws NullPointerException the specified
	 * string is {@code null}, or Dictionary has
	 * not been loaded or has failed to load with the
	 * {@link Dictionary#load(InputStream)} method
	 * @see Dictionary#load(InputStream)
	 * @see Dictionary#load(File)
	 * @see Dictionary#load(String) */
	public static final ArrayList<Entry> search(String search) {
		String formattedSearch = formatSearch(search);
		if (formattedSearch.isEmpty())
			return null;
		boolean chinese = containsChinese(formattedSearch);
		
		ArrayList<Entry> primarySearch
			= chinese
				? chineseSearch(formattedSearch)
				: pinyinSearch(formattedSearch);
		if (primarySearch != null)
			return primarySearch;
		
		ArrayList<Entry> secondarySearch
			= chinese
				? splitChineseSentence(search)
				: englishSearch(formattedSearch);
		if (secondarySearch != null)
			return secondarySearch;
		
		ArrayList<Entry> tertiarySearch
			= chinese
				? englishSearch(search)
				: chineseSearch(formattedSearch);
		return tertiarySearch;
	}
	private static final boolean
	containsChinese(String search) {
		for (int i = 0; i < search.length(); i++)
			if (search.charAt(i) > 9000)
				return true;
		return false;
	}
	private static final String formatSearch(String search) {
		String srcQry = search.toLowerCase();
		StringBuilder sb = new StringBuilder(srcQry.length());
		char ch;
		e: for (int i = 0; i < srcQry.length(); i++) {
			switch (ch = srcQry.charAt(i)) {
			
			//v and ü to u:
			case 'v': case 'ü':
				sb.append("u:");
				continue;
				
			//remove adjacent spaces + apostrophes
			case '\'': case ' ':
				if (i == 0)
					break;
				switch (srcQry.charAt(i - 1)) {
					case '1': case '2': case '3': case '4':
					case '5': case '\'': case ' ':
						continue e;
				}
				sb.append('\'');
				continue;
			
			}
			sb.append(ch);
		}
		
		if (sb.length() == 0)
			return "";
		if (sb.charAt(0) == ' ' ||
			sb.charAt(0) == '\'') //remove leading space
			sb.deleteCharAt(0);
		if (sb.length() > 0 &&
			sb.charAt(sb.length() - 1) == '\'') //remove ending space
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	
	//Dictionary chinese search
	private static final ArrayList<Entry>
	chineseSearch(String search) {
		Entry[] indexedChinese = getIndexedChineseEntries(search);
		if (indexedChinese == null)
			return null;
		ArrayList<Entry> result = new ArrayList<>();
		for (Entry e: indexedChinese)
			if (chineseMatches(e, search))
				result.add(e);
		return result.size() == 0 ? null : result;
	}
	private static final Entry[]
	getIndexedChineseEntries(String search) {
		/* get the Entry[] array from chineseIndexMap
		 * that has the smallest candidate length */
		Entry[] result = null;
		int lowestSize = Integer.MAX_VALUE;
		
		//iterate string codepoints
		for (int i = 0; i < search.length(); ) {
			int codePoint = search.codePointAt(i);
			Entry[] entries
				= getFromChineseIndexMap(codePoint);
			if (codePoint <= 9000) {
				//default / non-chinese letter
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
	private static final boolean
	chineseMatches(Entry entry, String search) {
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
				return false;
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
						return false;
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
					return false;
				searchI++;
				smplI++;
				tradI++;
				syllableCount++;
				continue;
			}
			
			
			if (searchCodePoint != smplCodePoint
				&& searchCodePoint != tradCodePoint)
				return false;
			searchI += Character.charCount(searchCodePoint);
			smplI += Character.charCount(smplCodePoint);
			tradI += Character.charCount(tradCodePoint);
			syllableCount++;
		}
		return true;
	}
	private static final ArrayList<Entry>
	splitChineseSentence(String search) {
		/* Returns the entries of all the words
		 * in the Chinese search
		 * e.g. the sentence "我们会法语" 
		 * split into the words "我们", "会", "法语"
		 * */
		
		ArrayList<Entry> result = new ArrayList<>();
		//iterate string codepoints
		e: for (int i = 0; i < search.length(); ) {
			int codePoint = search.codePointAt(i);
			int charCount = Character.charCount(codePoint);
			String codePointStr = search.substring(i, i + charCount);
			
			//Create possible matches from string index
			Entry[] indexedChinese
				= getFromChineseIndexMap(codePoint);
			ArrayList<PossibleMatch> possibleMatches = new ArrayList<>();
			for (Entry entry: indexedChinese) {
				for (int index: indexesOf(entry.simplified, codePointStr))
					if (i - index >= i)
						possibleMatches.add(
							new PossibleMatch(
								entry.simplified,
								entry,
								i - index));
				for (int index: indexesOf(entry.traditional, codePointStr))
					if (i - index >= i)
						possibleMatches.add(
							new PossibleMatch(
								entry.traditional,
								entry,
								i - index));
			}
			
			//sort by length, longest first
			Collections.sort(
				possibleMatches,
				new Comparator<PossibleMatch>() {
					@Override
					public int
					compare(PossibleMatch o1, PossibleMatch o2) {
						return o2.str.length() - o1.str.length();
					}
				});
			
			//find the match
			for (PossibleMatch pm: possibleMatches) {
				if (pm.matches(search)) {
					result.add(pm.entry);
					i += pm.str.length() + (pm.indexAt - i);
					continue e;
				}
			}
			
			i += charCount;
		}
		
		return result.size() == 0 ? null : result;
	}
	
	//node class for splitChineseSentence method
	private static final class PossibleMatch {
		final String str;
		final Entry entry;
		final int indexAt;
		PossibleMatch(String str, Entry entry, int indexAt) {
			this.str = str;
			this.entry = entry;
			this.indexAt = indexAt;
		}
		private final boolean matches(String search) {
			/* check contained substring of str at the index
			 * indexAt within the string search */
			if (indexAt < 0)
				return false;
			for (int i = indexAt; i < indexAt + str.length(); i++)
				if ((i - indexAt >= str.length() || i >= search.length())
					|| (search.charAt(i) != str.charAt(i - indexAt)))
					return false;
			return true;
		}
	}
	
	/** Finds and returns a {@link Match} of the nearest Chinese
	 * word at the specified index of the specified
	 * Chinese sentence
	 * @param chineseSentence the Chinese sentence
	 * @param index the index at which to search
	 * @return a match of the nearest Chinese word found at
	 * the index, or {@code null} if none
	 * @throws NullPointerException the specified string
	 * is {@code null}, or Dictionary has
	 * not been loaded or has failed to load with the
	 * {@link Dictionary#load(InputStream)} method
	 * @throws StringIndexOutOfBoundsException the given
	 * index is out of bounds with the given string */
	public static final Match
	findChineseWord(String chineseSentence, int index) {
		
		//Dictionary indexing
		int codePoint = chineseSentence.codePointAt(index);
		Entry[] indexedChinese
			= getFromChineseIndexMap(codePoint);
		if (indexedChinese == null)
			return null;
		
		//Create possible matches from string index
		String codePointStr
			= chineseSentence
				.substring(
					index,
					index + Character.charCount(codePoint));
		ArrayList<PossibleMatch> possibleMatches = new ArrayList<>();
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
			new Comparator<PossibleMatch>() {
				@Override
				public int
				compare(PossibleMatch o1, PossibleMatch o2) {
					return o2.str.length() - o1.str.length();
				}
			});
		
		//find the match
		for (PossibleMatch pm: possibleMatches)
			if (pm.matches(chineseSentence))
				return new Match(pm.str, pm.entry, pm.indexAt);
		
		//no match
		return null;
	}
	
	/** Match class represents a Chinese word match.
	 * It is the result of the
	 * {@link Dictionary#findChineseWord(String, int)}
	 * method. It maintains three variables: the 
	 * matched string {@link #str}, the
	 * matched {@link #entry}, and the {@link #index}
	 * at which the match is found */
	public static class Match {
		/** The string of the matched Chinese word
		 * in either simplified or traditional Chinese */
		public final String str;
		/** The matched {@link Entry} Chinese word
		 * found from the Chinese sentence */
		public final Entry entry;
		/** The index at which this match
		 * was found */
		public final int index;
		private Match(String str, Entry entry, int index) {
			this.str = str;
			this.entry = entry;
			this.index = index;
		}
	}
	
	
	
	//Dictionary pinyin search
	private static final ArrayList<Entry>
	pinyinSearch(String search) {
		return
				search.length() == 1
				? singleLetterPinyinSearch(search.charAt(0))
				: indexedPinyinLookup(search);
	}
	private static final
	ArrayList<Entry> singleLetterPinyinSearch(char ch) {
		ArrayList<String> strs = new ArrayList<>();
		for (String s: pinyinIndexMap.keySet())
			if (s.charAt(0) == ch)
				strs.add(s);
		if (strs.size() == 0)
			return null;
		Collections.sort(strs);
		ArrayList<Entry> result = new ArrayList<>();
		for (String s: strs)
			for (Entry e: pinyinIndexMap.get(s))
				if (e.pinyin.indexOf(' ') == -1) //only single-syllable
					result.add(e);
		return result;
	}
	private static final ArrayList<Entry>
	indexedPinyinLookup(String search) {
		ArrayList<Entry> result = new ArrayList<>();
		Entry[] indexedPinyin = getFromPinyinIndexMap(search);
		if (indexedPinyin == null)
			return null;
		int top = 0;
		for (Entry e: indexedPinyin) {
			switch (pinyinMatches(e.pinyin, search)) {
			case -1:
				break;
			case 0:
				result.add(e);
				break;
			case 1:
				result.add(top++, e);
				break;
			}
		}
		return result.size() == 0 ? null : result;
	}
	private static final int
	pinyinMatches(String pinyin, String search) {
		/* Compare the pinyin with the search
		 * return -1: no match
		 * return 0: match
		 * return 1: full-length match */
		
		int pinyinI = 0, pinyinLength = pinyin.length(),
				searchI = 0, searchLength = search.length();
		while (searchI < searchLength) {
			
			if (searchI >= pinyinLength
				|| pinyinI >= pinyinLength)
				return -1;
			
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
				
			//u cannot match u:
			case 'u':
				if (pinyinLength > (pinyinI + 1)
					&& pinyin.charAt(pinyinI + 1) == ':'
					&& (searchLength <= (searchI + 1)
					|| search.charAt(searchI + 1) != ':'))
					return -1;
			}
			
			
			/* skips spaces / pinyin tones for
			 * toneless / spaceless searching */
			e: while (true) {
				switch (pinyin.charAt(pinyinI)) {
				case ' ': case '1': case '2':
				case '3': case '4': case '5':
					if (++pinyinI >= pinyinLength)
						return -1;
					break;
				default: break e;
				}
			}
			
			
			if (!charsEqual(search.charAt(searchI), pinyin.charAt(pinyinI)))
				return -1;
			pinyinI++;
			searchI++;
		}
		
		//check full-length pinyin match
		if (pinyinI == pinyinLength) {
			return 1;
		} else if (pinyinI == pinyinLength - 1) {
			switch (pinyin.charAt(pinyinLength - 1)) {
			case '1': case '2': case '3': case '4': case '5':
				return 1;
			}
		}
		return 0;
	}

	private static final ArrayList<Entry> englishSearch(String search) {
		return null;//to be completed in future update of Bleco
	}

	

	private static final boolean charsEqual(
			char searchC, char pinyinC) {
		return searchC == pinyinC ||
				searchC == Character.toLowerCase(pinyinC);
	}
	
	//returns all indexes of the substring in string
	private static final Iterable<Integer>
	indexesOf(String str, String substr) {
		ArrayList<Integer> result = new ArrayList<>();
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
	
	
	//Dictionary Character type
	private static int characterType = 0;
	/** {@link Dictionary} character type preference
	 * for Simplified Chinese
	 * @see #TRADITIONAL_CHINESE
	 * @see #setCharacterType(int)
	 * @see #getCharacterType() */
	public static final int SIMPLIFIED_CHINESE = 0;
	/** {@link Dictionary} character type preference
	 * for Traditional Chinese
	 * @see #SIMPLIFIED_CHINESE
	 * @see #setCharacterType(int)
	 * @see #getCharacterType() */
	public static final int TRADITIONAL_CHINESE = 1;
	/** Sets the {@link Dictionary} character type preference
	 * used by {@link Entry#toString()}
	 * @param characterType the specified character type:
	 * {@link #SIMPLIFIED_CHINESE} or
	 * {@link #TRADITIONAL_CHINESE}
	 * @see #getCharacterType()
	 * @see #SIMPLIFIED_CHINESE
	 * @see #TRADITIONAL_CHINESE
	 * @see Entry#toString() */
	public static final void setCharacterType(int characterType) {
		Dictionary.characterType = characterType;
	}
	/** Returns the {@link Dictionary} character type preference
	 * used by {@link Entry#toString()}
	 * @return the {@link Dictionary}
	 * character type:
	 * {@link #SIMPLIFIED_CHINESE} or
	 * {@link #TRADITIONAL_CHINESE}
	 * @see #setCharacterType(int)
	 * @see #SIMPLIFIED_CHINESE
	 * @see #TRADITIONAL_CHINESE
	 * @see Entry#toString() */
	public static final int getCharacterType() {
		return characterType;
	}
	
	/** A Chinese-English {@link Dictionary}
	 * example sentence class that maintains the
	 * two strings, the {@link #chinese} sentence
	 * and its
	 * {@link #english} translation */
	public static class ExampleSentence {
		/** The Chinese sentence of this
		 * {@link ExampleSentence}
		 * @see #english */
		public final String chinese;
		/** The English sentence of this
		 * {@link ExampleSentence}
		 * @see #chinese */
		public final String english;
		/** Constructs a new {@link ExampleSentence},
		 * maintaining the strings: the {@link #chinese}
		 * sentence and the {@link #english} sentence
		 * @param chinese the Chinese sentence
		 * @param english the English sentence
		 * @see #chinese
		 * @see #english */
		public ExampleSentence(String chinese, String english) {
			this.chinese = chinese;
			this.english = english;
		}
	}
	
	/** An Entry object is a single {@link Dictionary} entry
	 * of the Chinese-English dictionary, holding its
	 * {@link #simplified} and {@link #traditional} texts, its
	 * {@link #pinyin}, its {@link #formattedPinyin}, its
	 * English {@link #definitions}, and its Chinese-English
	 * {@link #exampleSentences} */
	public static class Entry {
		/** The simplified Chinese text of this {@link Entry}
		 * @see #traditional
		 * @see #pinyin
		 * @see #formattedPinyin
		 * @see #definitions
		 * @see #exampleSentences */
		public final String simplified;
		/** The traditional Chinese text of this {@link Entry}
		 * @see #simplified
		 * @see #pinyin
		 * @see #formattedPinyin
		 * @see #definitions
		 * @see #exampleSentences */
		public final String traditional;
		/** The pinyin reading of this {@link Entry}
		 * @see #simplified
		 * @see #traditional
		 * @see #formattedPinyin
		 * @see #definitions
		 * @see #exampleSentences */
		public final String pinyin;
		/** The styled pinyin reading of this {@link Entry}
		 * @see #simplified
		 * @see #traditional
		 * @see #pinyin
		 * @see #definitions
		 * @see #exampleSentences */
		public final String formattedPinyin;
		/** The definitions of this {@link Entry}
		 * @see #simplified
		 * @see #traditional
		 * @see #pinyin
		 * @see #formattedPinyin
		 * @see #exampleSentences */
		public final String[] definitions;
		/** The example sentences of this {@link Entry}
		 * @see ExampleSentence
		 * @see #simplified
		 * @see #traditional
		 * @see #pinyin
		 * @see #formattedPinyin */
		public final ExampleSentence[] exampleSentences;
		
		/** Constructs a new {@link Entry} defining each field
		 * @param simplified the simplified text
		 * of the entry
		 * @param traditional the traditional text
		 * of the entry
		 * @param pinyin the pinyin of the entry
		 * @param formattedPinyin the formatted pinyin
		 * of the entry
		 * @param definitions the definitions of the entry
		 * @param exampleSentences the example sentences
		 * of the entry
		 * @see #simplified
		 * @see #traditional
		 * @see #pinyin
		 * @see #formattedPinyin
		 * @see #definitions
		 * @see #exampleSentences */
		public Entry(
				String simplified,
				String traditional,
				String pinyin,
				String formattedPinyin,
				String[] definitions,
				ExampleSentence[] exampleSentences) {
			this.simplified = simplified;
			this.traditional = traditional;
			this.pinyin = pinyin;
			this.formattedPinyin = formattedPinyin;
			this.definitions = definitions;
			this.exampleSentences = exampleSentences;
		}
		
		/** Returns a string representation of this
		 * {@link Entry} with its Chinese text
		 * (simplified or traditional according to
		 * {@link Dictionary#getCharacterType()
		 * getCharacterType()}),
		 * its styled pinyin reading, and all of its
		 * definitions separated by forward slash
		 * {@code '/'}.
		 * @return a string representation
		 * of this {@link Entry}
		 * 
		 * @see Dictionary#setCharacterType(int)
		 * setCharacterType(int)
		 * @see Dictionary#getCharacterType()
		 * getCharacterType()
		 * @see Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE
		 * @see Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE
		 * @see Entry#toString(int)
		 * @see #simplified
		 * @see #traditional
		 * @see #formattedPinyin
		 * @see #definitions */
		@Override
		public String toString() {
			return toString(characterType);
		}
		
		/** Returns a string representation of this
		 * {@link Entry} with its Chinese text
		 * (simplified or traditional according to
		 * the specified character type),
		 * its styled pinyin reading, and all of its
		 * definitions separated by forward slash
		 * {@code '/'}.
		 * @param characterType the specified
		 * character type,
		 * {@link Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE} or
		 * {@link Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE}
		 * @return a string representation
		 * of this {@link Entry}
		 * 
		 * @see Dictionary#setCharacterType(int)
		 * setCharacterType(int)
		 * @see Dictionary#getCharacterType()
		 * getCharacterType()
		 * @see Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE
		 * @see Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE
		 * @see #simplified
		 * @see #traditional
		 * @see #formattedPinyin
		 * @see #definitions */
		public String toString(int characterType) {
			return
					characterType == TRADITIONAL_CHINESE
					? toStringTraditional()
					: toStringSimplified();
		}
		private String toStringTraditional() {
			StringBuilder sb = new StringBuilder();
			sb.append(traditional);
			sb.append(" - ");
			sb.append(formattedPinyin);
			sb.append(" - ");
			appendTraditionalFormattedDefinitionsToStringBuilder(
					sb, definitions);
			return sb.toString();
		}
		private String toStringSimplified() {
			StringBuilder sb = new StringBuilder();
			sb.append(simplified);
			sb.append(" - ");
			sb.append(formattedPinyin);
			sb.append(" - ");
			appendSimplifiedFormattedDefinitionsToStringBuilder(
					sb, definitions);
			return sb.toString();
		}
		private static final void
		appendTraditionalFormattedDefinitionsToStringBuilder(
				StringBuilder sb, String[] definitions) {
			for (int i = 0; i < definitions.length; i++) {
				appendTraditionalFormattedDefinitionToStringBuilder(
						sb, definitions[i]);
				if (i < definitions.length - 1)
					sb.append(" / ");
			}
		}
		private static final void
		appendSimplifiedFormattedDefinitionsToStringBuilder(
				StringBuilder sb, String[] definitions) {
			for (int i = 0; i < definitions.length; i++) {
				appendSimplifiedFormattedDefinitionToStringBuilder(
						sb, definitions[i]);
				if (i < definitions.length - 1)
					sb.append(" / ");
			}
		}
		
		/* Format CC-CEDICT's simplified-traditional split
		 * with the character "|"
		 * For example the definition:
		 * cc-cedict  : "Waipu Township in Taichung County 臺中縣|�?�中县"
		 * traditional: "Waipu Township in Taichung County 臺中縣"
		 * simplified : "Waipu Township in Taichung County �?�中县" */
		private static final void
		appendTraditionalFormattedDefinitionToStringBuilder(
				StringBuilder sb, String definition) {
			int len = definition.length();
			char ch;
			e: for (int i = 0; i < len; i++) {
				switch (ch = definition.charAt(i)) {
				case '|':
					for (int ii = i + 1; ii < len; ii++) {
						switch (definition.charAt(ii)) {
						case '[': case ' ': case ',':
						case ':': case ';': case ')':
							i = --ii;
							continue e;
						}
					}
					return;
				case '[':
					for (int ii = i + 1; ii < len; ii++) {
						if (definition.charAt(ii) == ']') {
							i = ii;
							continue e;
						}
					}
				}
				sb.append(ch);
			}
		}
		private static final void
		appendSimplifiedFormattedDefinitionToStringBuilder(
				StringBuilder sb, String definition) {
			int len = definition.length();
			char ch;
			e: for (int i = 0; i < len; i++) {
				switch (ch = definition.charAt(i)) {
				case '|': {
					int ii = 0;
					e2: for (ii = i - 1; ii >= 0; ii--) {
						switch (definition.charAt(ii)) {
						case ':': case ' ': case ',': case '(':
							break e2;
						}
					}
					sb.setLength(sb.length() - (i - ii) + 1);
					continue e;
				}
				case '[':
					for (int ii = i + 1; ii < len; ii++) {
						if (definition.charAt(ii) == ']') {
							i = ii;
							continue e;
						}
					}
				}
				sb.append(ch);
			}
		}
		
		/** Returns the {@link #definitions} of this {@link Entry}
		 * formatting CC-CEDICT's simplified-traditional
		 * split with the character <code>'|'</code>,
		 * simplified or traditional depending on the
		 * character type of {@link Dictionary#getCharacterType()
		 * getCharacterType()}
		 * 
		 * @return the formatted {@link #definitions}
		 * of this {@link Entry}
		 * 
		 * @see Dictionary#setCharacterType(int)
		 * setCharacterType(int)
		 * @see Dictionary#getCharacterType()
		 * getCharacterType()
		 * @see Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE
		 * @see Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE
		 * @see Entry#formattedDefinitions(int)
		 * @see #definitions
		 * */
		public String[] formattedDefinitions() {
			return formattedDefinitions(characterType);
		}
		
		/** Returns the {@link #definitions} of this {@link Entry}
		 * formatting CC-CEDICT's simplified-traditional
		 * split with the character <code>'|'</code>,
		 * depending on the specified character type,
		 * {@link Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE} or
		 * {@link Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE}
		 * 
		 * @param characterType the specified
		 * character type,
		 * {@link Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE} or
		 * {@link Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE}
		 * 
		 * @return the formatted {@link #definitions}
		 * of this {@link Entry}
		 * 
		 * @see Dictionary#setCharacterType(int)
		 * setCharacterType(int)
		 * @see Dictionary#getCharacterType()
		 * getCharacterType()
		 * @see Dictionary#SIMPLIFIED_CHINESE
		 * SIMPLIFIED_CHINESE
		 * @see Dictionary#TRADITIONAL_CHINESE
		 * TRADITIONAL_CHINESE
		 * @see #definitions
		 * */
		public String[] formattedDefinitions(int characterType) {
			return characterType == Dictionary.TRADITIONAL_CHINESE
					? traditionalFormattedDefinitions()
					: simplifiedFormattedDefinitions();
		}
		private String[] traditionalFormattedDefinitions() {
			String[] result = new String[definitions.length];
			for (int i = 0; i < result.length; i++) {
				StringBuilder sb = new StringBuilder();
				appendTraditionalFormattedDefinitionToStringBuilder(
						sb, definitions[i]);
				result[i] = sb.toString();
			}
			return result;
		}
		private String[] simplifiedFormattedDefinitions() {
			String[] result = new String[definitions.length];
			for (int i = 0; i < result.length; i++) {
				StringBuilder sb = new StringBuilder();
				appendSimplifiedFormattedDefinitionToStringBuilder(
						sb, definitions[i]);
				result[i] = sb.toString();
			}
			return result;
		}
	}
	
	
	/** Loads {@link Dictionary} from the specified .bleco file
	 * @param blecoFile the .bleco file
	 * @throws IOException thrown by {@link InputStream}
	 * @throws FileNotFoundException thrown by
	 * {@link FileInputStream#FileInputStream(File) FileInputStream(File)}
	 * @throws SecurityException thrown by
	 * {@link FileInputStream#FileInputStream(File) FileInputStream(File)}
	 * @throws NullPointerException the specified file is {@code null}
	 * @see Dictionary#search(String)
	 * @see Dictionary#load(InputStream)
	 * @see Dictionary#load(String) */
	public static final void load(File blecoFile) throws IOException {
		load(new FileInputStream(blecoFile));
	}
	/** Loads {@link Dictionary} from the specified .bleco file
	 * @param blecoFile the .bleco filepath
	 * @throws IOException thrown by {@link InputStream}
	 * @throws FileNotFoundException thrown by
	 * {@link FileInputStream#FileInputStream(String) FileInputStream(String)}
	 * @throws SecurityException thrown by
	 * {@link FileInputStream#FileInputStream(String) FileInputStream(String)}
	 * @throws NullPointerException the specified string is {@code null}
	 * @see Dictionary#search(String)
	 * @see Dictionary#load(InputStream)
	 * @see Dictionary#load(File) */
	public static final void load(String blecoFile) throws IOException {
		load(new FileInputStream(blecoFile));
	}
	/** Loads {@link Dictionary} from the specified .bleco
	 * {@link InputStream} object,
	 * creating a new {@link BufferedInputStream}
	 * @param is the InputStream with .bleco data
	 * @throws IOException thrown by {@link InputStream}
	 * @throws NullPointerException the specified
	 * InputStream is {@code null}
	 * @see BufferedInputStream#BufferedInputStream(InputStream)
	 * BufferedInputStream(InputStream)
	 * @see Dictionary#search(String)
	 * @see Dictionary#load(File)
	 * @see Dictionary#load(String) */
	public static final void load(InputStream is) throws IOException {
		DataInputStream dis
			= new DataInputStream(new BufferedInputStream(is));
		exampleSentences = readExampleSentences(dis);
		entries = readEntries(dis, exampleSentences);
		chineseIndexMap = readChineseIndexMap(dis, entries);
		pinyinIndexMap = readPinyinIndexMap(dis, entries);
		dis.close();
	}
	
	
	//Read .bleco
	
	//read example sentences
	private static final ExampleSentence[]
	readExampleSentences(DataInputStream dis)
					throws IOException {
		ExampleSentence[] result = new ExampleSentence[dis.readInt()];
		for (int i = 0; i < result.length; i++)
			result[i] = new ExampleSentence(
					readString(dis), readString(dis));
		return result;
	}
	//read Entries
	private static final Entry[] readEntries(
			DataInputStream dis, ExampleSentence[] exampleSentences)
					throws IOException {
		Entry[] result = new Entry[dis.readInt()];
		for (int i = 0; i < result.length; i++)
			result[i] = readEntry(dis, exampleSentences);
		return result;
	}
	private static final Entry readEntry(
			DataInputStream dis, ExampleSentence[] exampleSentences)
					throws IOException {
		String simplified = readString(dis),
			traditional = readString(dis),
			pinyin = readString(dis),
			formattedPinyin = readString(dis);
		
		String[] definitions = new String[dis.read()];
		for (int i = 0; i < definitions.length; i++)
			definitions[i] = readString(dis);
		
		ExampleSentence[] entryExampleSentences
			= new ExampleSentence[dis.readInt()];
		for (int i = 0; i < entryExampleSentences.length; i++)
			entryExampleSentences[i] = exampleSentences[dis.readInt()];
		if (entryExampleSentences.length == 0)
			entryExampleSentences = null;
		
		return new Entry(
				simplified,
				traditional,
				pinyin,
				formattedPinyin,
				definitions,
				entryExampleSentences);
	}
	private static final String readString(DataInputStream dis)
			throws IOException {
		byte[] b = new byte[dis.readChar()];
		dis.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}
	
	//read maps
	private static final HashMap<Byte, Entry[]>
	readChineseIndexMap(DataInputStream dis, Entry[] entries)
			throws IOException {
		int capacity = dis.readChar();
		HashMap<Byte, Entry[]> result
			= new HashMap<>(capacity);
		for (int i = 0; i < capacity; i++) {
			byte key = dis.readByte();
			Entry[] value = new Entry[dis.readChar()];
			for (int ii = 0; ii < value.length; ii++)
				value[ii] = entries[dis.readInt()];
			result.put(key, value);
		}
		return result;
	}
	private static final HashMap<String, Entry[]>
	readPinyinIndexMap(DataInputStream dis, Entry[] entries)
			throws IOException {
		int capacity = dis.readChar();
		HashMap<String, Entry[]> result
			= new HashMap<>(capacity);
		for (int i = 0; i < capacity; i++) {
			String key = readString(dis);
			Entry[] value = new Entry[dis.readChar()];
			for (int ii = 0; ii < value.length; ii++)
				value[ii] = entries[dis.readInt()];
			result.put(key, value);
		}
		return result;
	}
}