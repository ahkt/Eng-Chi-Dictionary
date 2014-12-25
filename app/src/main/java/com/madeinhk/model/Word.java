package com.madeinhk.model;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tony on 8/11/14.
 */
public class Word {
    public String mWord;
    public String mPhoneticString;
    public List<TypeEntry> mTypeEntry;

    public Word(String word, String phoneticString, List<TypeEntry> typeEntry) {
        this.mWord = word;
        this.mPhoneticString = phoneticString;
        this.mTypeEntry = typeEntry;
    }

    public static class TypeEntry {
        public String mMeaning;
        public char mType;
        public String mEngExample;
        public String mChiExample;

        public String getTypeDescription() {
            switch (mType) {
                case '0':
                    return "N/A";
                case '1':
                    return "adv. 副詞";
                case '2':
                    return "pron. 代詞";
                case '3':
                    return "v. 動詞";
                case '4':
                    return "n. 名詞";
                case '5':
                    return "a. 形容詞";
                case '6':
                    return "ad. 副詞";
                case '7':
                    return "vt. 可及物動詞";
                case '8':
                    return "vi. 不及物動詞";
                case '9':
                    return "ph. 片語";
                case 'a':
                    return "adj. 形容詞";
                case 'b':
                    return "int. 感嘆詞";
                case 'c':
                    return "aux. 情態動詞";
                case 'd':
                    return "prep. 介詞";
                case 'e':
                    return "conj. 連詞";
                case 'f':
                    return "interj. 感嘆詞";
                case 'g':
                    return "abbr. 縮寫";
            }
            throw new IllegalArgumentException("No type: " + mType);
        }
    }

    public static Word fromLookupResult(LookupResult lookupResult) {
        String word = lookupResult.getWord();
        String mPhoneticString = lookupResult.getPhoneticString();
        String meaningString = lookupResult.getMeaning();
        String exampleString = lookupResult.getExample();
        Map<Character, String> meaningMap = parseString(meaningString);
        Map<Character, String> exampleMap = parseString(exampleString);

        List<TypeEntry> entries = new ArrayList<TypeEntry>();
        for (Map.Entry<Character, String> entry : meaningMap.entrySet()) {
            TypeEntry tmp = new TypeEntry();
            tmp.mMeaning = entry.getValue();
            String example = exampleMap.get(entry.getKey());
            if (!TextUtils.isEmpty(example)) {
                String[] tokens = example.split("\\^");
                tmp.mEngExample = tokens[0];
                tmp.mChiExample = tokens[1];
            }
            tmp.mType = entry.getKey();
            entries.add(tmp);
        }
        return new Word(word, mPhoneticString, entries);
    }

    enum ParserState {
        START, FIRST_SHARP, SECOND_SHARP, TYPE, DATA, END
    }

    private static String decodeHtmlString(String str) {
        return Html.fromHtml(str).toString();
    }

    private static Map<Character, String> parseString(String str) {
        str = decodeHtmlString(str);
        Map<Character, String> map = new HashMap<Character, String>();
        if (TextUtils.isEmpty(str)) {
            return map;
        }
        int currentIndex = 0;
        ParserState currentState = ParserState.START;
        int length = str.length();
        StringBuilder builder = null;
        char type = 0;
        while (currentState != ParserState.END) {
            char character = str.charAt(currentIndex);
            currentIndex++;
            switch (currentState) {
                case START:
                    if (character == '&') {
                        currentState = ParserState.FIRST_SHARP;
                    } else {
                        throw new IllegalStateException("Expect a &");
                    }
                    break;
                case FIRST_SHARP:
                    if (character == '&') {
                        currentState = ParserState.TYPE;
                    } else {
                        throw new IllegalStateException("Expect a &");
                    }
                    currentState = ParserState.SECOND_SHARP;
                    break;
                case SECOND_SHARP:
                    type = character;
                    currentState = ParserState.TYPE;
                    break;
                case TYPE:
                    builder = new StringBuilder();
                    builder.append(character);
                    currentState = ParserState.DATA;
                    break;
                case DATA:
                    boolean end = currentIndex == length;
                    if (character == '&' || end) {
                        if (end) {
                            builder.append(character);
                        }
                        map.put(type, builder.toString());
                        currentState = ParserState.FIRST_SHARP;
                        if (end) {
                            currentState = ParserState.END;
                        }
                    } else {
                        builder.append(character);
                        currentState = ParserState.DATA;
                    }
                    break;
            }
        }
        return map;
    }


}
