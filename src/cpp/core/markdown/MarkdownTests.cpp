/*
 * MarkdownTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <gtest/gtest.h>
#include <boost/regex.hpp>
#include <future>

#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>
#include <core/markdown/Markdown.hpp>

namespace rstudio {
namespace core {
namespace markdown {
namespace {

std::string render(const std::string& input,
                   const Extensions& extensions = Extensions(),
                   const HTMLOptions& options = HTMLOptions())
{
   std::string html;
   Error error = markdownToHTML(input, extensions, options, &html);
   EXPECT_FALSE(error) << error;
   return html;
}

TEST(MarkdownTest, BasicMarkup)
{
   EXPECT_EQ("<h1>Title</h1>\n", render("# Title\n"));
   EXPECT_EQ("<p><em>emphasis</em> and <strong>strong</strong></p>\n",
             render("*emphasis* and **strong**"));
   EXPECT_EQ("<p>snake_case_name</p>\n", render("snake_case_name"));
   EXPECT_EQ("", render(""));
}

TEST(MarkdownTest, GfmExtensions)
{
   EXPECT_NE(std::string::npos, render("a | b\n--- | ---\nx | y\n").find("<table>"));
   EXPECT_NE(std::string::npos, render("~~deleted~~").find("<del>deleted</del>"));
   EXPECT_NE(std::string::npos, render("https://example.com").find("href=\"https://example.com\""));

   Extensions extensions;
   extensions.tables = extensions.strikethrough = extensions.autolink = false;
   EXPECT_EQ(std::string::npos, render("a | b\n--- | ---\nx | y\n", extensions).find("<table>"));
   EXPECT_EQ(std::string::npos, render("~~deleted~~", extensions).find("<del>"));
   EXPECT_EQ(std::string::npos, render("https://example.com", extensions).find("<a "));
}

TEST(MarkdownTest, Superscripts)
{
   EXPECT_EQ("<p>x<sup>2</sup> and x<sup>a + b</sup></p>\n", render("x^2 and x^(a + b)"));
   EXPECT_EQ("<p>x<sup><em>n</em></sup></p>\n", render("x^(*n*)"));
   EXPECT_EQ("<p>x<sup>y<sup>z</sup></sup></p>\n", render("x^y^z"));
   EXPECT_EQ("<p>x^2 <code>x^2</code></p>\n", render(R"(x\^2 `x^2`)"));
   EXPECT_EQ("<p>x</p>\n", render("x^()"));
   EXPECT_EQ("<p>x^(unclosed</p>\n", render("x^(unclosed"));
   EXPECT_NE(std::string::npos, render("x^([link][ref])\n\n[ref]: https://example.com\n").find("<sup><a href="));

   Extensions extensions;
   extensions.superscript = false;
   EXPECT_EQ("<p>x^2</p>\n", render("x^2", extensions));
}

TEST(MarkdownTest, AuthoredHtmlIsPreserved)
{
   EXPECT_EQ("<p><span class=\"note\">text</span></p>\n",
             render("<span class=\"note\">text</span>"));
   EXPECT_NE(std::string::npos, render("<video src=\"movie.mp4\"></video>\n").find("<video"));
   EXPECT_NE(std::string::npos, render("![plot](plot.png)").find("src=\"plot.png\""));
}

TEST(MarkdownTest, HelpEscapesSourceHtmlAndRemovesImages)
{
   HTMLOptions options;
   options.escape = options.safelink = options.skipImages = true;
   std::string html = render(
      "<b>markup</b> **bold** ![image](https://example.com/a.png)", Extensions(), options);
   EXPECT_NE(std::string::npos, html.find("&lt;b&gt;"));
   EXPECT_NE(std::string::npos, string_utils::htmlUnescape(html).find("<b>markup</b>"));
   EXPECT_NE(std::string::npos, html.find("<strong>bold</strong>"));
   EXPECT_EQ(std::string::npos, html.find("<b>"));
   EXPECT_EQ(std::string::npos, html.find("<img"));

   html = render("<script>alert('x')</script>\n", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<script"));
   EXPECT_NE(std::string::npos, html.find("&lt;script&gt;"));
   html = render("![<b>**image**</b>](x) ^(<i>sup</i>)", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<img"));
   EXPECT_EQ(std::string::npos, html.find("<i>"));
}

TEST(MarkdownTest, HelpRetainsStrictLinkAllowlist)
{
   HTMLOptions options;
   options.escape = options.safelink = options.skipImages = true;
   for (const std::string url : {"relative.html", "#anchor", "//example.com", "javascript:alert%281%29",
                                 "vbscript:msgbox%281%29", "data:text/html,hello", "file:///tmp/a",
                                 "custom:command", "jav&#x61;script:alert%281%29"})
   {
      SCOPED_TRACE(url);
      EXPECT_EQ(std::string::npos, render("[label](" + url + ")", Extensions(), options).find("<a "));
   }
   for (const std::string url : {"https://example.com", "http://example.com", "ftp://example.com",
                                 "mailto:user@example.com", "/help/topic"})
   {
      SCOPED_TRACE(url);
      EXPECT_NE(std::string::npos, render("[label](" + url + ")", Extensions(), options).find("<a "));
   }
}

TEST(MarkdownTest, HelpEscapesRestoredMath)
{
   HTMLOptions options;
   options.escape = options.safelink = options.skipImages = true;
   std::string html = render(R"($<img src=x onerror=alert(1)>$)", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<img"));
   EXPECT_NE(std::string::npos, html.find("&lt;img"));
   EXPECT_TRUE(isMathJaxRequired(html));

   html = render(R"(\[<script>alert(1)</script>\])", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<script"));
   EXPECT_NE(std::string::npos, html.find("&lt;script&gt;"));
}

TEST(MarkdownTest, HelpPreservesEscapedTagsAndFormattedLinkLabelsInTables)
{
   HTMLOptions options;
   options.escape = options.safelink = options.skipImages = true;
   std::string html = render(
      "| column |\n| --- |\n| [<b>x</b>](relative.html) <i>y</i> |\n",
      Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<a "));
   EXPECT_EQ(std::string::npos, html.find("<b>"));
   EXPECT_NE(std::string::npos, string_utils::htmlUnescape(html).find("<b>x</b>"));
   EXPECT_NE(std::string::npos, string_utils::htmlUnescape(html).find("<i>y</i>"));

   html = render("| column |\n| --- |\n| [x^(2)](relative.html) |\n", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<a "));
   EXPECT_NE(std::string::npos, html.find("<sup>2</sup>"));

   options.skipLinks = true;
   html = render("| column |\n| --- |\n| [x^(2)](https://example.com) |\n", Extensions(), options);
   EXPECT_EQ(std::string::npos, html.find("<a "));
   EXPECT_NE(std::string::npos, html.find("<sup>2</sup>"));
}

TEST(MarkdownTest, MetadataAndMath)
{
   EXPECT_EQ("<p>body</p>\n", render("---\ntitle: example\n---\n\nbody\n"));
   EXPECT_EQ("<p>body</p>\n", render("% Title\n% Author\n\nbody\n"));
   std::string html = render(R"($x_1 + x^2$ and $$a_b < c_d$$)");
   EXPECT_NE(std::string::npos, html.find(R"(\( x_1 + x^2 \))"));
   EXPECT_NE(std::string::npos, html.find(R"(\[ a_b < c_d \])"));
   EXPECT_TRUE(isMathJaxRequired(html));
   EXPECT_EQ("<p><code>$x_1$</code></p>\n", render("`$x_1$`"));
   EXPECT_NE(std::string::npos, render("```r\n$x_1$\n```\n").find("$x_1$"));
   EXPECT_FALSE(isMathJaxRequired(render("```r\n$x_1$\n```\n")));
   EXPECT_FALSE(isMathJaxRequired(render("~~~r\n$x_1$\n~~~\n")));
   EXPECT_EQ("<p>\\[ x_1 \\]</p>\n", render("---\ntitle: '$$'\n---\n\n$$x_1$$"));
   EXPECT_EQ("<p>\\[ x_1 \\]</p>\n", render("% $$\n\n$$x_1$$"));
   EXPECT_EQ("<p>body</p>\n", render("---\ntitle: |\n  $$\n\n  x_1\n  $$\n---\n\nbody"));
   EXPECT_EQ("<p>body</p>\n", render("---\ntitle: example\n\nauthor: name\n---\n\nbody"));
   html = render("Note: intro\n\n---\n\nDetail: text\n\n---\n\nBody");
   EXPECT_NE(std::string::npos, html.find("Note: intro"));
   EXPECT_NE(std::string::npos, html.find("Detail: text"));
   EXPECT_NE(std::string::npos, html.find("Body"));
   EXPECT_EQ("<p><code>code</code>    \\( x_1 \\)</p>\n", render("`code`    $x_1$"));
   EXPECT_NE(std::string::npos, render("`code`\n    $x_1$").find("\\( x_1 \\)"));
}

TEST(MarkdownTest, MathAmongManyCodeSpans)
{
   std::string input;
   for (int i = 0; i < 10000; ++i)
      input += "`code`\n\n";
   EXPECT_FALSE(isMathJaxRequired(render(input)));
   input += "$x_1$\n\n```\n$literal$\n```\n\n$x_2$";
   std::string html = render(input);
   EXPECT_NE(std::string::npos, html.find("\\( x_1 \\)"));
   EXPECT_NE(std::string::npos, html.find("\\( x_2 \\)"));
   EXPECT_NE(std::string::npos, html.find("<code>$literal$\n</code>"));
}

TEST(MarkdownTest, OverlappingFencesDoNotRescanTheDocument)
{
   std::string input;
   for (int i = 0; i < 10000; ++i)
      input += "```text\n~~~text\n```\n\n";
   input += "$x_1$";
   EXPECT_NE(std::string::npos, render(input).find("\\( x_1 \\)"));
}

TEST(MarkdownTest, HtmlPreserve)
{
   Extensions extensions;
   extensions.htmlPreserve = true;
   std::string preserved = "<!--html_preserve--><div>**literal**</div><!--/html_preserve-->";
   EXPECT_NE(std::string::npos, render(preserved + "\n", extensions).find(preserved));
}

TEST(MarkdownTest, WrappingAndSmartPunctuation)
{
   HTMLOptions options;
   options.hardWrap = true;
   EXPECT_TRUE(boost::regex_match(render("one\ntwo", Extensions(), options),
                                 boost::regex("<p>one<br\\s*/>\ntwo</p>\n")));
   options.smartypants = false;
   EXPECT_NE(std::string::npos, render("one -- two...", Extensions(), options).find("-- two..."));
   EXPECT_EQ(std::string::npos, render("one -- two...").find("-- two..."));
}

