package com.github.sahlaysta.blecodict;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import com.github.sahlaysta.cccedict.CCCEDICTEntry;

/* The indexed pinyin search data. Indexes the
 * first two letters of the pinyin
 * of every entry */
final class IndexedPinyinSearchData {
	
	static final void
		writeData(
			DataOutputStream dos,
			List<CCCEDICTEntry> cccedict)
						throws IOException {

		/* MultiValueMap, maps the substring of
		 * first 2 characters of every entry,
		 * to the int index of
		 * the CCCEDICTEntry in the list
		 * for example the pinyin entry
		 * "ni3 hao3" to "ni" */
		MultiValueMap<String, Integer> mvm
		= new MultiValueMap<>(
			new HashMap<>(),
			new MultiValueMap
			.CollectionManager<Integer>() {
				@Override
				public Collection<Integer>
							newCollection() {
					return new LinkedHashSet<>();
				}
			});
		for (int i = 0; i < cccedict.size(); i++) {
			CCCEDICTEntry e = cccedict.get(i);
			if (e.pronunciation.length() < 2)
					continue;
			
			String key = e.pronunciation.substring(0, 2)
									.toLowerCase();
			mvm.put(key, i);
			
			/* Map pinyin with tones, for example
			 * the pinyin entry 'a1 fu4',
			 * to both 'af' and 'a1' */
			if (e.pronunciation.length() > 3) {
				switch (key.charAt(1)) {
				case '1': case '2': case '3':
				case '4': case '5':
					key = new String(
							new char[] {
									key.charAt(0),
									e.pronunciation
										.charAt(3) })
							.toLowerCase();
					mvm.put(key, i);
				}
			}
			if (e.pronunciation.length() > 2) {
				if (key.charAt(1) == ' ') {
					key = new String(
							new char[] {
									key.charAt(0),
									e.pronunciation
										.charAt(2) })
							.toLowerCase();
					mvm.put(key, i);
				}
			}
		}
		
		
		//Write data
		dos.writeChar(mvm.getNodeCount());
		for (MultiValueMap.MVMNode
				<String, Collection<Integer>>
				node: mvm) {
			BlecoDict.writeString(dos, node.key);
			dos.writeChar(node.value.size());
			for (int i: node.value)
				dos.writeInt(i);
		}
	}
}