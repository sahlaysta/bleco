package com.github.sahlaysta.bleco.dict;

/** Represents a Chinese word match;
 * the result of the
 * {@link Dictionary#findChineseWord(String, int)}
 * method. Maintains three variables: the 
 * matched string {@link #str}, the
 * matched {@link #entry}, and the string
 * {@link #index} at which the match was found */
public class Match {
	
	/** The matched substring in either
	 * simplified or traditional Chinese */
	public final String str;
	
	/** The matched {@link Entry} associated
	 * with this match */
	public final Entry entry;
	
	/** The index at which this match was
	 * found in the Chinese sentence string */
	public final int index;
	
	/** Constructs a {@link Match} with each field
	 * @param str the matched substring
	 * @param entry the matched entry
	 * @param index the string index of the match
	 * @see #str
	 * @see #entry
	 * @see #index */
	public Match(String str, Entry entry, int index) {
		this.str = str;
		this.entry = entry;
		this.index = index;
	}
}