#ifdef RSTUDIO_USE_CMARK_GFM
TEST(MarkdownTest, CommonMarkFencesAndInfoStrings)
{
   EXPECT_EQ("<pre><code class=\"language-r\">x &lt; 2\n</code></pre>\n",
             render("~~~r\nx < 2\n~~~\n"));
   EXPECT_EQ("<p>a<em>b</em>c</p>\n", render("a*b*c"));
   EXPECT_EQ("<p>~literal~</p>\n", render("~literal~"));
}

TEST(MarkdownTest, MathExclusionRespectsCodeDelimiterLengths)
{
   EXPECT_EQ("<pre><code>```\n$x_1$\n</code></pre>\n", render("````\n```\n$x_1$\n````\n"));
   EXPECT_EQ("<pre><code>~~~\n$x_1$\n</code></pre>\n", render("~~~~\n~~~\n$x_1$\n~~~~\n"));
   EXPECT_EQ("<pre><code>$x_1$\n</code></pre>\n", render("````\n$x_1$\n"));
   EXPECT_EQ("<p><code>a ` $x_1$</code></p>\n", render("``a ` $x_1$``"));
   EXPECT_EQ("<p><code>$x_1$</code></p>\n", render("``` $x_1$ ```"));
   EXPECT_EQ("<p>text <code>$x_1$</code></p>\n", render("text ``` $x_1$ ```"));
   EXPECT_TRUE(isMathJaxRequired(render(std::string(256, '`') + "\ncode\n" +
                                        std::string(255, '`') + "\n\n$x_1$")));
   EXPECT_TRUE(isMathJaxRequired(render(std::string(256, '~') + "\ncode\n" +
                                        std::string(255, '~') + "\n\n$x_1$")));
   for (std::size_t count : {80, 81})
   {
      std::string ticks(count, '`');
      std::string html = render("text " + ticks + " $x_1$ " + ticks);
      EXPECT_EQ(count == 81, isMathJaxRequired(html));
      EXPECT_EQ(count == 80, html.find("<code>") != std::string::npos);
   }
}

