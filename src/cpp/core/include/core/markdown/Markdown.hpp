/*
 * Markdown.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_MARKDOWN_MARKDOWN_HPP
#define CORE_MARKDOWN_MARKDOWN_HPP

#include <string>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace markdown {

struct Extensions
{
   Extensions()
      : noIntraEmphasis(true),
        tables(true),
        fencedCode(true),
        autolink(true),
        laxSpacing(true),
        spaceHeaders(true),
        strikethrough(true),
        superscript(true),
        ignoreMath(true),
        stripMetadata(true),
        htmlPreserve(false)
   {
   }

   bool noIntraEmphasis;
   bool tables;
   bool fencedCode;
   bool autolink;
   bool laxSpacing;
   bool spaceHeaders;
   bool strikethrough;
   bool superscript;
   bool ignoreMath;
   bool stripMetadata;
   bool htmlPreserve;
};

struct HTMLOptions
{
   HTMLOptions()
      : useXHTML(true),
        hardWrap(false),
        smartypants(true),
        safelink(false),
        toc(false),
        skipHTML(false),
        skipStyle(false),
        skipImages(false),
        skipLinks(false),
        escape(false)
   {
   }
   bool useXHTML;
   bool hardWrap;
   bool smartypants;
   bool safelink;
   bool toc;
   bool skipHTML;
   bool skipStyle;
   bool skipImages;
   bool skipLinks;
   bool escape;
};

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const FilePath& markdownFile,
                     const Extensions& extensions,
                     const HTMLOptions& htmlOptions,
                     const FilePath& htmlFile);

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const FilePath& markdownFile,
                     const Extensions& extensions,
                     const HTMLOptions& htmlOptions,
                     std::string* pHTMLOutput);

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const std::string& markdownInput,
                     const Extensions& extensions,
                     const HTMLOptions& htmlOptions,
                     std::string* pHTMLOutput);


bool isMathJaxRequired(const std::string& htmlOutput);

} // namespace markdown
} // namespace core 
} // namespace rstudio

#endif // CORE_MARKDOWN_MARKDOWN_HPP

