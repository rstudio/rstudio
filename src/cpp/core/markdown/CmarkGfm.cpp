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

#include <algorithm>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <unordered_map>
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

std::mutex& parserMutex()
{
   static std::mutex mutex;
   return mutex;
}

void attachGfmExtensions(cmark_parser* parser, const Extensions& extensions)
{
   cmark_gfm_core_extensions_ensure_registered();
   for (const auto& extension : {std::make_pair("table", extensions.tables),
                                 std::make_pair("strikethrough", extensions.strikethrough),
                                 std::make_pair("autolink", extensions.autolink)})
   {
      if (extension.second)
         cmark_parser_attach_syntax_extension(parser, cmark_find_syntax_extension(extension.first));
   }
}

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
   if (!parenthesized)
   {
      // Defer bare superscripts until emphasis and links have been parsed.
      // Scanning to whitespace here would consume their closing delimiters.
      cmark_node* marker = cmark_node_new(CMARK_NODE_CUSTOM_INLINE);
      cmark_node_set_on_enter(marker, "^");
      cmark_node_set_syntax_extension(marker, extension);
      cmark_inline_parser_advance_offset(inlineParser);
      return marker;
   }
   ++begin;
   int end = begin;
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
   cmark_inline_parser_set_offset(inlineParser, end + 1);
   return node;
}

bool isSuperscript(cmark_node* node)
{
   return cmark_node_get_type(node) == CMARK_NODE_CUSTOM_INLINE &&
          std::string(cmark_node_get_on_enter(node)) == "<sup>";
}

