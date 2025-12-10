/*
 * SessionLSP.cpp
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

#include "SessionLSP.hpp"

#include <session/SessionModuleContext.hpp>

using namespace rstudio;
using namespace rstudio::core;

#define kRStudioDocumentPrefix "rstudio-document://"
#define kFilePrefix "file://"

namespace rstudio {
namespace session {
namespace modules {
namespace lsp {

namespace {

struct Document
{
   int64_t version = 0;
   bool opened = false;
};

std::map<lsp::DocumentUri, Document> s_documents;

// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocumentItem
std::map<std::string, std::string> s_extToLanguageIdMap = {
   { ".abap",  "abap" },
   { ".bash",  "shellscript" },
   { ".bat",   "bat" },
   { ".bib",   "bibtex" },
   { ".c",     "c" },
   { ".cc",    "cpp" },
   { ".clj",   "clojure"},
   { ".coffee","coffeescript" },
   { ".cpp",   "cpp" },
   { ".cs",    "csharp" },
   { ".css",   "css" },
   { ".dart",  "dart" },
   { ".diff",  "diff" },
// { "",       "dockerfile" }, (special handling due to lack of extension)
   { ".erl",   "erlang" },
   { ".etx",   "tex" },
   { ".ex",    "elixir" },
   { ".fs",    "fsharp" },
   { ".go",    "go" },
   { ".groovy","groovy" },
   { ".h",     "c" },
   { ".hbs",   "handlebars" },
   { ".hpp",   "cpp" },
   { ".html",  "html" },
   { ".ini",   "ini" },
   { ".jade",  "jade" },
   { ".java",  "java" },
   { ".js",    "javascript" },
   { ".jsx",   "javascriptreact" },
   { ".json",  "json" },
   { ".less",  "less" },
   { ".lua",   "lua" },
   { ".m",     "objective-c" },
   { ".md",    "markdown" },
   { ".mjs",   "javascript" },
   { ".ps",    "powershell" },
   { ".mk",    "makefile" }, // (special handling for extensionless "makefile" / "Makefile")
   { ".mm",    "objective-cpp" },
   { ".php",   "php" },
   { ".pl",    "perl" },
   { ".pl6",   "perl6" },
   { ".pug",   "jade" },
   { ".py",    "python" },
   { ".qmd",   "quarto" },
   { ".r",     "r" },
   { ".razor", "razor" },
   { ".rb",    "ruby" },
   { ".rmd",   "r" },
   { ".rnb",   "r" },
   { ".rnw",   "r" },
   { ".rs",    "rust" },
   { ".sass",  "sass" },
   { ".sc",    "scala" },
   { ".scala", "scala" },
   { ".scss",  "scss" },
   { ".sh",    "shellscript" },
   { ".shader","shaderlab" },
   { ".sql",   "sql" },
   { ".swift", "swift" },
   { ".tex",   "latex" },
   { ".toml",  "toml" },
   { ".ts",    "typescript" },
   { ".tsx",   "typescriptreact" },
   { ".vb",    "vb" },
   { ".xml",   "xml" },
   { ".xsl",   "xsl" },
   { ".yml",   "yaml" },
};

std::map<std::string, std::string> makeLanguageIdToExtMap()
{
   std::map<std::string, std::string> map;
   for (auto&& entry : s_extToLanguageIdMap)
      map[entry.second] = entry.first;
   return map;
}

std::map<std::string, std::string>& languageIdToExtMap()
{
   static auto instance = makeLanguageIdToExtMap();
   return instance;
}

std::string languageIdFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (pDoc->isRMarkdownDocument() || pDoc->isRFile())
      return "r";

   FilePath docPath(pDoc->path());
   std::string name = docPath.getFilename();
   std::string stem = docPath.getStem();
   if (name == "Makefile" || name == "makefile")
      return "makefile";
   else if (stem == "Dockerfile")
      return "dockerfile";
   
   return boost::algorithm::to_lower_copy(pDoc->type());
}

void onDocAdded(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   std::string uri = uriFromDocument(pDoc);
   auto&& document = s_documents[uri];
   if (document.opened)
      return;

   document.opened = true;

   TextDocumentItem textDocument {
      .uri        = uri,
      .languageId = languageIdFromDocument(pDoc),
      .version    = document.version,
      .text       = pDoc->contents(),
   };

   DidOpenTextDocumentParams params {
      .textDocument = textDocument
   };

   events().didOpen(params);
}

void onDocContentsChanged(
   boost::shared_ptr<source_database::SourceDocument> pDoc,
   std::string replacement,
   int offset,
   int length)
{
   std::string uri = uriFromDocument(pDoc);
   auto&& document = s_documents[uri];
   document.version += 1;

   Range range = createRange(
      core::string_utils::offsetToPosition(pDoc->contents(), offset),
      core::string_utils::offsetToPosition(pDoc->contents(), offset + length));

   std::vector<TextDocumentContentChangeEvent> contentChanges;
   contentChanges.push_back({
      .range = range,
      .text  = replacement,
   });

   VersionedTextDocumentIdentifier textDocument {
      .uri     = uri,
      .version = document.version,
   };

   DidChangeTextDocumentParams params {
      .textDocument   = textDocument,
      .contentChanges = contentChanges,
   };

   events().didChange(params);
}

void onDocRemovedImpl(const std::string& uri)
{
   auto&& document = s_documents[uri];
   if (!document.opened)
      return;

   document.opened = false;

   TextDocumentIdentifier textDocument {
      .uri = uri,
   };

   DidCloseTextDocumentParams params {
      .textDocument = textDocument,
   };

   events().didClose(params);
}

void onDocRemoved(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   onDocRemovedImpl(uriFromDocument(pDoc));
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   onDocAdded(pDoc);
}

void onDocReopened(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   onDocRemoved(pDoc);
   onDocAdded(pDoc);
}

void onRemoveAll()
{
   std::vector<DocumentUri> uris;
   for (auto&& entry : s_documents)
      uris.push_back(entry.first);

   for (auto&& uri : uris)
      onDocRemovedImpl(uri);
}

} // end anonymous namespace

core::Error sourceDocumentFromUri(
   const std::string& uri,
   boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (boost::algorithm::starts_with(uri, kRStudioDocumentPrefix))
   {
      std::string id = uri.substr(strlen(kRStudioDocumentPrefix));
      return source_database::get(id, pDoc);
   }
   else if (boost::algorithm::starts_with(uri, kFilePrefix))
   {
      FilePath path(uri.substr(strlen(kFilePrefix)));

      std::string id;
      Error error = source_database::getId(path, &id);
      if (error)
         return error;

      return source_database::get(id, pDoc);
   }
   else
   {
      return Error(boost::system::errc::protocol_error, ERROR_LOCATION);
   }
}

std::string uriFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (pDoc->isUntitled())
   {
      return fmt::format("{}{}", kRStudioDocumentPrefix, pDoc->id());
   }
   else
   {
      FilePath docPath = module_context::resolveAliasedPath(pDoc->path());
      return uriFromDocumentPath(docPath);
   }
}

std::string uriFromDocumentPath(const core::FilePath path)
{
   std::string absolutePath = path.getAbsolutePath();
   return fmt::format("{}{}", kFilePrefix, absolutePath);
}

std::string languageIdFromExtension(const std::string& ext)
{
   if (s_extToLanguageIdMap.count(ext))
   {
      return s_extToLanguageIdMap[ext];
   }
   else
   {
      return std::string();
   }
}

std::string extensionFromLanguageId(const std::string& languageId)
{
   auto&& map = languageIdToExtMap();
   if (map.count(languageId))
   {
      return map[languageId];
   }
   else
   {
      return std::string();
   }
}

int64_t documentVersionFromUri(const std::string& uri)
{
   return s_documents[uri].version;
}

namespace {

void logEvent(const std::string& event, const core::json::Value& json)
{
   std::cerr << ">>> " << event << std::endl;
   json.writeFormatted(std::cerr);
   std::cerr << std::endl << std::endl;
}

void didOpen(DidOpenTextDocumentParams params)
{
   logEvent("textDocument/didOpen", toJson(params));
}

void didChange(DidChangeTextDocumentParams params)
{
   logEvent("textDocument/didChange", toJson(params));
}

void didClose(DidCloseTextDocumentParams params)
{
   logEvent("textDocument/didClose", toJson(params));
}

} // end anonymous namespace

Error initialize()
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocContentsChanged.connect(onDocContentsChanged);
   source_database::events().onDocPendingRemove.connect(onDocRemoved);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocReopened.connect(onDocReopened);
   source_database::events().onRemoveAll.connect(onRemoveAll);

   events().didOpen.connect(didOpen);
   events().didChange.connect(didChange);
   events().didClose.connect(didClose);

   return Success();
}

} // end namespace lsp
} // end namespace modules
} // end namespace session
} // end namespace rstudio
