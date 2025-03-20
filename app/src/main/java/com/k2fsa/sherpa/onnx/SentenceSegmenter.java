package com.k2fsa.sherpa.onnx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentenceSegmenter {
    // 配置参数
    private static final int MIN_LENGTH = 3;  // 最小句子长度
    private static final int MAX_LENGTH = 50; // 最大句子长度

    // 中文标点
    private static final String CN_STRONG_SEPARATORS = "。！？";
    private static final String CN_WEAK_SEPARATORS = "，、；：";

    // 英文标点
    private static final String EN_STRONG_SEPARATORS = ".!?";
    private static final String EN_WEAK_SEPARATORS = ",;:";

    // 其他特殊分隔符
    private static final String EN_QUOTATION_MARKS = "\"'";  // 英文引号
    private static final String CN_QUOTATION_MARKS = "\u201C\u201D\u2018\u2019";  // 中文引号
    private static final String QUOTATION_MARKS = EN_QUOTATION_MARKS + CN_QUOTATION_MARKS;
    private static final String BRACKETS = "()（）[]【】{}《》";
    private static final String ELLIPSIS = "...…";
    private static final String DASHES = "——–—";
    private static final String SPECIAL_ENDINGS = "?!！？!？?！";
    private static final String DIALOGUE_MARKS = "—";

    // 组合所有分隔符
    private static final String STRONG_SEPARATORS = CN_STRONG_SEPARATORS + EN_STRONG_SEPARATORS;
    private static final String WEAK_SEPARATORS = CN_WEAK_SEPARATORS + EN_WEAK_SEPARATORS;

    // 英文缩写词列表
    private static final String ENGLISH_ABBREVIATIONS =
            "Mr\\.|Mrs\\.|Dr\\.|Prof\\.|etc\\.|i\\.e\\.|e\\.g\\.|vs\\.|Ph\\.D\\.|U\\.S\\.A\\.|U\\.K\\.";

    // 预编译的正则表达式
    private static final Pattern ENGLISH_CONTEXT_PATTERN = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern TITLE_PATTERN = Pattern.compile(".*[。，！？.!?,;；].*");
    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile(ENGLISH_ABBREVIATIONS);
    private static final Pattern ELLIPSIS_PATTERN = Pattern.compile("\\.{3}");

    // 使用Set存储标点符号，提高查找效率
    private static final Set<Character> STRONG_SEPARATOR_SET = new HashSet<>();
    private static final Set<Character> WEAK_SEPARATOR_SET = new HashSet<>();
    private static final Set<Character> QUOTATION_SET = new HashSet<>();
    private static final Set<Character> BRACKET_SET = new HashSet<>();
    private static final Set<Character> ELLIPSIS_SET = new HashSet<>();
    private static final Set<Character> DASH_SET = new HashSet<>();
    private static final Set<Character> SPECIAL_ENDING_SET = new HashSet<>();
    private static final Set<Character> DIALOGUE_SET = new HashSet<>();

    static {
        // 初始化Set
        for (char c : STRONG_SEPARATORS.toCharArray()) {
            STRONG_SEPARATOR_SET.add(c);
        }
        for (char c : WEAK_SEPARATORS.toCharArray()) {
            WEAK_SEPARATOR_SET.add(c);
        }
        for (char c : QUOTATION_MARKS.toCharArray()) {
            QUOTATION_SET.add(c);
        }
        for (char c : BRACKETS.toCharArray()) {
            BRACKET_SET.add(c);
        }
        for (char c : ELLIPSIS.toCharArray()) {
            ELLIPSIS_SET.add(c);
        }
        for (char c : DASHES.toCharArray()) {
            DASH_SET.add(c);
        }
        for (char c : SPECIAL_ENDINGS.toCharArray()) {
            SPECIAL_ENDING_SET.add(c);
        }
        for (char c : DIALOGUE_MARKS.toCharArray()) {
            DIALOGUE_SET.add(c);
        }
    }

    // 存储分句结果
    private List<String> sentences;
    private StringBuilder currentSentence;
    private boolean inQuotation = false;
    private char currentQuotationMark = 0;
    private boolean isQuotationEnd = false;

    public List<String> segment(String text) {
        sentences = new ArrayList<>();
        currentSentence = new StringBuilder();
        inQuotation = false;
        isQuotationEnd = false;

        // 预处理文本
        text = preprocessText(text);

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            currentSentence.append(c);

            // 处理引号
            if (QUOTATION_SET.contains(c)) {
                handleQuotation(c);
            }

            // 检查是否需要断句
            if (shouldBreakSentence(chars, i)) {
                addSentence();
                isQuotationEnd = false;
            }

            // 处理长句
            if (currentSentence.length() >= MAX_LENGTH) {
                handleLongSentence();
            }
        }

        // 处理最后一句
        if (currentSentence.length() > 0) {
            addSentence();
        }

        // 合并短句
        mergeShortSentences();

        // 还原缩写中的点号
        restoreAbbreviations();

        // 后处理：过滤掉只包含标点符号的句子
        postProcessSentences();

        return sentences;
    }

    private void handleQuotation(char quote) {
        if (!inQuotation) {
            inQuotation = true;
            currentQuotationMark = quote;
            isQuotationEnd = false;
        } else if (isMatchingQuote(quote, currentQuotationMark)) {
            inQuotation = false;
            currentQuotationMark = 0;
            isQuotationEnd = true;
            // 如果引号后是标点，立即断句
            if (currentSentence.length() > 0) {
                char lastChar = currentSentence.charAt(currentSentence.length() - 1);
                if (STRONG_SEPARATOR_SET.contains(lastChar) || SPECIAL_ENDING_SET.contains(lastChar)) {
                    addSentence();
                    isQuotationEnd = false;
                }
            }
        }
    }

    private boolean isMatchingQuote(char current, char previous) {
        return (previous == '"' && current == '"') ||
                (previous == '\u2018' && current == '\u2019') ||
                (previous == '\u201C' && current == '\u201D') ||
                (previous == '\'' && current == '\'');
    }

    private String preprocessText(String text) {
        // 处理特殊情况
        text = ELLIPSIS_PATTERN.matcher(text).replaceAll("…"); // 处理省略号
        text = handleAbbreviations(text);         // 处理缩写
        return text;
    }

    private String handleAbbreviations(String text) {
        Matcher matcher = ABBREVIATION_PATTERN.matcher(text);
        while (matcher.find()) {
            String abbr = matcher.group();
            text = text.replace(abbr, abbr.replace(".", "#"));
        }
        return text;
    }

    private void handleLongSentence() {
        char[] chars = currentSentence.toString().toCharArray();
        int lastCommaIndex = -1;
        for (int i = 0; i < chars.length; i++) {
            if (WEAK_SEPARATOR_SET.contains(chars[i])) {
                lastCommaIndex = i;
            }
        }
        if (lastCommaIndex > 0) {
            sentences.add(new String(chars, 0, lastCommaIndex + 1));
            currentSentence = new StringBuilder(new String(chars, lastCommaIndex + 1, chars.length - lastCommaIndex - 1));
        }
    }

    private boolean shouldBreakSentence(char[] chars, int currentPos) {
        char currentChar = chars[currentPos];

        // 处理省略号
        if (ELLIPSIS_SET.contains(currentChar) || 
            (currentChar == '.' && currentPos >= 2 && 
             chars[currentPos-1] == '.' && chars[currentPos-2] == '.')) {
            return true;
        }

        // 检查是否是强分隔符
        if (STRONG_SEPARATOR_SET.contains(currentChar)) {
            // 检查是否是缩写中的点号
            if (currentChar == '.' && isPartOfAbbreviation(chars, currentPos)) {
                return false;
            }
            return true;
        }

        // 检查特殊结尾组合
        if (SPECIAL_ENDING_SET.contains(currentChar)) {
            return true;
        }

        // 检查引号后的空格或换行
        if (QUOTATION_SET.contains(currentChar) && currentPos < chars.length - 1) {
            char nextChar = chars[currentPos + 1];
            if (nextChar == ' ' || nextChar == '\n') {
                return true;
            }
        }

        return false;
    }

    private boolean isPartOfAbbreviation(char[] chars, int pos) {
        if (pos >= 2) {
            String prevTwo = new String(chars, pos - 2, 3);
            return prevTwo.matches("Mr\\.|Dr\\.|Ms\\.");
        }
        return false;
    }

    private boolean isEnglishContext(String text) {
        return ENGLISH_CONTEXT_PATTERN.matcher(text).matches();
    }

    private void addSentence() {
        String sentence = currentSentence.toString().trim();
        if (!sentence.isEmpty()) {
            sentences.add(sentence);
        }
        currentSentence = new StringBuilder();
    }

    private void mergeShortSentences() {
        if (sentences.size() < 2) return;

        List<String> mergedSentences = new ArrayList<>();
        StringBuilder current = new StringBuilder(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            String nextSentence = sentences.get(i);
            String currentStr = current.toString();

            // 检查是否需要合并
            boolean shouldMerge = false;
            
            // 如果当前句子以冒号结尾，且下一句以引号开头，则合并
            if (currentStr.endsWith("：") || currentStr.endsWith(":")) {
                if (nextSentence.startsWith("\"") || nextSentence.startsWith("\"") ||
                    nextSentence.startsWith("'") || nextSentence.startsWith("'")) {
                    shouldMerge = true;
                }
            }

            // 如果当前句子太短，尝试与下一句合并
            if (!shouldKeepShort(currentStr) &&
                    current.length() < MIN_LENGTH &&
                    current.length() + nextSentence.length() <= MAX_LENGTH) {
                shouldMerge = true;
            }

            if (shouldMerge) {
                current.append(nextSentence);
            } else {
                mergedSentences.add(currentStr);
                current = new StringBuilder(nextSentence);
            }
        }

        // 添加最后一句
        if (current.length() > 0) {
            mergedSentences.add(current.toString());
        }

        sentences = mergedSentences;
    }

    private void restoreAbbreviations() {
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            sentences.set(i, sentence.replace("#", "."));
        }
    }

    private void postProcessSentences() {
        List<String> filteredSentences = new ArrayList<>();
        for (String sentence : sentences) {
            if (!isOnlyPunctuation(sentence)) {
                filteredSentences.add(sentence);
            }
        }
        sentences = filteredSentences;
    }

    private boolean isOnlyPunctuation(String sentence) {
        for (char c : sentence.toCharArray()) {
            if (!Character.isWhitespace(c) && !isPunctuation(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPunctuation(char c) {
        return STRONG_SEPARATOR_SET.contains(c) || 
               WEAK_SEPARATOR_SET.contains(c) || 
               QUOTATION_SET.contains(c) ||
               BRACKET_SET.contains(c) ||
               ELLIPSIS_SET.contains(c) ||
               DASH_SET.contains(c) ||
               SPECIAL_ENDING_SET.contains(c) ||
               DIALOGUE_SET.contains(c);
    }

    private boolean shouldKeepShort(String sentence) {
        // 检查是否是破折号开头的对话
        if (sentence.startsWith("—") || sentence.startsWith("-")) {
            return true;
        }
        // 检查是否可能是标题（没有标点符号且较短）
        if (!TITLE_PATTERN.matcher(sentence).matches() && sentence.length() < 20) {
            return true;
        }
        return false;
    }
}
