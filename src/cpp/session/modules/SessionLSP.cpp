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
};

std::map<lsp::DocumentUri, Document> s_openDocuments;

std::string uriFromDocumentPath(const std::string& path)
{
   return fmt::format("file://{}", path);
}

std::string uriFromDocumentId(const std::string& id)
{
   return fmt::format("rstudio-document://{}", id);
}

std::string uriFromDocumentImpl(const std::string& id,
                                const std::string& path,
                                bool isUntitled)
{
   FilePath resolvedPath = module_context::resolveAliasedPath(path);
   return isUntitled ? uriFromDocumentId(id) : uriFromDocumentPath(resolvedPath.getAbsolutePath());
}

std::string uriFromDocument(const boost::shared_ptr<source_database::SourceDocument>& pDoc)
{
   return uriFromDocumentImpl(pDoc->id(), pDoc->path(), pDoc->isUntitled());
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

int64_t versionFromDocument(const boost::shared_ptr<source_database::SourceDocument>& pDoc)
{
   std::string uri = uriFromDocument(pDoc);
   return s_openDocuments.count(uri) ? s_openDocuments[uri].version : 0;
}

void onDocAdded(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   s_openDocuments[uriFromDocument(pDoc)].version = 0;

   TextDocumentItem textDocument {
      .uri        = uriFromDocument(pDoc),
      .languageId = languageIdFromDocument(pDoc),
      .version    = versionFromDocument(pDoc),
      .text       = pDoc->contents(),
   };

   DidOpenTextDocumentParams params {
      .textDocument = textDocument
   };

   events().didOpen(params);
}

void onDocContentsChanged(boost::shared_ptr<source_database::SourceDocument> pDoc,
                  std::string replacement,
                  int offset,
                  int length)
{
   s_openDocuments[uriFromDocument(pDoc)].version += 1;

   Range range = createRange(
      core::string_utils::offsetToPosition(pDoc->contents(), offset),
      core::string_utils::offsetToPosition(pDoc->contents(), offset + length));

   std::vector<TextDocumentContentChangeEvent> contentChanges;
   contentChanges.push_back({
      .range = range,
      .text  = replacement,
   });

   VersionedTextDocumentIdentifier textDocument {
      .uri     = uriFromDocument(pDoc),
      .version = versionFromDocument(pDoc),
   };

   DidChangeTextDocumentParams params {
      .textDocument   = textDocument,
      .contentChanges = contentChanges,
   };

   events().didChange(params);
}

void onDocRemovedImpl(const std::string& uri)
{
   s_openDocuments.erase(uri);

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
   std::string uri = uriFromDocument(pDoc);
   if (s_openDocuments.count(uri))
      return;

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
   for (auto&& entry : s_openDocuments)
      uris.push_back(entry.first);

   for (auto&& uri : uris)
      onDocRemovedImpl(uri);
}

} // end anonymous namespace

core::Error documentFromUri(
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


Error initialize()
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocContentsChanged.connect(onDocContentsChanged);
   source_database::events().onDocPendingRemove.connect(onDocRemoved);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocReopened.connect(onDocReopened);
   source_database::events().onRemoveAll.connect(onRemoveAll);

   return Success();
}

} // end namespace lsp
} // end namespace modules
} // end namespace session
} // end namespace rstudio
