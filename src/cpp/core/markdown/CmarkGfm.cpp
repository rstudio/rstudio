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
#include <cctype>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <utility>
#include <vector>

#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <shared_core/Error.hpp>

#include <cmark-gfm.h>
#include <cmark-gfm-core-extensions.h>
#include <cmark-gfm-extension_api.h>
#include <cmark-gfm/config.h>

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
   // Markers that the AST pass must still resolve or turn back into text.
   int markers = 0;
   // Pending ^( openers in the block being parsed, so only candidate closers
   // enter the delimiter stack.
   int openers = 0;
   const cmark_node* parent = nullptr;
};

const char* const kBareMarker = "^";
const char* const kParenthesizedMarker = "^(";

cmark_node* newMarker(cmark_syntax_extension* extension, const char* text)
{
   cmark_node* marker = cmark_node_new(CMARK_NODE_CUSTOM_INLINE);
   cmark_node_set_on_enter(marker, text);
   cmark_node_set_syntax_extension(marker, extension);
   return marker;
}

cmark_node* matchSuperscript(cmark_syntax_extension* extension,
                             cmark_parser*,
                             cmark_node* parent,
                             unsigned char character,
                             cmark_inline_parser* inlineParser)
{
   auto* state = static_cast<SuperscriptState*>(cmark_syntax_extension_get_private(extension));
   if (state->parent != parent)
   {
      state->parent = parent;
      state->openers = 0;
   }

   if (character == ')')
   {
      if (state->openers == 0)
         return nullptr;
      --state->openers;
      cmark_node* closer = cmark_node_new(CMARK_NODE_TEXT);
      cmark_node_set_literal(closer, ")");
      cmark_inline_parser_advance_offset(inlineParser);
      cmark_inline_parser_push_delimiter(inlineParser, '^', false, true, closer);
      return closer;
   }

   if (character != '^')
      return nullptr;

   // Markers are custom inlines rather than text: the table extension only
   // admits a fixed set of inline types to cells, and the parser attaches
   // whatever this function returns, whereas later type changes are checked.
   ++state->markers;
   int offset = cmark_inline_parser_get_offset(inlineParser);
   if (cmark_inline_parser_peek_at(inlineParser, offset + 1) == '(')
   {
      // Pair ^( with ) in the delimiter stack, like strikethrough pairs its
      // tildes, so the contents are parsed by the main inline parser along
      // with emphasis, links and reference definitions.
      ++state->openers;
      cmark_node* opener = newMarker(extension, kParenthesizedMarker);
      cmark_inline_parser_set_offset(inlineParser, offset + 2);
      cmark_inline_parser_push_delimiter(inlineParser, '^', true, false, opener);
      return opener;
   }

   // Defer bare superscripts until emphasis and links have been parsed.
   // Scanning to whitespace here would consume their closing delimiters.
   cmark_inline_parser_advance_offset(inlineParser);
   return newMarker(extension, kBareMarker);
}

bool hasImageAncestor(cmark_node* node)
{
   for (cmark_node* parent = cmark_node_parent(node); parent; parent = cmark_node_parent(parent))
   {
      if (cmark_node_get_type(parent) == CMARK_NODE_IMAGE)
         return true;
   }
   return false;
}

delimiter* insertSuperscript(cmark_syntax_extension* extension,
                             cmark_parser*,
                             cmark_inline_parser* inlineParser,
                             delimiter* opener,
                             delimiter* closer)
{
   auto* state = static_cast<SuperscriptState*>(cmark_syntax_extension_get_private(extension));
   delimiter* next = closer->next;
   cmark_node* node = opener->inl_text;

   // Image descriptions render only literal text, so leave the delimiters
   // in place there; the AST pass turns the opener back into text.
   if (!hasImageAncestor(node))
   {
      --state->markers;
      if (cmark_node_next(node) == closer->inl_text)
      {
         // x^() contributes nothing, as with Sundown.
         cmark_node_free(closer->inl_text);
         cmark_node_free(node);
      }
      else
      {
         cmark_node_set_on_enter(node, "<sup>");
         cmark_node_set_on_exit(node, "</sup>");
         cmark_node_set_syntax_extension(node, nullptr);
         for (cmark_node* child = cmark_node_next(node); child != closer->inl_text; )
         {
            cmark_node* following = cmark_node_next(child);
            cmark_node_append_child(node, child);
            child = following;
         }
         cmark_node_free(closer->inl_text);
      }
   }

   for (delimiter* delim = closer; delim != opener; )
   {
      delimiter* previous = delim->previous;
      cmark_inline_parser_remove_delimiter(inlineParser, delim);
      delim = previous;
   }
   cmark_inline_parser_remove_delimiter(inlineParser, opener);
   return next;
}

bool isSuperscript(cmark_node* node)
{
   return cmark_node_get_type(node) == CMARK_NODE_CUSTOM_INLINE &&
          std::strcmp(cmark_node_get_on_enter(node), "<sup>") == 0;
}

