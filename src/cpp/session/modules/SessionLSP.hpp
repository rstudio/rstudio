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
#include <shared_core/json/Json.hpp>

#include <core/BoostSignals.hpp>
#include <core/collection/Position.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace lsp {

template <typename T>
core::json::Array toJson(const std::vector<T>& values)
{
   core::json::Array json;
   for (auto&& value : values)
      json.push_back(toJson(value));
   return json;
}


using DocumentUri = std::string;

struct Position
{
   uint64_t line;
   uint64_t character;
};

inline core::json::Object toJson(const Position& position)
{
   core::json::Object json;
   json["line"] = position.line;
   json["character"] = position.character;
   return json;
}


struct Range
{
   Position start;
   Position end;
};

inline core::json::Object toJson(const Range& range)
{
   core::json::Object json;
   json["start"] = toJson(range.start);
   json["end"] = toJson(range.end);
   return json;
}


struct TextDocumentItem
{
   DocumentUri uri;
   std::string languageId;
   int64_t version;
   std::string text;
};

inline core::json::Object toJson(const TextDocumentItem& item)
{
   core::json::Object json;
   json["uri"] = item.uri;
   json["languageId"] = item.languageId;
   json["version"] = item.version;
   json["text"] = item.text;
   return json;
}


struct TextDocumentIdentifier
{
   DocumentUri uri;
};

inline core::json::Object toJson(const TextDocumentIdentifier& textDocument)
{
   core::json::Object json;
   json["uri"] = textDocument.uri;
   return json;
}


struct VersionedTextDocumentIdentifier : public TextDocumentIdentifier
{
   DocumentUri uri;
   int64_t version;
};

inline core::json::Object toJson(const VersionedTextDocumentIdentifier& textDocument)
{
   core::json::Object json;
   json["uri"] = textDocument.uri;
   json["version"] = textDocument.version;
   return json;
}


struct TextDocumentContentChangeEvent
{
   Range range;
   std::string text;
};

inline core::json::Object toJson(const TextDocumentContentChangeEvent& event)
{
   core::json::Object json;
   json["range"] = toJson(event.range);
   json["text"] = event.text;
   return json;
}


struct DidOpenTextDocumentParams
{
   TextDocumentItem textDocument;
};

inline core::json::Object toJson(const DidOpenTextDocumentParams& params)
{
   core::json::Object json;
   json["textDocument"] = toJson(params.textDocument);
   return json;
}


struct DidChangeTextDocumentParams
{
   VersionedTextDocumentIdentifier textDocument;
   std::vector<TextDocumentContentChangeEvent> contentChanges;
};

inline core::json::Object toJson(const DidChangeTextDocumentParams& params)
{
   core::json::Object json;
   json["textDocument"] = toJson(params.textDocument);
   json["contentChanges"] = toJson(params.contentChanges);
   return json;
}


struct DidCloseTextDocumentParams
{
   TextDocumentIdentifier textDocument;
};

inline core::json::Object toJson(const DidCloseTextDocumentParams& params)
{
   core::json::Object json;
   json["textDocument"] = toJson(params.textDocument);
   return json;
}


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

} // end namespace lsp
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_LSP_HPP */