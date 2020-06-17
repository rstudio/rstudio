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

FilePath indexFilePath()
{
   return module_context::scopedScratchPath().completeChildPath("bookdown-xrefs");
}


struct XRefIndex
{
   XRefIndex(const std::string& file) : file(file) {}
   std::string file;
   std::vector<std::string> entries;
};


XRefIndex indexForDoc(const std::string& file, const std::string& contents)
{
   XRefIndex index(file);

   // run pandoc w/ custom lua filter to capture index
   std::vector<std::string> args;
   args.push_back("--to");

   // TODO: get filter

   core::system::ProcessResult result;
   Error error = module_context::runPandoc(args, contents, &result);


   return index;
}

XRefIndex indexForDoc(const FilePath& filePath, const std::string& contents)
{
   std::string file = filePath.getRelativePath(projects::projectContext().buildTargetPath());
   return indexForDoc(file, contents);
}

XRefIndex indexForDoc(const FilePath& filePath)
{
   std::string contents;
   Error error = core::readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   return indexForDoc(filePath, contents);
}

XRefIndex indexForDoc(const FileInfo& fileInfo)
{
   return indexForDoc(FilePath(fileInfo.absolutePath()));
}



XRefIndex indexForDoc(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
   return indexForDoc(filePath, pDoc->contents());
}







bool isBookdownRmd(const FileInfo& fileInfo)
{
   FilePath filePath(fileInfo.absolutePath());
   FilePath bookDir = projects::projectContext().buildTargetPath();
   return filePath.isWithin(bookDir) && (filePath.getExtensionLowerCase() == ".rmd");
}




void fileChangeHandler(const core::system::FileChangeEvent& event)
{

   // alias the filename
   std::string file = event.fileInfo().absolutePath();

   // if this is an add or an update then re-index
   if (event.type() == core::system::FileChangeEvent::FileAdded ||
       event.type() == core::system::FileChangeEvent::FileModified)
   {
      std::cerr << file << std::endl;
   }



}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FileInfo fileInfo(module_context::resolveAliasedPath(pDoc->path()));

   if (isBookdownRmd(fileInfo))
   {
      std::cerr << "UPDATED: " << fileInfo << " " << (pDoc->dirty() ? "(dirty)" : "") << std::endl;
   }


}

void onSourceDocRemoved(const std::string& id, const std::string& path)
{
   // ignore if the file has no path
   if (path.empty())
      return;


   FileInfo fileInfo(module_context::resolveAliasedPath(path));

   if (isBookdownRmd(fileInfo))
   {
      std::cerr << "REMOVED: " << fileInfo << std::endl;

   }



}

void onAllSourceDocsRemoved()
{

}

void onDeferredInit(bool)
{
   if (module_context::isBookdownWebsite())
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


   module_context::events().onDeferredInit.connect(onDeferredInit);

   return Success();
}

} // namespace xrefs
} // namespace bookdown
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
