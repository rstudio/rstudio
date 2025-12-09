/*
 * SessionLSP.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#ifndef RSTUDIO_SESSION_MODULES_LSP_HPP
#define RSTUDIO_SESSION_MODULES_LSP_HPP

#include <shared_core/Error.hpp>

#include <core/BoostSignals.hpp>
#include <core/collection/Position.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace lsp {

using DocumentUri = std::string;

struct Position
{
   uint64_t line;
   uint64_t character;
};

struct Range
{
   Position start;
   Position end;
};

struct TextDocumentItem
{
   DocumentUri uri;
   std::string languageId;
   int64_t version;
   std::string text;
};

struct TextDocumentIdentifier
{
   DocumentUri uri;
};

struct VersionedTextDocumentIdentifier : public TextDocumentIdentifier
{
   DocumentUri uri;
   int64_t version;
};

struct TextDocumentContentChangeEvent
{
   Range range;
   std::string text;
};

struct DidOpenTextDocumentParams
{
   TextDocumentItem textDocument;
};

struct DidChangeTextDocumentParams
{
   VersionedTextDocumentIdentifier textDocument;
   std::vector<TextDocumentContentChangeEvent> contentChanges;
};

struct DidCloseTextDocumentParams
{
   TextDocumentIdentifier textDocument;
};

struct Events : boost::noncopyable
{
   RSTUDIO_BOOST_SIGNAL<void(DidOpenTextDocumentParams)>   didOpen;
   RSTUDIO_BOOST_SIGNAL<void(DidChangeTextDocumentParams)> didChange;
   RSTUDIO_BOOST_SIGNAL<void(DidCloseTextDocumentParams)>  didClose;
};

inline Events& events()
{
   static Events instance;
   return instance;
}

core::Error initialize();

inline Range createRange(const core::collection::Position& start,
                         const core::collection::Position& end)
{
   Range range;

   range.start.line = start.row;
   range.start.character = start.column;

   range.end.line = end.row;
   range.end.character = end.column;

   return range;
}

inline Range createRange(const core::collection::Range& other)
{
   auto&& start = other.begin();
   auto&& end = other.end();

   Range range;

   range.start.line = start.row;
   range.start.character = start.column;

   range.end.line = end.row;
   range.end.character = end.column;

   return range;
}

} // end namespace lsp
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_LSP_HPP */