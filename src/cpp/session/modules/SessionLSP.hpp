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
#include <core/json/JsonBuilder.hpp>

#include <session/SessionSourceDatabase.hpp>

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
   return JSON {
      { "line", position.line },
      { "character", position.character }
   };
}


struct Range
{
   Position start;
   Position end;
};

inline core::json::Object toJson(const Range& range)
{
   return JSON {
      { "start", toJson(range.start) },
      { "end", toJson(range.end) }
   };
}


struct ClientInfo
{
   std::string name;
   std::string version;
};

inline core::json::Object toJson(const ClientInfo& clientInfo)
{
   return JSON {
      { "name", clientInfo.name },
      { "version", clientInfo.version },
   };
}


struct WorkspaceFolder
{
   std::string uri;
   std::string name;
};

inline core::json::Object toJson(const WorkspaceFolder& folder)
{
   return JSON {
      { "uri", folder.uri },
      { "name", folder.name }
   };
}


struct InitializeParams
{
   int processId;
   ClientInfo clientInfo;
   std::string locale;
   DocumentUri rootUri;
   core::json::Value initializationOptions;
   core::json::Object capabilities;
   std::vector<WorkspaceFolder> workspaceFolders;
};

inline core::json::Object toJson(const InitializeParams& params)
{
   return JSON {
      { "processId", params.processId },
      { "clientInfo", toJson(params.clientInfo) },
      { "locale", JSON::Optional(params.locale) },
      { "rootUri", JSON::Optional(params.rootUri) },
      { "initializationOptions", JSON::Optional(params.initializationOptions) },
      { "capabilities", params.capabilities },
      { "workspaceFolders", JSON::Optional(toJson(params.workspaceFolders)) }
   };
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
   return JSON {
      { "uri", item.uri },
      { "languageId", item.languageId },
      { "version", item.version },
      { "text", item.text }
   };
}


struct TextDocumentIdentifier
{
   DocumentUri uri;
};

inline core::json::Object toJson(const TextDocumentIdentifier& textDocument)
{
   return JSON {
      { "uri", textDocument.uri }
   };
}


struct VersionedTextDocumentIdentifier
{
   DocumentUri uri;
   int64_t version;

   operator TextDocumentIdentifier() const
   {
      return TextDocumentIdentifier {
         .uri = uri,
      };
   }
};

inline core::json::Object toJson(const VersionedTextDocumentIdentifier& textDocument)
{
   return JSON {
      { "uri", textDocument.uri },
      { "version", textDocument.version }
   };
}


struct TextDocumentContentChangeEvent
{
   Range range;
   std::string text;
};

inline core::json::Object toJson(const TextDocumentContentChangeEvent& event)
{
   return JSON {
      { "range", toJson(event.range) },
      { "text", event.text }
   };
}


struct DidOpenTextDocumentParams
{
   TextDocumentItem textDocument;
};

inline core::json::Object toJson(const DidOpenTextDocumentParams& params)
{
   return JSON {
      { "textDocument", toJson(params.textDocument) }
   };
}


struct DidChangeTextDocumentParams
{
   VersionedTextDocumentIdentifier textDocument;
   std::vector<TextDocumentContentChangeEvent> contentChanges;
};

inline core::json::Object toJson(const DidChangeTextDocumentParams& params)
{
   return JSON {
      { "textDocument", toJson(params.textDocument) },
      { "contentChanges", toJson(params.contentChanges) }
   };
}


struct DidCloseTextDocumentParams
{
   TextDocumentIdentifier textDocument;
};

inline core::json::Object toJson(const DidCloseTextDocumentParams& params)
{
   return JSON {
      { "textDocument", toJson(params.textDocument) }
   };
}


// Not part of the LSP specification, but used by Copilot.
struct DidFocusTextDocumentParams
{
   TextDocumentIdentifier textDocument;
};

inline core::json::Object toJson(const DidFocusTextDocumentParams& params)
{
   return JSON {
      { "textDocument", toJson(params.textDocument) }
   };
}


inline Range createRange(const core::collection::Position& start,
                         const core::collection::Position& end)
{
   return {
      .start = {
         .line = start.row,
         .character = start.column,
      },
      .end = {
         .line = end.row,
         .character = end.column,
      }
   };
}

inline Range createRange(const core::collection::Range& other)
{
   return createRange(other.begin(), other.end());
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

core::Error sourceDocumentFromUri(
   const std::string& uri,
   boost::shared_ptr<source_database::SourceDocument> pDoc);

std::string uriFromSourceDocument(boost::shared_ptr<source_database::SourceDocument> pDoc);

std::string uriFromDocumentPath(const core::FilePath path);

core::Error initialize();

} // end namespace lsp
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_LSP_HPP */