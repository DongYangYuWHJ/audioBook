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
    private static final String NEXT_LINE = "\n";

    // 组合所有分隔符
    private static final String STRONG_SEPARATORS = CN_STRONG_SEPARATORS + EN_STRONG_SEPARATORS + NEXT_LINE;
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

    // 添加 SentenceInfo 类
    public static class SentenceInfo {
        public int startPos;               // 在重组后文本中的起始位置
        public String text;                // 文本内容（包含空格和换行符）

        public SentenceInfo(String text, int startPos) {
            this.text = text;
            this.startPos = startPos;
        }
    }

    // 存储分句结果
    private List<SentenceInfo> sentences;
    private StringBuilder currentSentence;
    private boolean inQuotation = false;
    private char currentQuotationMark = 0;
    private boolean isQuotationEnd = false;
    private int lastProcessedPos = 0;  // 上次处理的位置

    public List<SentenceInfo> segment(String text) {
        List<SentenceInfo> sentenceInfos = new ArrayList<>();
        currentSentence = new StringBuilder();
        inQuotation = false;
        isQuotationEnd = false;
        int currentPos = 0;  // 当前处理位置
        lastProcessedPos = 0;  // 上次处理的位置

        // 预处理文本
        text = preprocessText(text);

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            currentSentence.append(c);

            // 处理引号
            if (QUOTATION_SET.contains(c)) {
                handleQuotation(c, sentenceInfos);
            }

            // 检查是否需要断句
            if (shouldBreakSentence(chars, i)) {
                addSentence(sentenceInfos, lastProcessedPos);
                lastProcessedPos = currentPos + 1;  // 更新上次处理的位置
                isQuotationEnd = false;
            }

            // 处理长句
            if (currentSentence.length() >= MAX_LENGTH) {
                handleLongSentence(sentenceInfos, lastProcessedPos);
                lastProcessedPos = currentPos + 1;  // 更新上次处理的位置
            }
            
            currentPos++;
        }

        // 处理最后一句
        if (currentSentence.length() > 0) {
            addSentence(sentenceInfos, lastProcessedPos);
        }

        // 合并短句
        mergeShortSentences(sentenceInfos);

        // 还原缩写中的点号
        restoreAbbreviations(sentenceInfos);

        // 后处理：过滤掉只包含标点符号的句子
        return postProcessSentences(sentenceInfos);
    }

    private void handleQuotation(char quote, List<SentenceInfo> sentenceInfos) {
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
                    addSentence(sentenceInfos, lastProcessedPos);
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

    private void handleLongSentence(List<SentenceInfo> sentenceInfos, int startPos) {
        char[] chars = currentSentence.toString().toCharArray();
        int lastCommaIndex = -1;
        for (int i = 0; i < chars.length; i++) {
            if (WEAK_SEPARATOR_SET.contains(chars[i])) {
                lastCommaIndex = i;
            }
        }
        if (lastCommaIndex > 0) {
            String firstPart = new String(chars, 0, lastCommaIndex + 1);
            sentenceInfos.add(new SentenceInfo(firstPart, startPos));
            currentSentence = new StringBuilder(new String(chars, lastCommaIndex + 1, chars.length - lastCommaIndex - 1));
            // 更新 lastProcessedPos 为当前句子的起始位置
            lastProcessedPos = startPos + lastCommaIndex + 1;
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
            // 确保不会在句子中间断句
            if (currentPos < chars.length - 1) {
                char nextChar = chars[currentPos + 1];
                // 如果下一个字符是引号或括号的结束，不要在这里断句
                if (QUOTATION_SET.contains(nextChar) || BRACKET_SET.contains(nextChar)) {
                    return false;
                }
            }
            return true;
        }

        // 检查特殊结尾组合
        if (SPECIAL_ENDING_SET.contains(currentChar)) {
            return true;
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

    private void addSentence(List<SentenceInfo> sentenceInfos, int startPos) {
        String originalText = currentSentence.toString();
        if (!originalText.isEmpty()) {
            // 创建新的 SentenceInfo，直接使用 originalText
            sentenceInfos.add(new SentenceInfo(originalText, startPos));
        }
        currentSentence = new StringBuilder();
    }

    private void mergeShortSentences(List<SentenceInfo> sentenceInfos) {
        if (sentenceInfos.size() < 2) return;

        List<SentenceInfo> mergedInfos = new ArrayList<>();
        int i = 0;
        int currentStartPos = 0;  // 跟踪当前合并句子的起始位置
        
        while (i < sentenceInfos.size()) {
            SentenceInfo current = sentenceInfos.get(i);
            StringBuilder mergedText = new StringBuilder(current.text);
            String originalText = current.text;
            int startPos = current.startPos;  // 保持原始的起始位置
            
            boolean merged = false;
            while (i + 1 < sentenceInfos.size()) {
                SentenceInfo next = sentenceInfos.get(i + 1);
                String currentStr = mergedText.toString();
                
                boolean shouldMerge = false;
                // 检查是否需要合并
                if (currentStr.endsWith("：") || currentStr.endsWith(":")) {
                    if (next.text.startsWith("\"") || next.text.startsWith("\"") ||
                        next.text.startsWith("'") || next.text.startsWith("'")) {
                        shouldMerge = true;
                    }
                }
                
                if (!shouldKeepShort(currentStr) &&
                    currentStr.length() < MIN_LENGTH &&
                    currentStr.length() + next.text.length() <= MAX_LENGTH) {
                    shouldMerge = true;
                }
                
                if (shouldMerge) {
                    // 计算两个句子之间的实际间隔
                    int expectedNextStart = startPos + originalText.length();
                    int actualGap = next.startPos - expectedNextStart;
                    String betweenText = "";
                    if (actualGap > 0) {
                        betweenText = " ".repeat(actualGap);
                    }
                    
                    mergedText.append(betweenText).append(next.text);
                    originalText = originalText + betweenText + next.text;
                    i++;
                    merged = true;
                } else {
                    break;
                }
            }
            
            // 添加合并后的句子，保持准确的起始位置
            mergedInfos.add(new SentenceInfo(mergedText.toString(), startPos));
            i++;
        }

        sentenceInfos.clear();
        sentenceInfos.addAll(mergedInfos);
    }

    private void restoreAbbreviations(List<SentenceInfo> sentenceInfos) {
        for (int i = 0; i < sentenceInfos.size(); i++) {
            SentenceInfo info = sentenceInfos.get(i);
            // 替换 "#" 为 "."，保持原始位置不变
            sentenceInfos.set(i, new SentenceInfo(info.text.replace("#", "."), info.startPos));
        }
    }

    private List<SentenceInfo> postProcessSentences(List<SentenceInfo> sentenceInfos) {
        // 直接返回原始的 sentenceInfos，不做过滤
        return sentenceInfos;
        
        /* 暂时注释掉过滤逻辑
        List<SentenceInfo> filteredInfos = new ArrayList<>();
        
        for (SentenceInfo info : sentenceInfos) {
            // 如果句子长度大于等于最小长度，直接保留
            if (info.text.length() >= MIN_LENGTH) {
                filteredInfos.add(info);
                continue;
            }
            
            // 对于短句，检查是否包含中英文字符
            boolean hasChineseOrEnglish = false;
            for (char c : info.text.toCharArray()) {
                // 检查是否是中文字符 (0x4e00-0x9fa5) 或英文字符
                if ((c >= 0x4e00 && c <= 0x9fa5) || 
                    (c >= 'a' && c <= 'z') || 
                    (c >= 'A' && c <= 'Z')) {
                    hasChineseOrEnglish = true;
                    break;
                }
            }
            
            if (hasChineseOrEnglish) {
                filteredInfos.add(info);
            }
        }
        
        return filteredInfos;
        */
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

    public String combineText(List<SentenceInfo> sentences) {
        if (sentences == null || sentences.isEmpty()) {
            return "";
        }
        
        StringBuilder combined = new StringBuilder();
        int currentPos = 0;  // 当前文本的结束位置
        
        for (SentenceInfo sentence : sentences) {
            // 更新句子的起始位置为当前文本的结束位置
            sentence.startPos = currentPos;
            // 添加句子文本
            combined.append(sentence.text);
            // 更新位置
            currentPos += sentence.text.length();
        }
        return combined.toString();
    }
}