TEST(MarkdownTest, FenceTextInHtmlDoesNotHideFollowingMath)
{
   for (const std::string html : {"<pre>\n~~~\n</pre>", "<div>\n~~~\n</div>",
                                  "<!--\n~~~\n-->", "<custom>\n~~~\n</custom>",
                                  "<pre>\n```\n</pre>", "<pre>\n~~~</pre>",
                                  "<pre>\n```</pre>", "<!--\n~~~-->",
                                  "<script>\n~~~</script>", "<textarea>\n~~~</textarea>"})
   {
      SCOPED_TRACE(html);
      std::string output = render(html + "\n\n$x_1$");
      EXPECT_NE(std::string::npos, output.find(html));
      EXPECT_NE(std::string::npos, output.find("\\( x_1 \\)"));
   }
   // A real code block containing HTML still ends only at its own closer.
   EXPECT_FALSE(isMathJaxRequired(render("```\n<pre>\n~~~\n</pre>\n$x_1$\n```")));
   EXPECT_FALSE(isMathJaxRequired(render("<pre>\n~~~\n</pre>\n```\n$x_1$\n```")));
   EXPECT_FALSE(isMathJaxRequired(render("<div>\n~~~\n\n```\n$x_1$\n```")));
   EXPECT_NE(std::string::npos,
             render("$$\n<pre>\n$$\n\n~~~\n$x_1$\n~~~\n").find("<code>$x_1$\n</code>"));
   EXPECT_NE(std::string::npos,
             render("<pre>\n~~~</pre>\n\n$$\n<pre>\n$$\n\n~~~\n$x_1$\n~~~\n")
                .find("<code>$x_1$\n</code>"));
   EXPECT_NE(std::string::npos,
             render("<div>\rcontent\r</div>\n\n~~~\n$x_1$\n~~~\n").find("<code>$x_1$\n</code>"));
   std::string output = render("$$\n<pre>\n~~~\n</pre>\n$$\n\n$x_1$");
   EXPECT_NE(std::string::npos, output.find("\\[ \n<pre>\n~~~\n</pre>\n \\]"));
   EXPECT_NE(std::string::npos, output.find("\\( x_1 \\)"));
   EXPECT_EQ("<p>&lt;\\( x_1 \\)&gt;</p>\n<pre><code>$y_1$\n</code></pre>\n",
             render("<$x_1$>\n~~~\n$y_1$\n~~~\n"));
   EXPECT_NE(std::string::npos,
             render("$x_1$.\n<custom>\n~~~\n$y_1$\n~~~\n").find("<code>$y_1$\n</code>"));
}

