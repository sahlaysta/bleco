package com.github.sahlaysta.bleco.dict;

import com.github.sahlaysta.bleco.dict.Dictionary;

/** Represents an example sentence in the
 * Chinese-English {@link Dictionary}.
 * Maintains the two strings,
 * {@link #chineseSentence} sentence
 * and its {@link #englishSentence} translation.
 * 
 * @author porog
 * */
public class ExampleSentence {
	
	/** The Chinese sentence of this
	 * {@link ExampleSentence}.
	 * @see #englishSentence */
	public final String chineseSentence;
	
	/** The English translated sentence of this
	 * {@link ExampleSentence}.
	 * @see #chineseSentence */
	public final String englishSentence;
	
	/** Constructs a new {@link ExampleSentence}
	 * to maintain the strings: the {@link #chineseSentence}
	 * and the {@link #englishSentence}.
	 * @param chineseSentence the Chinese example sentence
	 * @param englishSentence the English
	 * translation sentence
	 * @see #chineseSentence
	 * @see #englishSentence */
	public ExampleSentence(
			String chineseSentence,
			String englishSentence) {
		this.chineseSentence = chineseSentence;
		this.englishSentence = englishSentence;
	}
}