Error literalMarker(cmark_node* node)
{
   // Custom inline delimiters are omitted from image alternative text.
   // Unresolved carets must behave like ordinary literal text.
   std::string text = cmark_node_get_on_enter(node);
   if (!cmark_node_set_type(node, CMARK_NODE_TEXT))
      return systemError(boost::system::errc::invalid_argument,
                         "Could not preserve literal caret", ERROR_LOCATION);
   cmark_node_set_syntax_extension(node, nullptr);
   cmark_node_set_literal(node, text.c_str());
   return Success();
}

Error resolveSuperscripts(cmark_node* document, cmark_syntax_extension* extension)
{
   struct Entry
   {
      cmark_node* node;
      int inheritedDepth;
      bool inImage;
   };

   // Cache inherited depth during traversal; walking ancestors per marker
   // would be quadratic in documents with deeply nested block containers.
   std::vector<Entry> nodes;
   int inheritedDepth = 0, imageDepth = 0;
   std::unique_ptr<cmark_iter, decltype(&cmark_iter_free)> iter(
      cmark_iter_new(document), cmark_iter_free);
   while (cmark_iter_next(iter.get()) != CMARK_EVENT_DONE)
   {
      cmark_node* node = cmark_iter_get_node(iter.get());
      bool entering = cmark_iter_get_event_type(iter.get()) == CMARK_EVENT_ENTER;
      if (entering)
         nodes.push_back({node, inheritedDepth, imageDepth > 0});
      if (isSuperscript(node))
         inheritedDepth += entering ? 1 : -1;
      else if (cmark_node_get_type(node) == CMARK_NODE_IMAGE)
         imageDepth += entering ? 1 : -1;
   }
   iter.reset();

   // Process later siblings and descendants first, so nested exponents have a
   // known depth. Reuse the marker in place; see matchSuperscript.
   std::unordered_map<cmark_node*, int> depths;
   std::unordered_map<cmark_node*, int> suffixDepths;
   for (auto it = nodes.rbegin(); it != nodes.rend(); ++it)
   {
      cmark_node* node = it->node;
      if (cmark_node_get_syntax_extension(node) == extension)
      {
         bool bare = std::strcmp(cmark_node_get_on_enter(node), kBareMarker) == 0;
         std::vector<cmark_node*> contents;
         cmark_node* partial = nullptr;
         std::size_t prefixLength = 0;
         int depth = suffixDepths[cmark_node_next(node)] + it->inheritedDepth;
         // Once a suffix reaches the depth limit, earlier markers must not
         // repeatedly walk it. The cached suffix depth keeps this linear.
         for (cmark_node* next = cmark_node_next(node);
              bare && !it->inImage && depth < 16 && next;
              next = cmark_node_next(next))
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
            cmark_node_set_syntax_extension(node, nullptr);
         }
         else
         {
            Error error = literalMarker(node);
            if (error)
               return error;
         }
      }
      int depth = 0;
      for (cmark_node* child = cmark_node_first_child(node); child; child = cmark_node_next(child))
         depth = std::max(depth, depths[child]);
      depths[node] = depth + (isSuperscript(node) ? 1 : 0);
      cmark_node_type type = cmark_node_get_type(node);
      bool boundary = type == CMARK_NODE_SOFTBREAK || type == CMARK_NODE_LINEBREAK ||
         (type == CMARK_NODE_TEXT &&
          std::strpbrk(cmark_node_get_literal(node), " \t\r\n") != nullptr);
      suffixDepths[node] = boundary ? 0 : std::max(depths[node], suffixDepths[cmark_node_next(node)]);
   }
   return Success();
}

bool isWordBoundary(char c)
{
   unsigned char u = static_cast<unsigned char>(c);
   return c == '\0' || std::isspace(u) || std::ispunct(u);
}

bool hasSuffix(const std::string& text, std::size_t pos, const char* suffix)
{
   for (; *suffix; ++suffix, ++pos)
   {
      if (pos >= text.size() || std::tolower(static_cast<unsigned char>(text[pos])) != *suffix)
         return false;
   }
   return true;
}

// Sundown's smartypants also replaced these symbols and fractions; cmark's
// smart punctuation only covers quotes, dashes and ellipses.
std::string smartenSymbols(const std::string& text)
{
   std::string result;
   result.reserve(text.size());
   for (std::size_t i = 0; i < text.size(); ++i)
   {
      char c = text[i];
      if (c == '(')
      {
         if (hasSuffix(text, i + 1, "c)"))
         {
            result += "\xC2\xA9";
            i += 2;
            continue;
         }
         if (hasSuffix(text, i + 1, "r)"))
         {
            result += "\xC2\xAE";
            i += 2;
            continue;
         }
         if (hasSuffix(text, i + 1, "tm)"))
         {
            result += "\xE2\x84\xA2";
            i += 3;
            continue;
         }
      }
      else if ((c == '1' || c == '3') && (i == 0 || isWordBoundary(text[i - 1])) &&
               i + 2 < text.size() && text[i + 1] == '/')
      {
         char denominator = text[i + 2];
         bool boundary = i + 3 == text.size() || isWordBoundary(text[i + 3]);
         const char* replacement = nullptr;
         if (c == '1' && denominator == '2' && boundary)
            replacement = "\xC2\xBD";
         else if (c == '1' && denominator == '4' && (boundary || hasSuffix(text, i + 3, "th")))
            replacement = "\xC2\xBC";
         else if (c == '3' && denominator == '4' && (boundary || hasSuffix(text, i + 3, "ths")))
            replacement = "\xC2\xBE";
         if (replacement)
         {
            result += replacement;
            i += 2;
            continue;
         }
      }
      result.push_back(c);
   }
   return result;
}