TEST(MarkdownTest, MathExclusionPreservesExistingBlockBehavior)
{
   for (const std::string input : {"text <span title=\"`\">\n`a $x_1$ b`</span>",
                                   "| h |\n| --- |\n   | `a $x_1$ b` |",
                                   "`a $x_1$ b`\n| h |\n| --- |"})
   {
      SCOPED_TRACE(input);
      EXPECT_NE(std::string::npos, render(input).find("<code>a $x_1$ b</code>"));
   }
   EXPECT_TRUE(isMathJaxRequired(render("> ```\n> code\n$x_1$")));
   EXPECT_TRUE(isMathJaxRequired(render("- item\n\n  ```\n  code\n\n$x_1$")));
   EXPECT_TRUE(isMathJaxRequired(render("- item\n\n  ~~~\n  code\n\n$x_1$")));
   EXPECT_EQ("<pre><code>code\n</code></pre>\n<pre><code>$x_1$\n</code></pre>\n",
             render("~~~\ncode\n~~~\n    $x_1$\n"));
   EXPECT_EQ("<pre><code>code\n</code></pre>\n<pre><code>$x_1$\n</code></pre>\n",
             render("```\ncode\n```\n    $x_1$\n"));
}

TEST(MarkdownTest, SuperscriptsDoNotNestLinks)
{
   EXPECT_EQ("<p><a href=\"https://outer.example\">x<sup>https://inner.example</sup></a></p>\n",
             render("[x^(https://inner.example)](https://outer.example)"));
   EXPECT_EQ("<p><a href=\"/outer\">x<sup>label</sup></a></p>\n",
             render("[x^([label][ref])](/outer)\n\n[ref]: /inner"));
   EXPECT_EQ("<p>x<sup><a href=\"https://inner.example\">https://inner.example</a></sup></p>\n",
             render("x^(https://inner.example)"));
}

