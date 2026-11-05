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
   EXPECT_FALSE(isMathJaxRequired(render("  ```r\n$x_1$\n  ```\n")));
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
