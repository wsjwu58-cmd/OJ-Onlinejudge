package com.oj.common.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.lang.reflect.Field;
import java.util.Collection;

public class MarkdownUtil {

    private MarkdownUtil() {}

    public static String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    public static void parseMarkdownField(Object entity, String markdownFieldName, String htmlFieldName, Parser parser, HtmlRenderer renderer) {
        Class<?> clazz = entity.getClass();
        try {
            Field markdownField = clazz.getDeclaredField(markdownFieldName);
            markdownField.setAccessible(true);
            Object markdownValue = markdownField.get(entity);

            if (markdownValue instanceof String markdownText) {
                String htmlText = toHtml(markdownText, parser, renderer);
                Field htmlField = clazz.getDeclaredField(htmlFieldName);
                htmlField.setAccessible(true);
                htmlField.set(entity, htmlText);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field not found: " + markdownFieldName + " or " + htmlFieldName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + markdownFieldName + " or " + htmlFieldName, e);
        }
    }

    public static <T> void parseMarkdownFields(Collection<T> entities, String markdownFieldName, String htmlFieldName, Parser parser, HtmlRenderer renderer) {
        if (entities == null) return;
        for (T entity : entities) {
            parseMarkdownField(entity, markdownFieldName, htmlFieldName, parser, renderer);
        }
    }

    private static String toHtml(String markdownText, Parser parser, HtmlRenderer renderer) {
        if (markdownText == null || markdownText.isEmpty()) {
            return "";
        }
        Node document = parser.parse(markdownText);
        return renderer.render(document);
    }
}