Error resolveSuperscripts(cmark_node* document, cmark_syntax_extension* extension)
{
   // Cache inherited depth during traversal; walking ancestors per marker
   // would be quadratic in documents with deeply nested block containers.
   std::vector<std::pair<cmark_node*, int>> nodes;
   int inheritedDepth = 0;
   std::unique_ptr<cmark_iter, decltype(&cmark_iter_free)> iter(
      cmark_iter_new(document), cmark_iter_free);
   while (cmark_iter_next(iter.get()) != CMARK_EVENT_DONE)
   {
      cmark_node* node = cmark_iter_get_node(iter.get());
      if (cmark_iter_get_event_type(iter.get()) == CMARK_EVENT_ENTER)
      {
         nodes.emplace_back(node, inheritedDepth);
         if (isSuperscript(node))
            ++inheritedDepth;
      }
      else if (isSuperscript(node))
      {
         --inheritedDepth;
      }
   }
   iter.reset();

   // Process later siblings and descendants first, so nested exponents have a
   // known depth. Reuse the marker in place; table cells reject new custom
   // inline children, but the parser has already placed this marker there.
   std::unordered_map<cmark_node*, int> depths;
   std::unordered_map<cmark_node*, int> suffixDepths;
   for (auto it = nodes.rbegin(); it != nodes.rend(); ++it)
   {
      cmark_node* node = it->first;
      if (cmark_node_get_syntax_extension(node) == extension)
      {
         std::vector<cmark_node*> contents;
         cmark_node* partial = nullptr;
         std::size_t prefixLength = 0;
         int depth = suffixDepths[cmark_node_next(node)] + it->second;
         // Once a suffix reaches the depth limit, earlier markers must not
         // repeatedly walk it. The cached suffix depth keeps this linear.
         for (cmark_node* next = cmark_node_next(node); depth < 16 && next; next = cmark_node_next(next))
         {
            cmark_node_type type = cmark_node_get_type(next);
            if (type == CMARK_NODE_SOFTBREAK || type == CMARK_NODE_LINEBREAK)
               break;
            if (type == CMARK_NODE_TEXT)
            {
               std::string text = cmark_node_get_literal(next);
               std::size_t space = text.find_first_of(" \t\r\n");
               if (space != std::string::npos)
               {
                  partial = next;
                  prefixLength = space;
                  break;
               }
            }
            contents.push_back(next);
         }
         if ((!contents.empty() || prefixLength) && depth < 16)
         {
            for (cmark_node* child : contents)
            {
               if (!cmark_node_append_child(node, child))
                  return systemError(boost::system::errc::invalid_argument,
                                     "Could not attach superscript content", ERROR_LOCATION);
            }
            if (prefixLength)
            {
               std::string text = cmark_node_get_literal(partial);
               std::unique_ptr<cmark_node, decltype(&cmark_node_free)> prefix(
                  cmark_node_new(CMARK_NODE_TEXT), cmark_node_free);
               cmark_node_set_literal(prefix.get(), text.substr(0, prefixLength).c_str());
               if (!cmark_node_append_child(node, prefix.get()))
                  return systemError(boost::system::errc::invalid_argument,
                                     "Could not attach superscript text", ERROR_LOCATION);
               prefix.release();
               cmark_node_set_literal(partial, text.substr(prefixLength).c_str());
            }
            cmark_node_set_on_enter(node, "<sup>");
            cmark_node_set_on_exit(node, "</sup>");
         }
         else
         {
            // Custom inline delimiters are omitted from image alternative
            // text. Unresolved carets must behave like ordinary literal text.
            if (!cmark_node_set_type(node, CMARK_NODE_TEXT))
               return systemError(boost::system::errc::invalid_argument,
                                  "Could not preserve literal caret", ERROR_LOCATION);
            cmark_node_set_syntax_extension(node, nullptr);
            cmark_node_set_literal(node, "^");
         }
      }
      int depth = 0;
      for (cmark_node* child = cmark_node_first_child(node); child; child = cmark_node_next(child))
         depth = std::max(depth, depths[child]);
      depths[node] = depth + (isSuperscript(node) ? 1 : 0);
      cmark_node_type type = cmark_node_get_type(node);
      bool boundary = type == CMARK_NODE_SOFTBREAK || type == CMARK_NODE_LINEBREAK ||
         (type == CMARK_NODE_TEXT &&
          std::string(cmark_node_get_literal(node)).find_first_of(" \t\r\n") != std::string::npos);
      suffixDepths[node] = boundary ? 0 : std::max(depths[node], suffixDepths[cmark_node_next(node)]);
   }
   return Success();
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

void renderSkippedLink(cmark_syntax_extension*,
                       cmark_html_renderer*,
                       cmark_node*,
                       cmark_event_type,
                       int)
{
   // Render the children normally, without emitting an anchor around them.
}

Error filterNodes(cmark_node* document,
                  const HTMLOptions& options,
                  cmark_syntax_extension* skippedLink)
{
   // Collect before mutating, and visit children before parents so deleting an
   // image also safely discards any inline markup in its alternative text.
   std::vector<cmark_node*> nodes;
   int linkDepth = 0;
   std::unique_ptr<cmark_iter, decltype(&cmark_iter_free)> iter(
      cmark_iter_new(document), cmark_iter_free);
   while (cmark_iter_next(iter.get()) != CMARK_EVENT_DONE)
   {
      cmark_node* node = cmark_iter_get_node(iter.get());
      if (cmark_iter_get_event_type(iter.get()) == CMARK_EVENT_ENTER)
      {
         // Superscript contents use a fresh inline parser, which can create
         // links inside an enclosing label. Keep the outer destination and
         // render any inner link's children without a nested anchor.
         if (cmark_node_get_type(node) == CMARK_NODE_LINK && linkDepth++ > 0)
            cmark_node_set_syntax_extension(node, skippedLink);
         nodes.push_back(node);
      }
      else if (cmark_node_get_type(node) == CMARK_NODE_LINK)
      {
         --linkDepth;
      }
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
         // Keep this container: table cells reject custom inline children, so
         // moving a superscript out of a link can fail. A transparent renderer
         // preserves all formatted label content without emitting an anchor.
         cmark_node_set_syntax_extension(node, skippedLink);
      }
      else if (type == CMARK_NODE_HTML_INLINE || type == CMARK_NODE_HTML_BLOCK)
      {
         if (options.escape)
         {
            // Safe mode omits HTML. Help popups instead need to display the
            // literal tags, while retaining Markdown-generated formatting.
            std::unique_ptr<cmark_node, decltype(&cmark_node_free)> replacement(
               cmark_node_new(type == CMARK_NODE_HTML_INLINE ? CMARK_NODE_TEXT : CMARK_NODE_CUSTOM_BLOCK),
               cmark_node_free);
            if (type == CMARK_NODE_HTML_INLINE)
               cmark_node_set_literal(replacement.get(), cmark_node_get_literal(node));
            else
            {
               std::string escaped = string_utils::htmlEscape(cmark_node_get_literal(node));
               cmark_node_set_on_enter(replacement.get(), escaped.c_str());
            }
            if (!cmark_node_replace(node, replacement.get()))
               return systemError(boost::system::errc::invalid_argument,
                                  "Could not replace raw HTML node", ERROR_LOCATION);
            replacement.release();
            cmark_node_free(node);
         }
         else if (options.skipHTML)
         {
            cmark_node_free(node);
         }
      }
   }
   return Success();
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
   std::lock_guard<std::mutex> lock(parserMutex());

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
   std::unique_ptr<cmark_syntax_extension, decltype(&freeExtension)> skippedLink(
      cmark_syntax_extension_new("rstudio-skipped-link"), freeExtension);
   cmark_syntax_extension_set_html_render_func(skippedLink.get(), renderSkippedLink);
   std::unique_ptr<cmark_parser, decltype(&cmark_parser_free)> parser(
      cmark_parser_new(flags), cmark_parser_free);
   if (!parser)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   attachGfmExtensions(parser.get(), extensions);

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

   if (extensions.superscript)
   {
      Error error = resolveSuperscripts(document.get(), superscript.get());
      if (error)
         return error;
   }

   Error error = filterNodes(document.get(), options, skippedLink.get());
   if (error)
      return error;
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
