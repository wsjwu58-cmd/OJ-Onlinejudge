package com.oj.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.lang.reflect.Field;
import java.util.Collection;


public class MarkdownUtil {

    private MarkdownUtil() {} // 私有构造函数，防止实例化

    /**
     * 解析实体类中指定的 Markdown 属性，并将结果设置到对应的 HTML 属性
     *
     * @param entity              要处理的实体对象
     * @param markdownFieldName   实体中存储 Markdown 文本的属性名 (e.g., "contentMd")
     * @param htmlFieldName       实体中用于存储 HTML 文本的属性名 (e.g., "contentHtml")
     * @param parser              用于解析 Markdown 的 Parser 实例
     * @param renderer            用于渲染 HTML 的 HtmlRenderer 实例
     * @throws RuntimeException 如果属性不存在或访问失败
     */
    public static void parseMarkdownField(Object entity, String markdownFieldName, String htmlFieldName, Parser parser, HtmlRenderer renderer) {
        Class<?> clazz = entity.getClass();
        try {
            // 获取 Markdown 属性
            Field markdownField = clazz.getDeclaredField(markdownFieldName);
            markdownField.setAccessible(true); // 允许访问私有字段
            Object markdownValue = markdownField.get(entity);

            if (markdownValue instanceof String) {
                String markdownText = (String) markdownValue;

                // 解析 Markdown，传入配置好的 parser 和 renderer
                String htmlText = toHtml(markdownText, parser, renderer);

                // 获取 HTML 属性并设置值
                Field htmlField = clazz.getDeclaredField(htmlFieldName);
                htmlField.setAccessible(true);
                htmlField.set(entity, htmlText);
            } else {
                System.err.println("Warning: Field '" + markdownFieldName + "' is not of type String or is null.");
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field not found: " + markdownFieldName + " or " + htmlFieldName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + markdownFieldName + " or " + htmlFieldName, e);
        }
    }

    /**
     * 解析一个实体对象集合中每个实体的 Markdown 属性
     *
     * @param entities           实体对象集合
     * @param markdownFieldName  Markdown 属性名
     * @param htmlFieldName      HTML 属性名
     * @param parser             Parser 实例
     * @param renderer           HtmlRenderer 实例
     */
    public static <T> void parseMarkdownFields(Collection<T> entities, String markdownFieldName, String htmlFieldName, Parser parser, HtmlRenderer renderer) {
        if(entities == null) return;
        for (T entity : entities) {
            parseMarkdownField(entity, markdownFieldName, htmlFieldName, parser, renderer);
        }
    }

    /**
     * 内部方法，执行 Markdown 到 HTML 的转换
     */
    private static String toHtml(String markdownText, Parser parser, HtmlRenderer renderer) {
        if (markdownText == null || markdownText.isEmpty()) {
            return "";
        }
        Node document = parser.parse(markdownText);
        return renderer.render(document);
    }
}