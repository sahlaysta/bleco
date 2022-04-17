package sahlaysta.bleco.dict;

import java.util.concurrent.atomic.AtomicLong;

/** An Entry object is a {@link Dictionary} entry
 * of the Chinese-English dictionary, maintaining its
 * {@link #simplified} and {@link #traditional} Chinese, its
 * {@link #pinyin} and {@link #formattedPinyin}, its
 * English {@link #definitions}, and its Chinese-English
 * {@link #exampleSentences}.
 * 
 * <p>Whether an Entry object's {@link #toString()} operation
 * is returned in Simplified Chinese or in
 * Traditional Chinese is determined by the
 * character type of {@link Entry#getCharacterType()}.
 * 
 * @author sahlaysta
 * */
public class Entry {
	
	/** The Simplified Chinese of this {@link Entry}. */
	public final String simplified;
	/** The Traditional Chinese of this {@link Entry}. */
	public final String traditional;
	/** The unformatted pinyin reading
	 * of this {@link Entry}. */
	public final String pinyin;
	/** The styled pinyin reading
	 * of this {@link Entry}. */
	public final String formattedPinyin;
	/** The definitions of this {@link Entry}. */
	public final String[] definitions;
	/** The normalized searching definitions
	 * of this {@link Entry}. */
	public final String[] normalizedDefinitions;
	/** The example sentences of this {@link Entry}. */
	public final ExampleSentence[] exampleSentences;
	
	/** Constructs an {@link Entry} defining each field.
	 * @param simplified the Simplified Chinese
	 * of the entry
	 * @param traditional the Traditional Chinese
	 * of the entry
	 * @param pinyin the unformatted pinyin of the entry
	 * @param formattedPinyin the formatted pinyin
	 * of the entry
	 * @param definitions the definitions of the entry
	 * @param normalizedDefinitions the normalized
	 * definitions of the entry
	 * @param exampleSentences the example sentences
	 * of the entry
	 * @see #simplified
	 * @see #traditional
	 * @see #pinyin
	 * @see #formattedPinyin
	 * @see #definitions
	 * @see #normalizedDefinitions
	 * @see #exampleSentences */
	public Entry(
			String simplified,
			String traditional,
			String pinyin,
			String formattedPinyin,
			String[] definitions,
			String[] normalizedDefinitions,
			ExampleSentence[] exampleSentences) {
		this.simplified = simplified;
		this.traditional = traditional;
		this.pinyin = pinyin;
		this.formattedPinyin = formattedPinyin;
		this.definitions = definitions;
		this.normalizedDefinitions = normalizedDefinitions;
		this.exampleSentences = exampleSentences;
	}
	
	
	
	//Entry toString() Character type settings
	
	private static AtomicLong characterType = new AtomicLong(0);
	/** {@link Entry#toString()} type value
	 * for Simplified Chinese.
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#setCharacterType(int)
	 * @see Entry#getCharacterType()
	 * @see Entry#toString() */
	public static final int SIMPLIFIED_CHINESE = 0;
	/** {@link Entry#toString()} type value
	 * for Traditional Chinese.
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#setCharacterType(int)
	 * @see Entry#getCharacterType()
	 * @see Entry#toString() */
	public static final int TRADITIONAL_CHINESE = 1;
	/** Sets the character type
	 * used by {@link Entry#toString()}.
	 * @param characterType the new character type:
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}.
	 * @see Entry#getCharacterType()
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#toString() */
	public static final void setCharacterType(int characterType) {
		if (characterType != SIMPLIFIED_CHINESE
				&& characterType != TRADITIONAL_CHINESE)
			throw new IllegalArgumentException(
				"Bad character type: " + characterType);
		Entry.characterType.set(characterType);
	}
	/** Returns the character type value
	 * used by {@link Entry#toString()}
	 * @return the {@link Entry#toString()}
	 * character type:
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}.
	 * @see Entry#setCharacterType(int)
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#toString() */
	public static final int getCharacterType() {
		return characterType.intValue();
	}
	
	
	
