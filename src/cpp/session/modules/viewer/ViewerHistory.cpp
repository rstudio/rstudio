/*
 * ViewerHistory.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "ViewerHistory.hpp"

#include <boost/format.hpp>
#include <boost/foreach.hpp>

#include <core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace viewer {

ViewerHistory& viewerHistory()
{
   static ViewerHistory instance;
   return instance;
}

ViewerHistory::ViewerHistory()
   : currentIndex_(-1)
{
   entries_.set_capacity(20);
}

void ViewerHistory::add(const module_context::ViewerHistoryEntry& entry)
{
   entries_.push_back(entry);
   currentIndex_ = entries_.size() - 1;
}

void ViewerHistory::clear()
{
   currentIndex_ = -1;
   entries_.clear();
}

module_context::ViewerHistoryEntry ViewerHistory::current() const
{
   if (currentIndex_ == -1)
      return module_context::ViewerHistoryEntry();
   else
      return entries_[currentIndex_];
}

void ViewerHistory::clearCurrent()
{
   if (currentIndex_ != -1)
   {
      entries_.erase(entries_.begin() + currentIndex_);
      if (entries_.size() > 0)
         currentIndex_ = std::max(0, currentIndex_ - 1);
      else
         currentIndex_ = -1;
   }
}


bool ViewerHistory::hasNext() const
{
   int size = safe_convert::numberTo<int>(entries_.size() - 1, 0);
   return currentIndex_ != -1 && currentIndex_ < size;
}

module_context::ViewerHistoryEntry ViewerHistory::goForward()
{
   if (hasNext())
      return entries_[++currentIndex_];
   else
      return module_context::ViewerHistoryEntry();
}

bool ViewerHistory::hasPrevious() const
{
   return entries_.size() > 0 && currentIndex_ > 0;
}

module_context::ViewerHistoryEntry ViewerHistory::goBack()
{
   if (hasPrevious())
      return entries_[--currentIndex_];
   else
      return module_context::ViewerHistoryEntry();
}

namespace {

FilePath historyEntriesPath(const core::FilePath& serializationPath)
{
   return serializationPath.complete("history_entries");
}

FilePath currentIndexPath(const core::FilePath& serializationPath)
{
   return serializationPath.complete("current_index");
}

std::string historyEntryToString(const module_context::ViewerHistoryEntry& entry)
{
   return entry.sessionTempPath();
}

ReadCollectionAction historyEntryFromString(
         const std::string& url, module_context::ViewerHistoryEntry* pEntry)
{
   *pEntry = module_context::ViewerHistoryEntry(url);
   return ReadCollectionAddLine;
}


} // anonymous namespace

void ViewerHistory::saveTo(const core::FilePath& serializationPath) const
{
   // blow away any existing serialization data
   Error error = serializationPath.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // skip if there is no current index
   if (currentIndex_ == -1)
      return;

   // ensure the directory
   error = serializationPath.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // save the current index
   std::string currentIndex = safe_convert::numberToString(currentIndex_);
   error = core::writeStringToFile(currentIndexPath(serializationPath),
                                   currentIndex);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // save the list of entries
   using module_context::ViewerHistoryEntry;
   error = writeCollectionToFile<boost::circular_buffer<ViewerHistoryEntry> >(
                                         historyEntriesPath(serializationPath),
                                         entries_,
                                         historyEntryToString);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // copy the files
   FilePath tempDir = module_context::tempDir();
   BOOST_FOREACH(const ViewerHistoryEntry& entry, entries_)
   {
      Error error = entry.copy(tempDir, serializationPath);
      if (error)
         LOG_ERROR(error);
   }
}

void ViewerHistory::restoreFrom(const core::FilePath& serializationPath)
{
   // skip if the directory doesn't exist
   if (!serializationPath.exists())
      return;

   // clear existing
   currentIndex_ = -1;
   entries_.clear();

   // check if we have an index path (bail if we can't find one)
   FilePath indexPath = currentIndexPath(serializationPath);
   if (!indexPath.exists())
      return;

   // read the index
   std::string currentIndex;
   Error error = core::readStringFromFile(indexPath, &currentIndex);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   currentIndex_ = safe_convert::stringTo<int>(currentIndex, -1);
   if (currentIndex_ == -1)
      return;

   // read the entries
   using module_context::ViewerHistoryEntry;
   error = readCollectionFromFile<boost::circular_buffer<ViewerHistoryEntry> >(
                                         historyEntriesPath(serializationPath),
                                         &entries_,
                                         historyEntryFromString);
   if (error)
   {
      LOG_ERROR(error);
      entries_.clear();
      currentIndex_ = -1;
      return;
   }

   // copy the files to the session temp dir
   FilePath tempDir = module_context::tempDir();
   BOOST_FOREACH(const ViewerHistoryEntry& entry, entries_)
   {
      Error error = entry.copy(serializationPath, tempDir);
      if (error)
         LOG_ERROR(error);
   }
}


} // namespace viewer
} // namespace modules

namespace module_context {

std::string ViewerHistoryEntry::url() const
{
   return module_context::sessionTempDirUrl(sessionTempPath_);
}

core::Error ViewerHistoryEntry::copy(
             const core::FilePath& sourceDir,
             const core::FilePath& destinationDir) const
{
   // copy enclosing directory to the destinationDir
   FilePath entryPath = sourceDir.childPath(sessionTempPath_);
   FilePath parentDir = entryPath.parent();
   return module_context::recursiveCopyDirectory(parentDir, destinationDir);
}

void addViewerHistoryEntry(const ViewerHistoryEntry& entry)
{
   modules::viewer::viewerHistory().add(entry);
}

} // namespace module_context

} // namespace session
} // namespace rstudio