TEST(MarkdownTest, BareSuperscriptsRespectEnclosingInlineMarkup)
{
   EXPECT_EQ("<p><strong>x<sup>2</sup></strong></p>\n", render("**x^2**"));
   EXPECT_EQ("<p><em>x<sup>2</sup></em></p>\n", render("*x^2*"));
   EXPECT_EQ("<p><del>x<sup>2</sup></del></p>\n", render("~~x^2~~"));
   EXPECT_EQ("<p><a href=\"/help\">x<sup>2</sup></a></p>\n", render("[x^2](/help)"));
   EXPECT_EQ("<p>x<sup><em>n</em></sup></p>\n", render("x^*n*"));
   EXPECT_EQ("<p><em>done</em> x<sup>2*</sup></p>\n", render("*done* x^2*"));
   EXPECT_EQ("<p>x^ next</p>\n", render("x^ next"));
}

TEST(MarkdownTest, LiteralCaretsSurviveInImageAlternativeText)
{
   EXPECT_NE(std::string::npos, render("![x^ y](plot.png)").find("alt=\"x^ y\""));
   EXPECT_NE(std::string::npos, render("![x^](plot.png)").find("alt=\"x^\""));
   EXPECT_NE(std::string::npos, render("![x\\^2](plot.png)").find("alt=\"x^2\""));
}

TEST(MarkdownTest, SuperscriptsWithinDeepBlockquotes)
{
   std::string input(10000, '>');
   input += " ";
   for (int i = 0; i < 10000; ++i)
      input += "x^2 ";
   EXPECT_NE(std::string::npos, render(input).find("x<sup>2</sup>"));
}

TEST(MarkdownTest, UnsupportedLegacyOptionsAreExplicit)
{
   HTMLOptions options;
   options.toc = true;
   std::string html;
   EXPECT_TRUE(markdownToHTML(std::string("# title"), Extensions(), options, &html));
}

TEST(MarkdownTest, SuperscriptNestingAndUnmatchedDelimitersAreBounded)
{
   std::string nested = "x";
   std::string unmatched;
   for (int i = 0; i < 10000; ++i)
   {
      nested += "^x";
      unmatched += "^(";
   }
   EXPECT_FALSE(render(nested).empty());
   EXPECT_FALSE(render(unmatched).empty());
}

TEST(MarkdownTest, ConcurrentParsersKeepExtensionsSeparate)
{
   std::vector<std::future<void>> renders;
   for (bool enabled : {true, false, true, false})
   {
      renders.push_back(std::async(std::launch::async, [enabled]() {
         Extensions extensions;
         extensions.superscript = extensions.strikethrough = enabled;
         for (int i = 0; i < 25; ++i)
         {
            std::string html = render("x^2 ~~deleted~~", extensions);
            EXPECT_EQ(enabled, html.find("<sup>") != std::string::npos);
            EXPECT_EQ(enabled, html.find("<del>") != std::string::npos);
         }
      }));
   }
   for (auto& result : renders)
      result.get();
}
#endif

} // anonymous namespace
} // namespace markdown
} // namespace core
} // namespace rstudio
