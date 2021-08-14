package bleco;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//ZHConverter by Google: https://code.google.com/archive/p/java-zhconverter/
import com.spreada.utils.chinese.ZHConverter;

import bleco.GUI.SearchEntry;

final class CompiledDictionary {
	//Decompile method ///////////////////////
	/*
	 * Decompile compiledDict.dat, the custom dictionary
	 * file format for Bleco
	 */
	public static Entry[] decompile(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		Entry[] output = new Entry[readIntBig(is)];
		for (int i = 0; i < output.length; i++)
			output[i] = readEntry(bis);
		bis.close();
		return output;
	}
	public static GUI.Index[][] decompileDissectedExampleSentences(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		GUI.Index[][] output = new GUI.Index[readIntBig(is)][];
		for (int i = 0; i < output.length; i++) {
			GUI.Index[] indexArray = new GUI.Index[readInt(is)];
			for (int ii = 0; ii < indexArray.length; ii++)
				indexArray[ii] = new GUI.Index(readInt(is), readInt(is));
			output[i] = indexArray;
		}
		bis.close();
		return output;
	}
	static Entry readEntry(InputStream is) throws IOException {
		return new Entry(
				readString(is),
				readString(is),
				readStringArray(is),
				readSearchEntry(is),
				readString(is),
				readString(is),
				readString(is),
				readExampleSentences(is)
				);
	}
	static SearchEntry readSearchEntry(InputStream is) throws IOException {
		return new SearchEntry(readString(is), readString(is), readString(is));
	}
	static int[] readExampleSentences(InputStream is) throws IOException {
		int[] output = new int[readInt(is)];
		for (int i = 0; i < output.length; i++)
			output[i] = readInt(is);
		return output;
	}
	static GUI.Index[] readDissectedExampleSentences(InputStream is) throws IOException {
		GUI.Index[] output = new GUI.Index[readInt(is)];
		for (int i = 0; i < output.length; i++) {
			GUI.Index index = new GUI.Index(readInt(is), readInt(is));
			output[i] = index;
		}
		return output;
	}
	static String readString(InputStream is) throws IOException {
		byte[] bytes = new byte[readInt(is)];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte)is.read();
		return new String(bytes, StandardCharsets.UTF_8);
	}
	static String[] readStringArray(InputStream is) throws IOException {
		String[] output = new String[readInt(is)];
		for (int i = 0; i < output.length; i++)
			output[i] = readString(is);
		return output;
	}
	static int readInt(InputStream is) throws IOException {
		return ByteBuffer.wrap(new byte[]
				{ (byte)is.read(), (byte)is.read() } )
				.getChar();
	}
	static int readIntBig(InputStream is) throws IOException {
		return ByteBuffer.wrap(new byte[]
				{ (byte)is.read(), (byte)is.read(), (byte)is.read(), (byte)is.read() } )
				.getInt();
	}
	
	//Compile method ////////////////////////
	/*
	 * Compile compiledDict.dat
	 * Method to compile a cedict_ts.u8 file and tatoeba.tsv
	 * to create custom dictionary file format for Bleco
	 */
	
	//
	static void compileDissectedExampleSentences(File outputFile, GUI.Index[][] indexes) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
		writeIntBig(bos, indexes.length);
		for (GUI.Index[] indexArray: indexes) {
			writeInt(bos, indexArray.length);
			for (GUI.Index index: indexArray) {
				writeInt(bos, index.start);
				writeInt(bos, index.end);
			}
		}
		bos.close();
	}
	static void writeStringArray(OutputStream os, String[] strings) throws IOException {
		writeInt(os, strings.length);
		for (String str: strings)
			writeString(os, str);
	}
	static void writeSearchEntry(OutputStream os, SearchEntry se) throws IOException {
		writeString(os, se.pinyin);
		writeString(os, se.pinyinTone);
		writeString(os, se.pinyinRaw);
	}
	static void writeExampleSentences(OutputStream os, int[] es) throws IOException {
		writeInt(os, es.length);
		for (int e: es)
			writeInt(os, e);
	}
	static void writeDissectedExampleSentences(OutputStream os, GUI.Index[] ind) throws IOException {
		writeInt(os, ind.length);
		for (GUI.Index index: ind) {
			writeInt(os, index.start);
			writeInt(os, index.end);
		}
	}
	static void writeString(OutputStream os, String str) throws IOException {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		writeInt(os, bytes.length);
		os.write(bytes);
	}
	static void writeInt(OutputStream os, int num) throws IOException {
		os.write(ByteBuffer.allocate(2).putChar((char)num).array());
	}
	static void writeIntBig(OutputStream os, int num) throws IOException {
		os.write(ByteBuffer.allocate(4).putInt(num).array());
	}
	
	//Parse tatoeba.tsv
	static final class TatoebaParser {
		public static List<ExampleSentence> parse() throws IOException {
			return new TatoebaReader().output;
		}
		public static class ExampleSentence {
			public String english, simplified, traditional;
		}
		
		private static final class TatoebaReader {
			final List<ExampleSentence> output;
			TatoebaReader() throws IOException {
				output = parseTatoeba();
			}
			
			//Reader methods
			ZHConverter simpConverter = ZHConverter.getInstance(ZHConverter.SIMPLIFIED),
					tradConverter = ZHConverter.getInstance(ZHConverter.TRADITIONAL);
			int read;
			boolean delimiter;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					TatoebaReader.class.getResourceAsStream("resources/tatoeba.tsv"),
					StandardCharsets.UTF_8));
			List<ExampleSentence> parseTatoeba() throws IOException {
				List<ExampleSentence> output = new ArrayList<>();
				read(); read();
				while (read != -1)
					output.add(readExampleSentence());
				return output;
			}
			void read() throws IOException {
				switch (read = br.read()) {
				case '\t': delimiter = true; break;
				case '\r': read(); break;
				case '\n': delimiter = true; break;
				default: delimiter = false; break;
				}
			}
			ExampleSentence readExampleSentence() throws IOException {
				ExampleSentence es = new ExampleSentence();
				readString();
				String chinese = readString();
				es.simplified = simpConverter.convert(chinese);
				es.traditional = tradConverter.convert(chinese);
				readString();
				es.english = readString();
				return es;
			}
			String readString() throws IOException {
				StringBuilder sb = new StringBuilder();
				while (!delimiter) {
					sb.append((char)read);
					read();
				}
				read();
				return sb.toString();
			}
		}
	}
}
