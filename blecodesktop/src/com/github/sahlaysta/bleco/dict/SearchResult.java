package com.github.sahlaysta.bleco.dict;

/** A {@link SearchResult} object is a
 * search result returned from
 * the {@link Dictionary#search(String)}
 * method, often returned in
 * multiples.
 * 
 * @author porog
 * */
public class SearchResult {
	
	/** The {@link Entry} associated
	 * with this search result.
	 * @see Entry */
	public final Entry entry;
	
	/** The search result type
	 * of this search match.
	 * @see SearchResult#NO_TYPE
	 * @see SearchResult#CHINESE_SEARCH
	 * @see SearchResult#ENGLISH_SEARCH
	 * @see SearchResult#PINYIN_SEARCH
	 * @see SearchResult#SENTENCE_SPLIT */
	public final int type;
	
	/** (english and pinyin searches only)
	 * {@code true} if this search match
	 * was a full-length 100% match. */
	public final boolean isFullMatch;
	
	/** (sentence split searches only) {@code true} if
	 * this search match is the first of a sentence
	 * split group. */
	public final boolean isFirstOfSplitGroup;
	
	/** (english searches only) the nth
	 * definition where the search was matched. */
	public final int definition;
	
	/** (english searches only) the begin
	 * index of the search match. */
	public final int beginIndex;
	
	/** (english searches only) the end
	 * index of the search match. */
	public final int endIndex;
	
	/** Constructs a {@link SearchResult} with
	 * the associated {@link Entry} to the
	 * search result.
	 * @see #entry
	 * @see #SearchResult(Entry, int, boolean,
	 * boolean, int, int, int)
	 * */
	public SearchResult(Entry entry) {
		this.entry = entry;
		this.type = SearchResult.NO_TYPE;
		this.isFullMatch = false;
		this.isFirstOfSplitGroup = false;
		this.beginIndex = -1;
		this.endIndex = -1;
		this.definition = -1;
	}
	
	/** Constructs a {@link SearchResult}
	 * defining each field.
	 * @param entry the associated {@link Entry}
	 * to the {@link SearchResult}
	 * @param type the type of the search result
	 * @param isFullMatch (english and pinyin
	 * searches only) {@code true} if the search
	 * result is a full-length 100% match
	 * @param isFirstOfSplitGroup (sentence split
	 * searches only) {@code true} if the
	 * search result is the first of a split group match
	 * @param definition (english searches only) the
	 * nth definition where the search was matched
	 * @param beginIndex (english searches only)
	 * the begin index of the search match
	 * @param endIndex (english searches only) the
	 * end index of the search match
	 * @see #entry
	 * @see #type
	 * @see #isFullMatch
	 * @see #isFirstOfSplitGroup
	 * @see #definition
	 * @see #beginIndex
	 * @see #endIndex
	 * */
	public SearchResult(
			Entry entry,
			int type,
			boolean isFullMatch,
			boolean isFirstOfSplitGroup,
			int definition,
			int beginIndex,
			int endIndex) {
		this.entry = entry;
		this.type = type;
		this.isFullMatch = isFullMatch;
		this.isFirstOfSplitGroup = isFirstOfSplitGroup;
		this.definition = definition;
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}
	
	/** Returns the {@link Object#toString() toString()}
	 * of the associated {@link #entry} of this
	 * {@link SearchResult}.
	 * @return the {@link Object#toString() toString()}
	 * of this {@link SearchResult}'s {@link #entry}
	 * @see Entry#toString() */
	@Override
	public String toString() {
		return entry.toString();
	}
	
	
	
	//SearchResult type enum
	
	/** Type that indicates that
	 * a {@link SearchResult} is
	 * of no type.
	 * @see SearchResult#type */
	public static final int NO_TYPE = 0;
	
	/** Type that indicates that
	 * a {@link SearchResult} is
	 * from a search made in Chinese.
	 * @see SearchResult#type */
	public static final int CHINESE_SEARCH = 1;
	
	/** Type that indicates that
	 * a {@link SearchResult} is
	 * from a search made in English.
	 * @see SearchResult#type */
	public static final int ENGLISH_SEARCH = 2;
	
	/** Type that indicates that
	 * a {@link SearchResult} is
	 * from a search made in Pinyin.
	 * @see SearchResult#type */
	public static final int PINYIN_SEARCH = 3;
	
	/** Type that indicates that a
	 * {@link SearchResult} is
	 * from a Chinese sentence split.
	 * @see SearchResult#type */
	public static final int SENTENCE_SPLIT = 4;
}