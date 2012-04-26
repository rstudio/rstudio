/*
 * Markdown.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_MARKDOWN_MARKDOWN_HPP
#define CORE_MARKDOWN_MARKDOWN_HPP

#include <string>

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
        superscript(true)
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
};

struct HTMLOptions
{
   HTMLOptions()
      : useXHTML(true),
        hardWrap(true),
        safelink(true),
        smartypants(true),
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
   bool safelink;
   bool smartypants;
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

} // namespace markdown
} // namespace core 

#endif // CORE_MARKDOWN_MARKDOWN_HPP

