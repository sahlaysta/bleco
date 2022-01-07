package com.github.sahlaysta.bleco.dict;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/* Input stream abstraction that reads
 * from a .bleco file and loads Bleco dictionary */
final class BlecoInputStream extends DataInputStream {

	//Constructor
	BlecoInputStream(InputStream in) {
		super(in);
	}
	
	
	
	//Read methods
	
	//read string
	String readString() throws IOException {
		byte[] b = new byte[readChar()];
		read(b);
		return new String(b, StandardCharsets.UTF_8);
	}
	
	//read example sentence
	ExampleSentence readExampleSentence() throws IOException {
		return new ExampleSentence(readString(), readString());
	}
	ExampleSentence[] readExampleSentences() throws IOException {
		ExampleSentence[] result = new ExampleSentence[readInt()];
		for (int i = 0; i < result.length; i++)
			result[i] = readExampleSentence();
		return result;
	}
	
	//read entry
	Entry readEntry(ExampleSentence[] exampleSentences)
			throws IOException {
		//read strings
		String simplified = readString(),
				traditional = readString(),
				pinyin = readString(),
				formattedPinyin = readString();
		
		//read definitions
		String[] definitions = new String[read()];
		for (int i = 0; i < definitions.length; i++)
			definitions[i] = readString();
		
		//read normalized definitions
		String[] nDefinitions = new String[definitions.length];
		for (int i = 0; i < nDefinitions.length; i++)
			nDefinitions[i] = readString();
		
		//read example sentence indexes, then return entry
		int entryExampleSentencesSize = readInt();
		if (entryExampleSentencesSize == 0)
			return new Entry(
					simplified,
					traditional,
					pinyin,
					formattedPinyin,
					definitions,
					nDefinitions,
					null);
		ExampleSentence[] entryExampleSentences
			= new ExampleSentence[entryExampleSentencesSize];
		for (int i = 0; i < entryExampleSentencesSize; i++)
			entryExampleSentences[i] = exampleSentences[readInt()];
		return new Entry(
				simplified,
				traditional,
				pinyin,
				formattedPinyin,
				definitions,
				nDefinitions,
				entryExampleSentences);
	}
	Entry[] readEntries(ExampleSentence[] exampleSentences)
			throws IOException {
		Entry[] result = new Entry[readInt()];
		for (int i = 0; i < result.length; i++)
			result[i] = readEntry(exampleSentences);
		return result;
	}
	
	//read chinese index map
	Map<Byte, Entry[]> readChineseIndexMap(Entry[] entries)
			throws IOException {
		int size = readChar();
		Map<Byte, Entry[]> result = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			byte key = readByte();
			Entry[] value = new Entry[readChar()];
			for (int j = 0; j < value.length; j++)
				value[j] = entries[readInt()];
			result.put(key, value);
		}
		return result;
	}
	
	//read english index map
	Map<String, Entry[]> readEnglishIndexMap(Entry[] entries)
			throws IOException {
		int size = readChar();
		Map<String, Entry[]> result = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			String key = readString();
			Entry[] value = new Entry[readChar()];
			for (int j = 0; j < value.length; j++)
				value[j] = entries[readInt()];
			result.put(key, value);
		}
		return result;
	}
	
	//read pinyin index map
	Map<String, Entry[]> readPinyinIndexMap(Entry[] entries)
			throws IOException {
		int size = readChar();
		Map<String, Entry[]> result = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			String key = readString();
			Entry[] value = new Entry[readChar()];
			for (int j = 0; j < value.length; j++)
				value[j] = entries[readInt()];
			result.put(key, value);
		}
		return result;
	}
}