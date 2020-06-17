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

#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>

#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/IncrementalFileChangeHandler.hpp>


namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace bookdown {
namespace xrefs {

using namespace rstudio::core;

namespace {



bool isBookdownRmd(const FileInfo& fileInfo)
{
   FilePath filePath(fileInfo.absolutePath());
   FilePath bookDir = projects::projectContext().buildTargetPath();
   return filePath.isWithin(bookDir) && (filePath.getExtensionLowerCase() == ".rmd");
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
   FilePath xrefsPath = module_context::scopedScratchPath().completeChildPath("bookdown-xrefs");
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
   // date time
   std::vector<std::string> entries;
};


XRefFileIndex indexForDoc(const std::string& file, const std::string& contents)
{
   XRefFileIndex index(file);

   // run pandoc w/ custom lua filter to capture index
   std::vector<std::string> args;
   args.push_back("--to");
   FilePath resPath = session::options().rResourcesPath();
   FilePath xrefLuaPath = resPath.completePath("xref.lua");
   std::string xrefLua = string_utils::utf8ToSystem(xrefLuaPath.getAbsolutePath());
   args.push_back(xrefLua);
   core::system::ProcessResult result;
   Error error = module_context::runPandoc(args, contents, &result);
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

class XRefProjectIndex
{
public:

   json::Array toJson()
   {
      json::Array indexJson;

      // find out what the docs in the book are
      std::vector<std::string> sourceFiles = bookdownSourceFiles();

      for (std::vector<std::string>::size_type i = 0; i < sourceFiles.size(); i++) {

         // alias source files
         const std::string& sourceFile = sourceFiles[i];

         // create a file entry
         json::Object fileJson;
         fileJson["file"] = sourceFile;

         // prefer unsaved files
         std::vector<std::string> entries;
         std::map<std::string, XRefFileIndex>::const_iterator it = unsavedFiles_.find(sourceFile);
         if (it != unsavedFiles_.end())
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

         if (entries.size() > 0)
         {
            // entries
            json::Array entriesJson;
            for (std::vector<std::string>::size_type e = 0; e < entries.size(); e++)
            {
               json::Object entryJson;
               const std::string& entry = entries[e];
               if (entry.size() > 0)
               {
                  std::size_t spacePos = entry.find_first_of(' ');
                  if (spacePos != std::string::npos)
                  {
                     entryJson["key"] = entry.substr(0, spacePos);
                     entryJson["title"] = entry.substr(spacePos + 1);
                  }
                  else
                  {
                     entryJson["key"] = entry;
                  }
                  entriesJson.push_back(entryJson);
               }
            }
            fileJson["entries"] = entriesJson;

            // add to main index
            indexJson.push_back(fileJson);
         }
      }



      return indexJson;
   }


   void updateUnsaved(const FileInfo& fileInfo, const std::string& contents)
   {
      FilePath filePath = toFilePath(fileInfo);
      XRefFileIndex idx = indexForDoc(filePath, contents);
      unsavedFiles_[bookRelativePath(filePath)] = idx;
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
XRefProjectIndex s_projectIndex;


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
      XRefFileIndex idx = indexForDoc(rmdFile);
      Error error = writeStringVectorToFile(idxFile, idx.entries);
      if (error)
         LOG_ERROR(error);
   }
   // if this is a delete then remove the index
   else if (event.type() == core::system::FileChangeEvent::FileRemoved)
   {
      Error error = idxFile.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
}



void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // update unsaved if it's a bookdown rmd
   FileInfo fileInfo(module_context::resolveAliasedPath(pDoc->path()));
   if (isBookdownRmd(fileInfo))
      s_projectIndex.updateUnsaved(fileInfo, pDoc->contents());

}

void onSourceDocRemoved(const std::string&, const std::string& path)
{
   // ignore if the file has no path
   if (path.empty())
      return;

   // remove from unsaved if it's a bookdown rmd
   FileInfo fileInfo(module_context::resolveAliasedPath(path));
   if (isBookdownRmd(fileInfo))
      s_projectIndex.removeUnsaved(fileInfo);  
}

void onAllSourceDocsRemoved()
{
   s_projectIndex.removeAllUnsaved();
}

void onDeferredInit(bool)
{
   if (module_context::isBookdownWebsite() && module_context::isPackageInstalled("bookdown"))
   {
      // create an incremental file change handler (on the heap so that it
      // survives the call to this function and is never deleted)
      IncrementalFileChangeHandler* pFileChangeHandler =
         new IncrementalFileChangeHandler(
            isBookdownRmd,
            fileChangeHandler,
            boost::posix_time::seconds(3),
            boost::posix_time::milliseconds(500),
            true
         );
      pFileChangeHandler->subscribeToFileMonitor("Bookdown Cross References");
   }

}

} // anonymous namespace


Error initialize()
{
   // subscribe to source docs events for maintaining the unsaved files list
   source_database::events().onDocUpdated.connect(onSourceDocUpdated);
   source_database::events().onDocRemoved.connect(onSourceDocRemoved);
   source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);

   // deferred init (build xref file index)
   module_context::events().onDeferredInit.connect(onDeferredInit);

   return Success();
}

} // namespace xrefs
} // namespace bookdown
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
