/*
 * SessionBookdownXRefs.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionBookdownXRefs.hpp"

#include <boost/lambda/bind.hpp>

#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>

#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/IncrementalFileChangeHandler.hpp>


namespace rstudio {
namespace session {

using namespace rstudio::core;

namespace {

const char * const kBaseDir = "baseDir";
const char * const kRefs = "refs";
const char * const kFile = "file";
const char * const kType = "type";
const char * const kId = "id";
const char * const kSuffix = "suffix";
const char * const kTitle = "title";

bool isBookdownRmd(const FileInfo& fileInfo)
{
   FilePath filePath(fileInfo.absolutePath());
   FilePath bookDir = projects::projectContext().buildTargetPath();
   if (bookDir.exists())
      return filePath.isWithin(bookDir) && (filePath.getExtensionLowerCase() == ".rmd");
   else
      return false;
}

std::vector<std::string> bookdownSourceFiles()
{
   std::vector<std::string> files;
   std::string inputDir = string_utils::utf8ToSystem(projects::projectContext().buildTargetPath().getAbsolutePath());
   Error error = r::exec::RFunction(".rs.bookdown.SourceFiles", inputDir).call(&files);
   if (error)
      LOG_ERROR(error);
   return files;
}


std::string bookRelativePath(const FilePath& rmdFile)
{
   return rmdFile.getRelativePath(projects::projectContext().buildTargetPath());
}

FilePath xrefIndexDirectory()
{
   FilePath xrefsPath = module_context::scopedScratchPath().completeChildPath("bookdown-crossref");
   Error error = xrefsPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return xrefsPath;
}


FilePath xrefIndexFilePath(const std::string& rmdRelativePath)
{
   FilePath indexFilePath = xrefIndexDirectory().completeChildPath(rmdRelativePath + ".xref");
   Error error = indexFilePath.getParent().ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return indexFilePath;
}

FilePath xrefIndexFilePath(const FilePath& rmdFile)
{
   std::string rmdRelativePath = bookRelativePath(rmdFile);
   return xrefIndexFilePath(rmdRelativePath);
}


struct XRefFileIndex
{
   XRefFileIndex() {}
   explicit XRefFileIndex(const std::string& file) : file(file) {}
   std::string file;
   std::vector<std::string> entries;
};

struct XRefIndexEntry
{
   XRefIndexEntry() {}
   XRefIndexEntry(const std::string& file, const std::string& entry)
      : file(file), entry(entry)
   {
   }
   std::string file;
   std::string entry;
};


XRefFileIndex indexForDoc(const std::string& file, const std::string& contents)
{
   // move rmd code chunk preamble *into* chunk (so pandoc parses it as a code block)
   std::vector<std::string> lines;
   boost::algorithm::split(lines, contents, boost::algorithm::is_any_of("\r\n"));
   std::vector<std::string> indexLines;
   boost::regex beginChunkRe("^([\\t >]*)(```+\\s*)(\\{[a-zA-Z0-9_]+( *[ ,].*)?\\}\\s*)$");
   for (auto line : lines) {
      boost::smatch matches;
      if (boost::regex_search(line, matches, beginChunkRe))
      {
         indexLines.push_back(matches[1] + matches[2]);
         indexLines.push_back(matches[1] + matches[3]);
      }
      else
      {
         indexLines.push_back(line);
      }
   }
   std::string indexContents = boost::algorithm::join(indexLines, "\n");

   // build index
   XRefFileIndex index(file);

   // run pandoc w/ custom lua filter to capture index
   std::vector<std::string> args;
   args.push_back("--from");
   args.push_back("markdown");
   args.push_back("--to");
   FilePath resPath = session::options().rResourcesPath();
   FilePath xrefLuaPath = resPath.completePath("xref.lua");
   std::string xrefLua = string_utils::utf8ToSystem(xrefLuaPath.getAbsolutePath());
   args.push_back(xrefLua);
   core::system::ProcessResult result;
   Error error = module_context::runPandoc(args, indexContents, &result);
   if (error)
   {
      LOG_ERROR(error);
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR(systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION));
   }
   else
   {
      boost::algorithm::split(index.entries, result.stdOut, boost::algorithm::is_any_of("\n"));
   }

   // return the index
   return index;
}

XRefFileIndex indexForDoc(const FilePath& filePath, const std::string& contents)
{
   std::string file = bookRelativePath(filePath);
   return indexForDoc(file, contents);
}



XRefFileIndex indexForDoc(const FilePath& filePath)
{
   std::string contents;
   Error error = core::readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   return indexForDoc(filePath, contents);
}


bool writeEntryId(const std::string& id, json::Object* pEntryJson)
{
   std::size_t colonPos = id.find_first_of(':');
   if (colonPos != std::string::npos)
   {
      pEntryJson->operator[](kType) = id.substr(0, colonPos);
      pEntryJson->operator[](kId) = id.substr(colonPos + 1);
      pEntryJson->operator[](kSuffix) = "";
      return true;
   }
   else
   {
      return false;
   }
}


class XRefUnsavedIndex
{
public:

   const std::map<std::string, XRefFileIndex>& unsavedIndexes(){
      return unsavedFiles_;
   }


   void updateUnsaved(const FileInfo& fileInfo, const std::string& contents, bool dirty)
   {
      // always remove to start with
      removeUnsaved(fileInfo);

      // add it back if it's dirty
      if (dirty)
      {
         FilePath filePath = toFilePath(fileInfo);
         XRefFileIndex idx = indexForDoc(filePath, contents);
         unsavedFiles_[bookRelativePath(filePath)] = idx;
      }
   }

   void removeUnsaved(const FileInfo& fileInfo)
   {
      FilePath filePath = toFilePath(fileInfo);
      unsavedFiles_.erase(bookRelativePath(filePath));

   }

   void removeAllUnsaved()
   {
      unsavedFiles_.clear();
   }

private:
   std::map<std::string, XRefFileIndex> unsavedFiles_;
};
XRefUnsavedIndex s_unsavedIndex;

typedef boost::function<bool(const std::string&)> IndexEntryFilter;

std::vector<XRefIndexEntry> indexEntriesForProject(IndexEntryFilter filter)
{
   std::vector<XRefIndexEntry> indexEntries;

   // find out what the docs in the book are
   std::vector<std::string> sourceFiles = bookdownSourceFiles();

   for (std::vector<std::string>::size_type i = 0; i < sourceFiles.size(); i++) {

      // alias source files
      const std::string& sourceFile = sourceFiles[i];

      // prefer unsaved files
      std::vector<std::string> entries;
      auto unsaved = s_unsavedIndex.unsavedIndexes();
      std::map<std::string, XRefFileIndex>::const_iterator it = unsaved.find(sourceFile);
      if (it != unsaved.end())
      {
         entries = it->second.entries;
      }
      // then check the disk based index
      else
      {
         FilePath filePath = xrefIndexFilePath(sourceFile);
         if (filePath.exists())
         {
            Error error = readStringVectorFromFile(filePath, &entries);
            if (error)
               LOG_ERROR(error);
         }
      }

      for (auto entry : entries)
      {
         if (filter(entry))
         {
            XRefIndexEntry indexEntry(sourceFile, entry);
            indexEntries.push_back(indexEntry);
         }
      }
   }

   return indexEntries;
}

std::vector<XRefIndexEntry> indexEntriesForFile(const XRefFileIndex& fileIndex, IndexEntryFilter filter)
{
   std::vector<XRefIndexEntry> indexEntries;
   for (auto entry : fileIndex.entries)
   {
      if (filter(entry))
      {
         XRefIndexEntry indexEntry(fileIndex.file, entry);
         indexEntries.push_back(indexEntry);
      }
   }

   return indexEntries;
}

std::map<std::string,int> readMultiKeys()
{
   std::map<std::string,int> multiKeys;
   FilePath refKeys = projects::projectContext().buildTargetPath().completePath("_book/reference-keys.txt");
   if (refKeys.exists())
   {
      // read the keys
      std::vector<std::string> keys;
      Error error = core::readStringVectorFromFile(refKeys, &keys);
      if (error)
      {
         LOG_ERROR(error);
         return multiKeys;
      }

      // look for keys with a -N suffix
      boost::regex multiRe("^(?:[a-z]+:)?(.*?)(?:-(\\d+))$");
      for (auto key : keys)
      {
         boost::smatch match;
         if (boost::regex_search(key, match, multiRe))
            multiKeys[match[1]] = boost::lexical_cast<int>(match[2]);
      }
   }

   return multiKeys;
}


json::Array indexEntriesToXRefs(const std::vector<XRefIndexEntry>& entries, bool isBookdownProject)
{
   // split out text refs (as a map) and normal entries
   std::map<std::string,std::string> textRefs;
   std::vector<XRefIndexEntry> normalEntries;
   boost::regex textRefRe("^(\\(.*\\))\\s+(.*)$");
   for (auto indexEntry : entries)
   {
      boost::smatch matches;
      if (boost::regex_search(indexEntry.entry, matches, textRefRe))
      {
         textRefs[matches[1]] = matches[2];
      }
      else
      {
         normalEntries.push_back(indexEntry);
      }
   }

   // read in referece-keys.txt so we can detect entires w/ suffixes
   std::map<std::string,int> multiKeys;
   if (isBookdownProject)
      multiKeys = readMultiKeys();

   // turn normal entries into xref json
   json::Array xrefsJson;
   for (auto indexEntry : normalEntries)
   {
      json::Object xrefJson;

      xrefJson[kFile] = indexEntry.file;

      auto entry = indexEntry.entry;
      if (entry.size() > 0)
      {
         bool validEntryId = false;
         std::size_t spacePos = entry.find_first_of(' ');
         if (spacePos != std::string::npos)
         {
            // write the id
            validEntryId = writeEntryId(entry.substr(0, spacePos), &xrefJson);

            // get the title (substitute textref if we have one)
            std::string title = entry.substr(spacePos + 1);

            std::string textrefTitle = textRefs[title];
            if (textrefTitle.length() > 0)
               title = textrefTitle;

            // write the title
            xrefJson[kTitle] = title;
         }
         else
         {
            validEntryId = writeEntryId(entry, &xrefJson);
         }

         // add the entry (suffixed if necessary)
         if (validEntryId)
         {
            // if this key has a suffix then add multiple items w/ suffixes
            std::string id = xrefJson["id"].getString();
            std::map<std::string,int>::const_iterator it = multiKeys.find(id);
            if (it != multiKeys.end() && it->second > 1)
            {
               for (int i=1; i<=it->second; i++)
               {
                  json::Object xrefJsonSuffixed = xrefJson;
                  xrefJsonSuffixed[kSuffix] = "-" + boost::lexical_cast<std::string>(i);
                  xrefsJson.push_back(xrefJsonSuffixed);
               }
            }
            else
            {
               xrefsJson.push_back(xrefJson);
            }
         }
      }
   }

   return xrefsJson;
}



void fileChangeHandler(const core::system::FileChangeEvent& event)
{
   // paths for the rmd file and it's corresponding index file
   FilePath rmdFile = FilePath(event.fileInfo().absolutePath());
   FilePath idxFile = xrefIndexFilePath(FilePath(event.fileInfo().absolutePath()));

   if (event.type() == core::system::FileChangeEvent::FileAdded)
   {
      if (idxFile.exists() && idxFile.getLastWriteTime() > rmdFile.getLastWriteTime())
         return;
   }

   // if this is an add or an update then re-index
   if (event.type() == core::system::FileChangeEvent::FileAdded ||
       event.type() == core::system::FileChangeEvent::FileModified)
   {
      if (rmdFile.exists())
      {
         XRefFileIndex idx = indexForDoc(rmdFile);
         Error error = writeStringVectorToFile(idxFile, idx.entries);
         if (error)
            LOG_ERROR(error);
      }
   }
   // if this is a delete then remove the index
   else if (event.type() == core::system::FileChangeEvent::FileRemoved)
   {
      Error error = idxFile.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
}

bool isBookdownContext()
{
   return module_context::isBookdownProject() && module_context::isPackageInstalled("bookdown");
}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // update unsaved if it's a bookdown rmd
   FileInfo fileInfo(module_context::resolveAliasedPath(pDoc->path()));
   if (isBookdownRmd(fileInfo))
      s_unsavedIndex.updateUnsaved(fileInfo, pDoc->contents(), pDoc->dirty());

}

void onSourceDocRemoved(const std::string&, const std::string& path)
{
   // ignore if the file has no path
   if (path.empty())
      return;

   // remove from unsaved if it's a bookdown rmd
   FileInfo fileInfo(module_context::resolveAliasedPath(path));
   if (isBookdownRmd(fileInfo))
      s_unsavedIndex.removeUnsaved(fileInfo);
}

void onAllSourceDocsRemoved()
{
   s_unsavedIndex.removeAllUnsaved();
}

void onDeferredInit(bool)
{
   if (isBookdownContext())
   {     
      // index docs
      std::vector<boost::shared_ptr<source_database::SourceDocument> > pDocs;
      Error error = source_database::list(&pDocs);
      if (error)
         LOG_ERROR(error);
      std::for_each(pDocs.begin(), pDocs.end(), onSourceDocUpdated);

      // hookup source doc events
      source_database::events().onDocUpdated.connect(onSourceDocUpdated);
      source_database::events().onDocRemoved.connect(onSourceDocRemoved);
      source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);

      // create an incremental file change handler (on the heap so that it
      // survives the call to this function and is never deleted)
      IncrementalFileChangeHandler* pFileChangeHandler =
         new IncrementalFileChangeHandler(
            isBookdownRmd,
            fileChangeHandler,
            boost::posix_time::seconds(1),
            boost::posix_time::milliseconds(500),
            true
         );
      pFileChangeHandler->subscribeToFileMonitor("Bookdown Cross References");   
   }

}

json::Object xrefIndexforProject(IndexEntryFilter filter)
{
   json::Object indexJson;
   indexJson[kBaseDir] = module_context::createAliasedPath(projects::projectContext().buildTargetPath());
   std::vector<XRefIndexEntry> entries = indexEntriesForProject(filter);
   indexJson[kRefs] = indexEntriesToXRefs(entries, true);
   return indexJson;
}

json::Object xrefIndex(const std::string& file, IndexEntryFilter filter)
{
   // resolve path
   FilePath filePath = module_context::resolveAliasedPath(file);

   // result to return
   json::Object indexJson;

   // if this is a bookdown context then send the whole project index
   if (isBookdownContext() && filePath.isWithin(projects::projectContext().buildTargetPath()))
   {
      indexJson = xrefIndexforProject(filter);
   }

   // otherwise just send an index for this file (it will be in the source database)
   else
   {
      indexJson[kBaseDir] = module_context::createAliasedPath(filePath.getParent());

      std::string id;
      source_database::getId(filePath, &id);
      if (!id.empty())
      {
         boost::shared_ptr<source_database::SourceDocument> pDoc(
                  new source_database::SourceDocument());
         Error error = source_database::get(id, pDoc);
         if (error)
         {
            LOG_ERROR(error);
            indexJson[kRefs] = json::Array();
         }
         else
         {
            XRefFileIndex idx = indexForDoc(filePath.getFilename(), pDoc->contents());
            std::vector<XRefIndexEntry> entries = indexEntriesForFile(idx, filter);
            indexJson["refs"] = indexEntriesToXRefs(entries, false);
         }
      }
      else
      {
         indexJson[kRefs] = json::Array();
      }
   }

   return indexJson;
}


Error xrefIndexForFile(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file;
   Error error = json::readParams(request.params, &file);
   if (error)
      return error;

   // filter that returns all entries
   IndexEntryFilter includeAll = boost::lambda::constant(true);

   // get index and return it
   pResponse->setResult(xrefIndex(file, includeAll));

   return Success();
}

Error xrefForId(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file, id;
   Error error = json::readParams(request.params, &file, &id);
   if (error)
      return error;

   // get index containing just the entry that matches this id
   json::Object indexJson = xrefIndex(file, [id](const std::string& entry) {
      std::string entryId = entry.substr(0, entry.find_first_of(' '));
      if (id == entryId)
      {
         return true;
      }
      else
      {
         // headings also match on just the id part
         entryId = boost::regex_replace(entryId, boost::regex("^h\\d\\:"), "");
         if (id == entryId)
            return true;

         // we can also match after trimming off any provided suffix
         std::string trimmedId = boost::regex_replace(id, boost::regex("-\\d$"), "");
         if (trimmedId == entryId)
            return true;
      }

      return false;
   });

   // if there is more than one item returned it could have been a suffix match,
   // in that case winnow it down to the passed id
   json::Array refsJson = indexJson["refs"].getArray();
   if (refsJson.getSize() > 1)
   {
      for (auto refJsonValue : refsJson)
      {
         json::Object refJson = refJsonValue.getObject();
         boost::format fmt("%1%:%2%%3%");
         std::string refId = boost::str(fmt %
                                        refJson[kType].getString() %
                                        refJson[kId].getString() %
                                        refJson[kSuffix].getString());
         if (refId == id)
         {
            json::Array suffixRefsJson;
            suffixRefsJson.push_back(refJson);
            indexJson["refs"] = suffixRefsJson;
            break;
         }
      }
   }

   // return it
   pResponse->setResult(indexJson);

   return Success();
}

} // anonymous namespace

namespace modules {
namespace rmarkdown {
namespace bookdown {
namespace xrefs {

Error initialize()
{
   // deferred init (build xref file index)
   module_context::events().onDeferredInit.connect(onDeferredInit);

   // register rpc functions
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "xref_index_for_file", xrefIndexForFile))
     (boost::bind(module_context::registerRpcMethod, "xref_for_id", xrefForId))
   ;
   return initBlock.execute();


}

} // namespace xrefs
} // namespace bookdown
} // namespace rmarkdown
} // namespace modules

namespace module_context {

core::json::Value bookdownXRefIndex()
{
   if (isBookdownContext())
      return xrefIndexforProject(boost::lambda::constant(true));
   else
      return json::Value();
}

} // namespace module_context

} // namespace session
} // namespace rstudio
