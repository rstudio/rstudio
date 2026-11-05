/*
 * CmarkGfm.cpp
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

#include "CmarkGfm.hpp"

#include <cstdlib>
#include <memory>
#include <mutex>
#include <utility>
#include <vector>

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>

#include <cmark-gfm.h>
#include <cmark-gfm-core-extensions.h>
#include <cmark-gfm-extension_api.h>
#include <cmark-gfm/config.h>
#include <chunk.h>
#include <inlines.h>
#include <parser.h>

namespace rstudio {
namespace core {
namespace markdown {
namespace {

struct SuperscriptState
{
   int depth = 0;
   const cmark_node* parent = nullptr;
   bool unmatchedParenthesis = false;
};

cmark_node* matchSuperscript(cmark_syntax_extension* extension,
                           cmark_parser* parser,
                           cmark_node* parent,
                           unsigned char character,
                           cmark_inline_parser* inlineParser)
{
   auto* state = static_cast<SuperscriptState*>(cmark_syntax_extension_get_private(extension));
   if (character != '^' || state->depth >= 16)
      return nullptr;

   if (state->parent != parent)
   {
      state->parent = parent;
      state->unmatchedParenthesis = false;
   }

   cmark_chunk* chunk = cmark_inline_parser_get_chunk(inlineParser);
   int offset = cmark_inline_parser_get_offset(inlineParser);
   int begin = offset + 1;
   if (begin >= chunk->len)
      return nullptr;

   bool parenthesized = chunk->data[begin] == '(';
   if (parenthesized)
      ++begin;
   int end = begin;
   if (parenthesized)
   {
      if (state->unmatchedParenthesis)
         return nullptr;
      while (end < chunk->len && chunk->data[end] != ')')
         ++end;
      if (end == chunk->len)
      {
         // Avoid repeatedly scanning a long suffix containing unmatched ^(.
         state->unmatchedParenthesis = true;
         return nullptr;
      }
   }
   else
   {
      while (end < chunk->len && chunk->data[end] != ' ' &&
             chunk->data[end] != '\t' && chunk->data[end] != '\r' && chunk->data[end] != '\n')
         ++end;
      if (end == begin)
         return nullptr;
   }

   cmark_node* node = cmark_node_new(CMARK_NODE_CUSTOM_INLINE);
   cmark_node_set_on_enter(node, end == begin ? "" : "<sup>");
   cmark_node_set_on_exit(node, end == begin ? "" : "</sup>");
   std::string content(reinterpret_cast<const char*>(chunk->data + begin), end - begin);
   cmark_node_set_string_content(node, content.c_str());

   // Parse the contents as inlines using the same extensions and reference
   // definitions. This also keeps code spans, escaped carets, links and math
   // out of the superscript scanner, unlike preprocessing with a regex.
   SuperscriptState previous = *state;
   ++state->depth;
   cmark_parse_inlines(parser, node, parser->refmap, parser->options);
   *state = previous;
   cmark_inline_parser_set_offset(inlineParser, end + (parenthesized ? 1 : 0));
   return node;
}

void freeExtension(cmark_syntax_extension* extension)
{
   cmark_syntax_extension_free(cmark_get_default_mem_allocator(), extension);
}

// Preserve the allowlist used for custom help. cmark's safe mode also permits
// relative links and unknown schemes; it is not equivalent to HTML_SAFELINK.
bool isSafeLink(const std::string& url)
{
   for (const std::string prefix : {"/", "http://", "https://", "ftp://", "mailto:"})
   {
      if (url.size() > prefix.size() && boost::algorithm::istarts_with(url, prefix))
      {
         char next = url[prefix.size()];
         if ((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z') ||
             (next >= '0' && next <= '9'))
            return true;
      }
   }
   return false;
}

void filterNodes(cmark_node* document, const HTMLOptions& options)
{
   // Collect before mutating, and visit children before parents so deleting an
   // image also safely discards any inline markup in its alternative text.
   std::vector<cmark_node*> nodes;
   std::unique_ptr<cmark_iter, decltype(&cmark_iter_free)> iter(
      cmark_iter_new(document), cmark_iter_free);
   while (cmark_iter_next(iter.get()) != CMARK_EVENT_DONE)
   {
      if (cmark_iter_get_event_type(iter.get()) == CMARK_EVENT_ENTER)
         nodes.push_back(cmark_iter_get_node(iter.get()));
   }
   iter.reset();

   for (auto it = nodes.rbegin(); it != nodes.rend(); ++it)
   {
      cmark_node* node = *it;
      cmark_node_type type = cmark_node_get_type(node);
      if (type == CMARK_NODE_IMAGE && options.skipImages)
      {
         cmark_node_free(node);
      }
      else if (type == CMARK_NODE_LINK &&
               (options.skipLinks ||
                (options.safelink && !isSafeLink(cmark_node_get_url(node)))))
      {
         // Keep the formatted label, but make a rejected target non-clickable.
         while (cmark_node* child = cmark_node_first_child(node))
            cmark_node_insert_before(node, child);
         cmark_node_free(node);
      }
      else if (type == CMARK_NODE_HTML_INLINE || type == CMARK_NODE_HTML_BLOCK)
      {
         if (options.escape)
         {
            // Safe mode omits HTML. Help popups instead need to display the
            // literal tags, while retaining Markdown-generated formatting.
            std::string escaped = string_utils::htmlEscape(cmark_node_get_literal(node));
            cmark_node* replacement = cmark_node_new(
               type == CMARK_NODE_HTML_INLINE ? CMARK_NODE_CUSTOM_INLINE : CMARK_NODE_CUSTOM_BLOCK);
            cmark_node_set_on_enter(replacement, escaped.c_str());
            cmark_node_replace(node, replacement);
            cmark_node_free(node);
         }
         else if (options.skipHTML)
         {
            cmark_node_free(node);
         }
      }
   }
}

} // anonymous namespace

Error renderCmarkGfm(const std::string& input,
                     const Extensions& extensions,
                     const HTMLOptions& options,
                     std::string* pOutput)
{
   // These non-default settings have no callers in RStudio. Fail explicitly
   // instead of silently promising Sundown semantics.
   if (options.toc || options.skipStyle || !options.useXHTML ||
       !extensions.fencedCode || !extensions.spaceHeaders ||
       !extensions.laxSpacing || !extensions.noIntraEmphasis)
   {
      return systemError(boost::system::errc::not_supported,
                         "Unsupported legacy Markdown option in cmark-gfm",
                         ERROR_LOCATION);
   }

   // Both registration and inline parsing mutate upstream global tables of
   // special characters. Serialize access even though each parser is local.
   static std::mutex mutex;
   std::lock_guard<std::mutex> lock(mutex);
   cmark_gfm_core_extensions_ensure_registered();

   int flags = CMARK_OPT_VALIDATE_UTF8 | CMARK_OPT_STRIKETHROUGH_DOUBLE_TILDE;
   if (options.smartypants)
      flags |= CMARK_OPT_SMART;
   if (options.hardWrap)
      flags |= CMARK_OPT_HARDBREAKS;
   // Presentations and HTML previews intentionally accept authored raw HTML.
   // Help callers opt into escaping, safe links and image removal together.
   if (!options.safelink)
      flags |= CMARK_OPT_UNSAFE;

   SuperscriptState superscriptState;
   std::unique_ptr<cmark_syntax_extension, decltype(&freeExtension)> superscript(
      nullptr, freeExtension);
   std::unique_ptr<cmark_parser, decltype(&cmark_parser_free)> parser(
      cmark_parser_new(flags), cmark_parser_free);
   if (!parser)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   for (const auto& extension : {std::make_pair("table", extensions.tables),
                                 std::make_pair("strikethrough", extensions.strikethrough),
                                 std::make_pair("autolink", extensions.autolink)})
   {
      if (extension.second)
         cmark_parser_attach_syntax_extension(
            parser.get(), cmark_find_syntax_extension(extension.first));
   }

   if (extensions.superscript)
   {
      superscript.reset(cmark_syntax_extension_new("rstudio-superscript"));
      cmark_syntax_extension_set_private(superscript.get(), &superscriptState, nullptr);
      cmark_syntax_extension_set_match_inline_func(superscript.get(), matchSuperscript);
      cmark_syntax_extension_set_special_inline_chars(
         superscript.get(),
         cmark_llist_append(cmark_get_default_mem_allocator(), nullptr,
                            reinterpret_cast<void*>('^')));
      cmark_parser_attach_syntax_extension(parser.get(), superscript.get());
   }

   cmark_parser_feed(parser.get(), input.data(), input.size());
   std::unique_ptr<cmark_node, decltype(&cmark_node_free)> document(
      cmark_parser_finish(parser.get()), cmark_node_free);
   if (!document)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   filterNodes(document.get(), options);
   std::unique_ptr<char, decltype(&std::free)> html(
      cmark_render_html(document.get(), flags, cmark_parser_get_syntax_extensions(parser.get())),
      std::free);
   if (!html)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   *pOutput = html.get();
   return Success();
}

} // namespace markdown
} // namespace core
} // namespace rstudio
