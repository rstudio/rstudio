/*
 * Markdown.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/markdown/Markdown.hpp>

#include <iostream>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include "sundown/markdown.h"
#include "sundown/html.h"

namespace core {
namespace markdown {

namespace {

class SundownBuffer : boost::noncopyable
{
public:
   explicit SundownBuffer(std::size_t unit = 128)
      : pBuff_(NULL)
   {
      pBuff_ = ::bufnew(unit);
   }

   explicit SundownBuffer(const std::string& str)
   {
      pBuff_ = ::bufnew(str.length());
      if (pBuff_ != NULL)
      {
         if (grow(str.length()) == BUF_OK)
         {
            put(str);
         }
         else
         {
            ::bufrelease(pBuff_);
            pBuff_ = NULL;
         }
      }
   }

   ~SundownBuffer()
   {
      if (pBuff_)
         ::bufrelease(pBuff_);
   }

   // COPYING: prohibited (boost::noncopyable)

   bool allocated() const { return pBuff_ != NULL; }

   int grow(std::size_t size)
   {
      return ::bufgrow(pBuff_, size);
   }

   void put(const std::string& str)
   {
      ::bufput(pBuff_, str.data(), str.length());
   }

   uint8_t* data() const
   {
      return pBuff_->data;
   }

   std::size_t size() const
   {
      return pBuff_->size;
   }

   const char* c_str() const
   {
      return ::bufcstr(pBuff_);
   }

   operator buf*() const
   {
      return pBuff_;
   }

private:
   friend class SundownMarkdown;
   buf* pBuff_;
};

class SundownMarkdown : boost::noncopyable
{
public:
   SundownMarkdown(unsigned int extensions,
                   size_t maxNesting,
                   const struct sd_callbacks* pCallbacks,
                   void *pOpaque)
      : pMD_(NULL)
   {
      pMD_ = ::sd_markdown_new(extensions, maxNesting,  pCallbacks, pOpaque);
   }

   ~SundownMarkdown()
   {
      if (pMD_)
         ::sd_markdown_free(pMD_);
   }

   // COPYING: prohibited (boost::noncopyable)

   bool allocated() const { return pMD_ != NULL; }

   void render(const SundownBuffer& input, SundownBuffer* pOutput)
   {
      ::sd_markdown_render(pOutput->pBuff_,
                           input.pBuff_->data,
                           input.pBuff_->size,
                           pMD_);
   }

private:
   struct sd_markdown* pMD_;
};

Error allocationError(const ErrorLocation& location)
{
   return systemError(boost::system::errc::not_enough_memory, location);
}

Error renderMarkdown(const SundownBuffer& inputBuffer,
                     const Extensions& extensions,
                     bool smartypants,
                     struct sd_callbacks* pHtmlCallbacks,
                     struct html_renderopt* pHtmlOptions,
                     std::string* pOutput)
{
   // render markdown
   const int kMaxNesting = 16;
   int mdExt = 0;
   if (extensions.noIntraEmphasis)
      mdExt |= MKDEXT_NO_INTRA_EMPHASIS;
   if (extensions.tables)
      mdExt |= MKDEXT_TABLES;
   if (extensions.fencedCode)
      mdExt |= MKDEXT_FENCED_CODE;
   if (extensions.autolink)
      mdExt |= MKDEXT_AUTOLINK;
   if (extensions.strikethrough)
      mdExt |= MKDEXT_STRIKETHROUGH;
   if (extensions.laxHTMLBlocks)
      mdExt |= MKDEXT_LAX_HTML_BLOCKS;
   if (extensions.spaceHeaders)
      mdExt |= MKDEXT_SPACE_HEADERS;
   if (extensions.superscript)
      mdExt |= MKDEXT_SUPERSCRIPT;

   SundownMarkdown md(mdExt, kMaxNesting, pHtmlCallbacks, pHtmlOptions);
   if (!md.allocated())
      return allocationError(ERROR_LOCATION);
   SundownBuffer outputBuffer;
   md.render(inputBuffer, &outputBuffer);

   // do smartypants substitution if requested
   if (smartypants)
   {
      SundownBuffer smartyBuffer;
      if (!smartyBuffer.allocated())
         return allocationError(ERROR_LOCATION);

      ::sdhtml_smartypants(smartyBuffer,
                           outputBuffer.data(),
                           outputBuffer.size());

      *pOutput = smartyBuffer.c_str();
   }
   else
   {
      *pOutput = outputBuffer.c_str();
   }

   return Success();
}

} // anonymous namespace

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const FilePath& markdownFile,
                     const Extensions& extensions,
                     const HTMLOptions& options,
                     const FilePath& htmlFile)
{
   std::string markdownOutput;
   Error error = markdownToHTML(markdownFile,
                                extensions,
                                options,
                                &markdownOutput);
   if (error)
      return error;

   return core::writeStringToFile(htmlFile,
                                  markdownOutput,
                                  string_utils::LineEndingNative);
}

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const FilePath& markdownFile,
                     const Extensions& extensions,
                     const HTMLOptions& options,
                     std::string* pHTMLOutput)
{
   std::string markdownInput;
   Error error = core::readStringFromFile(markdownFile,
                                          &markdownInput,
                                          string_utils::LineEndingPosix);
   if (error)
      return error;

   return markdownToHTML(markdownInput, extensions, options, pHTMLOutput);
}

// render markdown to HTML -- assumes UTF-8 encoding
Error markdownToHTML(const std::string& markdownInput,
                     const Extensions& extensions,
                     const HTMLOptions& options,
                     std::string* pHTMLOutput)

{
   // setup input buffer
   SundownBuffer inputBuffer(markdownInput);
   if (!inputBuffer.allocated())
      return allocationError(ERROR_LOCATION);

   // render table of contents if requested
   if (options.toc)
   {
      struct sd_callbacks htmlCallbacks;
      struct html_renderopt htmlOptions;
      ::sdhtml_toc_renderer(&htmlCallbacks, &htmlOptions);
      std::string tocOutput;
      Error error = renderMarkdown(inputBuffer,
                                   extensions,
                                   options.smartypants,
                                   &htmlCallbacks,
                                   &htmlOptions,
                                   &tocOutput);
      if (error)
         return error;
      pHTMLOutput->append("<div id=\"table_of_contents\">\n");
      pHTMLOutput->append("<h1>Table of Contents</h1>\n");
      pHTMLOutput->append(tocOutput);
      pHTMLOutput->append("</div>\n");
      pHTMLOutput->append("\n");
   }

   // setup html renderer
   struct sd_callbacks htmlCallbacks;
   struct html_renderopt htmlOptions;
   int htmlRenderMode = 0;
   if (options.useXHTML)
      htmlRenderMode |= HTML_USE_XHTML;
   if (options.hardWrap)
      htmlRenderMode |= HTML_HARD_WRAP;
   if (options.toc)
      htmlRenderMode |= HTML_TOC;
   if (options.safelink)
      htmlRenderMode |= HTML_SAFELINK;
   if (options.skipHTML)
      htmlRenderMode |= HTML_SKIP_HTML;
   if (options.skipStyle)
      htmlRenderMode |= HTML_SKIP_STYLE;
   if (options.skipImages)
      htmlRenderMode |= HTML_SKIP_IMAGES;
   if (options.skipLinks)
      htmlRenderMode |= HTML_SKIP_LINKS;
   if (options.escape)
      htmlRenderMode |= HTML_ESCAPE;
   ::sdhtml_renderer(&htmlCallbacks, &htmlOptions, htmlRenderMode);

   // render page
   std::string output;
   Error error = renderMarkdown(inputBuffer,
                                extensions,
                                options.smartypants,
                                &htmlCallbacks,
                                &htmlOptions,
                                &output);
   if (error)
      return error;

   // append output and return success
   pHTMLOutput->append(output);
   return Success();
}

} // namespace markdown
} // namespace core
   