void smartenText(cmark_node* document)
{
   int imageDepth = 0;
   std::unique_ptr<cmark_iter, decltype(&cmark_iter_free)> iter(
      cmark_iter_new(document), cmark_iter_free);
   while (cmark_iter_next(iter.get()) != CMARK_EVENT_DONE)
   {
      cmark_node* node = cmark_iter_get_node(iter.get());
      cmark_node_type type = cmark_node_get_type(node);
      bool entering = cmark_iter_get_event_type(iter.get()) == CMARK_EVENT_ENTER;
      if (type == CMARK_NODE_IMAGE)
         imageDepth += entering ? 1 : -1;
      if (type != CMARK_NODE_TEXT || imageDepth > 0)
         continue;
      const char* literal = cmark_node_get_literal(node);
      if (std::strchr(literal, '(') == nullptr && std::strchr(literal, '/') == nullptr)
         continue;
      std::string smartened = smartenSymbols(literal);
      if (smartened != literal)
         cmark_node_set_literal(node, smartened.c_str());
   }
}

// Sundown accepted knitr-style fences such as ```{r} or ```{r, echo=FALSE}
// and used the language inside the braces as the class.
void normalizeFenceInfo(cmark_node* node)
{
   std::string info = cmark_node_get_fence_info(node);
   if (info.empty() || info[0] != '{')
      return;
   std::size_t close = info.find('}');
   if (close == std::string::npos)
      return;
   std::string language = boost::algorithm::trim_copy(info.substr(1, close - 1));
   language = language.substr(0, language.find_first_of(" \t,"));
   if (!language.empty() && language[0] == '.')
      language.erase(0, 1);
   cmark_node_set_fence_info(node, language.c_str());
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
         // Keep this container: table cells reject custom inline children, so
         // moving a superscript out of a link can fail. A transparent renderer
         // preserves all formatted label content without emitting an anchor.
         cmark_node_set_syntax_extension(node, skippedLink);
      }
      else if (type == CMARK_NODE_CODE_BLOCK)
      {
         normalizeFenceInfo(node);
      }
      else if (type == CMARK_NODE_HTML_INLINE || type == CMARK_NODE_HTML_BLOCK)
      {
         if (options.escape)
         {
            // Safe mode omits HTML. Help popups instead need to display the
            // literal tags. The renderer escapes text nodes, and a block
            // becomes a paragraph of text, as Sundown's HTML_ESCAPE produced.
            std::string literal = cmark_node_get_literal(node);
            boost::algorithm::trim_right_if(literal, boost::algorithm::is_any_of("\r\n"));
            std::unique_ptr<cmark_node, decltype(&cmark_node_free)> text(
               cmark_node_new(CMARK_NODE_TEXT), cmark_node_free);
            cmark_node_set_literal(text.get(), literal.c_str());
            std::unique_ptr<cmark_node, decltype(&cmark_node_free)> replacement(
               type == CMARK_NODE_HTML_INLINE ? text.release() : cmark_node_new(CMARK_NODE_PARAGRAPH),
               cmark_node_free);
            if (text && !cmark_node_append_child(replacement.get(), text.get()))
               return systemError(boost::system::errc::invalid_argument,
                                  "Could not escape raw HTML block", ERROR_LOCATION);
            text.release();
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
   // Like Sundown's HTML_SAFELINK, 'safelink' only filters link targets; raw
   // HTML is removed or escaped by filterNodes for skipHTML and escape.
   if (!options.escape && !options.skipHTML)
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
      cmark_syntax_extension_set_inline_from_delim_func(superscript.get(), insertSuperscript);
      cmark_llist* specialChars = cmark_llist_append(cmark_get_default_mem_allocator(), nullptr,
                                                     reinterpret_cast<void*>('^'));
      specialChars = cmark_llist_append(cmark_get_default_mem_allocator(), specialChars,
                                        reinterpret_cast<void*>(')'));
      cmark_syntax_extension_set_special_inline_chars(superscript.get(), specialChars);
      cmark_parser_attach_syntax_extension(parser.get(), superscript.get());
   }

   cmark_parser_feed(parser.get(), input.data(), input.size());
   std::unique_ptr<cmark_node, decltype(&cmark_node_free)> document(
      cmark_parser_finish(parser.get()), cmark_node_free);
   if (!document)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   if (superscriptState.markers > 0)
   {
      Error error = resolveSuperscripts(document.get(), superscript.get());
      if (error)
         return error;
   }

   if (options.smartypants)
      smartenText(document.get());

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
