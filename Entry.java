package bleco;

import java.io.IOException;
import java.util.List;

import bleco.CompiledDictionary.TatoebaParser.ExampleSentence;
import bleco.GUI.SearchEntry;

class Entry {
	//Entry tuple class
	final static List<ExampleSentence> EXAMPLE_SENTENCES = parseTatoeba();
	final SearchEntry searchEntry;
	final String simplified, traditional, styledPinyin, toStringSimplified, toStringTraditional;
	final String[] definitions;
	final int[] exampleSentences;
	Entry(
			String traditional,
			String simplified,
			String[] definitions,
			SearchEntry searchEntry,
			String styledPinyin,
			String toStringSimplified,
			String toStringTraditional,
			int[] exampleSentences
		) {
			this.simplified = simplified;
			this.traditional = traditional;
			this.definitions = definitions;
			this.searchEntry = searchEntry;
			this.styledPinyin = styledPinyin;
			this.toStringSimplified = toStringSimplified;
			this.toStringTraditional = toStringTraditional;
			this.exampleSentences = exampleSentences;
		}
	static String styleDefinition(String definition, boolean simplified) {
		/*
		 * Split the '|' separator in a definition String.
		 * 
		 * Example:
		 * definition = "Linnei township in Yunlin county 雲林縣|云林县, Taiwan"
		 * output (simplified == true) = "Linnei township in Yunlin county 云林县, Taiwan"
		 * output (simplified == false) = "Linnei township in Yunlin county 雲林縣, Taiwan"
		 */
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < definition.length(); i++) {
			char c = definition.charAt(i);
			if (c == '|') {
				if (simplified) {
					int sbLength = sb.length();
					int traditionalStartIndex = sbLength - 1;
					while (sb.charAt(traditionalStartIndex--) > 125) {
						if (traditionalStartIndex < 0) {
							traditionalStartIndex=-2;
							break;
						}
					}
					sb.replace(traditionalStartIndex += 2, sbLength, "");
					continue;
				} else {
					do {
						if (i >= definition.length() - 1) {
							i = definition.length();
							break;
						} 
					} while (definition.charAt(++i) > 125);
					i--;
					continue;
				}
			}
			if (c == '[') {
				while (definition.charAt(++i) != ']');
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}
	
	static List<ExampleSentence> parseTatoeba() {
		try {
			return CompiledDictionary.TatoebaParser.parse();
		} catch (IOException e) {
			return null;
		}
	}
}