	// Entry toString operations
	
	/** Returns a string representation of this
	 * {@link Entry} (with its Chinese in
	 * simplified or traditional according to
	 * {@link Entry#getCharacterType()}),
	 * its styled pinyin reading, and all of its
	 * definitions separated by forward slash
	 * {@code '/'}.
	 * @return a string representation
	 * of this {@link Entry}.
	 * 
	 * @see Entry#setCharacterType(int)
	 * @see Entry#getCharacterType()
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#toString(int)
	 * @see Entry#simplified
	 * @see Entry#traditional
	 * @see Entry#formattedPinyin
	 * @see Entry#definitions */
	@Override
	public String toString() {
		return toString(characterType.intValue());
	}
	
	/** Returns a string representation of this
	 * {@link Entry} (with its Chinese in
	 * simplified or traditional according to
	 * the specified character type),
	 * its styled pinyin reading, and all of its
	 * definitions separated by forward slash
	 * {@code '/'}.
	 * @param characterType the specified
	 * character type,
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}
	 * @return a string representation
	 * of this {@link Entry}.
	 * 
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#simplified
	 * @see Entry#traditional
	 * @see Entry#formattedPinyin
	 * @see Entry#definitions */
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
	 * cc-cedict  : "Yangcheng, a nickname for 廣州|广州"
	 * traditional: "Yangcheng, a nickname for 廣州"
	 * simplified : "Yangcheng, a nickname for 广州"
	 * */
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
					//all verified lookaheads
					case '[': case ' ': case ',':
					case ':': case ';': case ')':
					case '<':
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
					//all verified lookbehinds
					case ':': case ' ': case ',': case '(':
					case '>':
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
	 * to simplified or traditional depending on the
	 * character type of {@link Entry#getCharacterType()}.
	 * 
	 * @return the formatted {@link #definitions}
	 * of this {@link Entry}
	 * 
	 * @see Entry#setCharacterType(int)
	 * @see Entry#getCharacterType()
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#formattedDefinitions(int)
	 * @see Entry#definitions
	 * */
	public String[] formattedDefinitions() {
		return formattedDefinitions(characterType.intValue());
	}
	
	/** Returns the {@link #definitions} of this {@link Entry}
	 * formatting CC-CEDICT's simplified-traditional
	 * split with the character <code>'|'</code>,
	 * depending on the specified character type,
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}.
	 * 
	 * @param characterType the specified
	 * character type,
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}
	 * 
	 * @return formatted {@link #definitions}
	 * of this {@link Entry}
	 * 
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#definitions
	 * */
	public String[] formattedDefinitions(int characterType) {
		return characterType == Entry.TRADITIONAL_CHINESE
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
	
	
	/** Returns either {@link #simplified}
	 * or {@link #traditional} of
	 * this {@link Entry} according to the
	 * character type of {@link Entry#getCharacterType()}.
	 * @return the simplified or traditional chinese
	 * of this {@link Entry}
	 * @see Entry#setCharacterType(int)
	 * @see Entry#getCharacterType()
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#getName(int)
	 * @see Entry#simplified
	 * @see Entry#traditional */
	public String getName() {
		return getName(characterType.intValue());
	}
	
	/** Returns either {@link #simplified}
	 * or {@link #traditional} of
	 * this {@link Entry} according to the specified
	 * character type,
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}.
	 * 
	 * @param characterType the specified
	 * character type,
	 * {@link Entry#SIMPLIFIED_CHINESE} or
	 * {@link Entry#TRADITIONAL_CHINESE}
	 * 
	 * @return the simplified or traditional chinese
	 * of this {@link Entry}
	 * 
	 * @see Entry#SIMPLIFIED_CHINESE
	 * @see Entry#TRADITIONAL_CHINESE
	 * @see Entry#simplified
	 * @see Entry#traditional */
	public String getName(int characterType) {
		return
			characterType == SIMPLIFIED_CHINESE
				? simplified
				: traditional;
	}
}