package org.ruyisdk.news.util;

import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Utility class for rendering Markdown text to HTML.
 */
public final class MarkdownRenderer {

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private static final String HTML_TEMPLATE = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta charset="UTF-8">
                    <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        padding: 10px;
                        margin: 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%%;
                        margin: 10px 0;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f4f4f4;
                    }
                    a {
                        color: #0066cc;
                    }
                    code {
                        background-color: #f4f4f4;
                        padding: 2px 4px;
                        border-radius: 3px;
                        font-family: monospace;
                    }
                    pre {
                        background-color: #f4f4f4;
                        padding: 10px;
                        border-radius: 5px;
                        overflow-x: auto;
                    }
                    pre code {
                        padding: 0;
                        background-color: transparent;
                    }
                    blockquote {
                        border-left: 4px solid #ddd;
                        margin: 0;
                        padding-left: 16px;
                        color: #666;
                    }
                    </style>
                    </head>
                    <body>
                    %s
                    </body>
                    </html>
                    """;

    /**
     * Renders Markdown text to a complete HTML document.
     *
     * @param markdown the Markdown text to render
     * @return a complete HTML document with rendered content
     */
    public static String renderToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return String.format(HTML_TEMPLATE, "");
        }
        final var document = PARSER.parse(markdown);
        final var htmlContent = HTML_RENDERER.render(document);
        return String.format(HTML_TEMPLATE, htmlContent);
    }
}
