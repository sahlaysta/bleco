package sahlaysta.bleco.dict;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/** The Chinese-English dictionary of Bleco.
 * Maintains {@link Entry} objects and is
 * searchable by {@link #search(String)}.
 * 
 * @author sahlaysta
 * */
public class Dictionary {
	
	//Constructor
	/** Constructs an empty {@link Dictionary}
	 * that is not loaded .
	 * @see Dictionary#load(InputStream) */
	public Dictionary() {}
	
	private boolean isDictionaryLoaded = false;
	
	//Dictionary

	/** Array of all the dictionary entries. */
	protected Entry[] entries;
	
	/** Array of all the dictionary example
	 * sentences. */
	protected ExampleSentence[] exampleSentences;
	
	
	
	
	//Dictionary searching
	
	//indexed searching abstractions
	DictionaryChineseSearch chineseSearch;
	DictionaryEnglishSearch englishSearch;
	DictionaryPinyinSearch pinyinSearch;
	
	//chinese sentence splitter
	final DictionarySentenceHelper dsh
		= new DictionarySentenceHelper(this);
	
	/** Returns an unmodifiable {@link List}
	 * of {@link SearchResult} objects searched from this
	 * {@link Dictionary} with the specified search string
	 * @param search the string to search
	 * this {@link Dictionary}.
	 * @return an unmodifiable {@link List} of
	 * {@link SearchResult} objects found with the
	 * specified search, or {@code null} if no search
	 * results are found
	 * @throws NullPointerException this {@link Dictionary} is
	 * not loaded or failed to load
	 * @see Dictionary#load(InputStream) */
	public List<SearchResult> search(String search) {
		if (!isDictionaryLoaded)
			throw new NullPointerException("Dictionary not loaded");

		//Format input string before search (important)
		if (search == null || search.isEmpty())
			return null;
		String formattedSearch = DictionaryAbstractSearch.formatSearch(search);
		if (formattedSearch.isEmpty())
			return null;

		//Wildcard search
		if (formattedSearch.indexOf('*') != -1)
			return wildcardSearch(formattedSearch);
		
		//Try searches
		for (int i = 1; ; i++) {
			List<SearchResult> result = null;
			switch (i) {
			case 1:
				result = pinyinSearch.search(formattedSearch);
				break;
			case 2:
				result = chineseSearch.search(formattedSearch);
				break;
			case 3:
				result = englishSearch.search(search);
				break;
			case 4:
				result = sentenceSplitSearch(search);
				break;
			case 5:
				return null;
			}
			if (result != null)
				return uml(result);
		}
	}
	private static <T> List<T> uml(List<T> list) {
		//make a list unmodifiable
		return
			list == null
				? null
				: Collections.unmodifiableList(list);
	}
	private List<SearchResult> wildcardSearch(String search) {
		/* wildcard searches must have at least
		 * one chinese character (because of indexing) */
		int minRange = DictionaryAbstractSearch.UNICODE_CHINESE_START_RANGE;
		for (int i = 0, len = search.length(); i < len; i++) {
			char ch = search.charAt(i);
			if (ch != '*' && ch > minRange)
				return uml(chineseSearch.search(search));
		}
		return null;
	}
	private List<SearchResult> sentenceSplitSearch(String search) {
		/* returns null if the search
		 * has no chinese characters */
		int minRange = DictionaryAbstractSearch.UNICODE_CHINESE_START_RANGE;
		for (int i = 0, len = search.length(); i < len; i++)
			if (search.charAt(i) > minRange)
				return dsh.splitChineseSentence(search);
		return null;
	}
	
	
	/** Returns an unmodifiable {@link List}
	 * of {@link SearchResult} objects searched from this
	 * {@link Dictionary} with the specified search string,
	 * searching exclusively in English.
	 * @param search the string to search
	 * this {@link Dictionary}
	 * @return an unmodifiable {@link List} of
	 * {@link SearchResult} objects found with the
	 * specified search, or {@code null} if no search
	 * results are found
	 * @throws NullPointerException this {@link Dictionary} is
	 * not loaded or failed to load
	 * @see Dictionary#load(InputStream) */
	public List<SearchResult> englishSearch(String search) {
		List<SearchResult> result
			= englishSearch.search(search);
		if (result == null || result.size() == 0)
			return null;
		return Collections.unmodifiableList(result);
	}
	
	
	
	/** Identifies a Chinese word at the specified index inside the
	 * specified Chinese sentence string. Does both lookbehind and
	 * lookahead search from the specified index.
	 * @param chineseSentence the Chinese sentence string
	 * @param index the index in the sentence string of the
	 * word to be identified
	 * @return a {@link Match} with the identified Chinese
	 * word, or {@code null} if none
	 * @throws NullPointerException this {@link Dictionary} is
	 * not loaded or failed to load
	 * @see Dictionary#load(InputStream) */
	public Match findChineseWord(String chineseSentence, int index) {
		if (!isDictionaryLoaded)
			throw new NullPointerException("Dictionary not loaded");
		return dsh.findChineseWord(chineseSentence, index);
	}
	
	
	//Load Bleco dictionary
	/** Loads this {@link Dictionary} from the
	 * given .bleco file.
	 * @param is an input stream of .bleco file data
	 * @throws IOException if an I/O error occurs
	 * */
	public void load(InputStream is) throws IOException {
		isDictionaryLoaded = false;
		BlecoInputStream bis
			= new BlecoInputStream(
				new BufferedInputStream(is));
		exampleSentences = bis.readExampleSentences();
		entries = bis.readEntries(exampleSentences);
		chineseSearch = new DictionaryChineseSearch(
			bis.readChineseIndexMap(entries));
		englishSearch = new DictionaryEnglishSearch(
			bis.readEnglishIndexMap(entries));
		pinyinSearch = new DictionaryPinyinSearch(
			bis.readPinyinIndexMap(entries));
		bis.close();
		isDictionaryLoaded = true;
	}
}