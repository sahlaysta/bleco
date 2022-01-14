package com.github.sahlaysta.blecodict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import com.github.sahlaysta.cccedict.CCCEDICTEntry;

final class EntrySorter {
	
	//Sort dictionary entries by word frequency
	static final void sort(
			List<CCCEDICTEntry> cccedict,
			File tatoebaFile)
				throws IOException {
		
		//parse chinese sentences
		String[] chineseSentences =
			parseTatoeba(tatoebaFile);
		
		/* Counts how many time each Chinese
		 * character is used in sentences */
		MultiValueMap<Integer, String> mvm
			= new MultiValueMap<>(
				new HashMap<>(),
				new MultiValueMap
				.CollectionManager<String>() {
					@Override
					public Collection<String>
								newCollection() {
						return new LinkedHashSet<>();
					}
				});
		for (String s: chineseSentences)
			for (int i = 0; i < s.codePointCount(0, s.length()); i++)
				//iterate string codepoints
				mvm.put(s.codePointAt(i), s);
		
		//Score entries by word frequency
		List<ScoredCCCEDICTEntry> scoredDict
			= new ArrayList<>(cccedict.size());
		for (int i = 0; i < cccedict.size(); i++)
			scoredDict.add(
				new ScoredCCCEDICTEntry(
					cccedict.get(i),
					score(cccedict.get(i), mvm)));
		Collections.sort(scoredDict,
			new Comparator<ScoredCCCEDICTEntry>() {
				@Override
				public int compare(
						ScoredCCCEDICTEntry o1,
						ScoredCCCEDICTEntry o2) {
					return o2.score - o1.score;
				}
		});
		
		//Put output
		cccedict.clear();
		for (int i = 0; i < scoredDict.size(); i++)
			cccedict.add(scoredDict.get(i).entry);
	}
	
	private static final class ScoredCCCEDICTEntry {
		//CCCEDICTEntry with word frequency score
		final CCCEDICTEntry entry;
		final int score;
		public ScoredCCCEDICTEntry(
				CCCEDICTEntry entry,
				int score) {
			this.entry = entry;
			this.score = score;
		}
	}
	
	private static final int score(
			CCCEDICTEntry entry,
			MultiValueMap<Integer, String> mvm) {
		/* Word frequency score is computed
		 * as number of its example sentences
		 * occurences times 100, plus its
		 * number of definitions */
		
		int score = 0;
		if (entry.simplified.length() == 1) {
			score += (100 * (mvm.getCount(entry
					.simplified.codePointAt(0))
				+ mvm.getCount(entry
					.traditional.codePointAt(0))))
				+ entry.definitions.size();
		}
		else {
			int count = 0;
			Iterable<String> it = mvm.get(
				entry.simplified.codePointAt(0));
			if (it != null)
				for (String s: it)
					if (s.contains(entry.simplified))
						count++;
			
			it = mvm.get(
					entry.traditional.codePointAt(0));
			if (it != null)
				for (String s: it)
					if (s.contains(entry.traditional))
						count++;
			count *= 100;
			score += count + entry.definitions.size();
		}
		
		//higher score for neutral tone + single-syllable
		if (entry.pronunciation.endsWith("5") &&
				!entry.pronunciation.contains(" "))
			score += 5;
		//lower score of names/proper nouns
		if (Character.isUpperCase(
				entry.pronunciation.charAt(0)))
			score -= 5;
		
		//special cases
		if (entry.simplified.equals("什")
				&& entry.definitions.get(0)
				.equals("what"))
			score += 5;
		else if (entry.simplified.equals("没")
				&& entry.definitions.get(0)
				.equals("(negative prefix for verbs)"))
			score += 5;
		else if (entry.simplified.equals("听")
				&& entry.definitions.get(0)
				.equals("to listen"))
			score += 20000;
		else if (entry.simplified.equals("着")
				&& entry.definitions.get(0)
				.startsWith("aspect particle"))
			score += 5;
		else if (entry.simplified.equals("厂")
				&& entry.definitions.get(0)
				.equals("factory"))
			score += 20000;
		else if (entry.simplified.equals("后")
				&& entry.definitions.get(0)
				.equals("back"))
			score += 130000;
		
		return score;
		
	}
	
	
	//parse tatoeba.tsv
	private static final
	String[] parseTatoeba(File tatoebaFile)
			throws IOException {
		List<String> result = new ArrayList<>();
		BufferedReader br
			= new BufferedReader(
				new FileReader(
					tatoebaFile));
		while (true) {
			
			//skip sentence tags
			while (br.read() != '\t');
			while (br.read() != '\t');
			
			result.add(br.readLine());
			if (br.read() == -1)
				break;
		}
		br.close();
		
		String[] resultArray = new String[result.size()];
		for (int i = 0; i < resultArray.length; i++)
			resultArray[i] = result.get(i);
		return resultArray;
	}
}