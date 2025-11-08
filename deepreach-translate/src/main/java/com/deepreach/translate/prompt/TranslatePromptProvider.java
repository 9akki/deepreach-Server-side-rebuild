package com.deepreach.translate.prompt;

import org.springframework.util.StringUtils;

/**
 * 负责构建与 Python translate_prompt.py 一致的提示词
 */
public final class TranslatePromptProvider {

    private TranslatePromptProvider() {
    }

    public static String build(String text, String targetLang, String sourceLang) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("待翻译文本不能为空");
        }
        if (!StringUtils.hasText(targetLang)) {
            throw new IllegalArgumentException("目标语言不能为空");
        }
        String target = targetLang.trim();
        String body = text.trim();
        return """
You are an exceptionally skilled translator, and you are required to translate the following content into %s.

Below is the text you need to translate:
%s

The response must be returned in JSON format with no other content.
{
    "language":"Returned language",
    "translation":"Content translated into %s"
}
""".formatted(target, body, target);
    }
}
