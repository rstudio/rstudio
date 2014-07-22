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

#include <session/SessionModuleContext.hpp>

using namespace core;

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
   return currentIndex_ != -1 && currentIndex_ < (entries_.size() - 1);
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


} // namespace viewer
} // namespace modules

namespace module_context {

std::string ViewerHistoryEntry::url() const
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      boost::format fmt("http://localhost:%1%/session/%2%");
      return boost::str(fmt % rLocalHelpPort() % sessionTempPath_);
   }
   else
   {
      boost::format fmt("session/%1%");
      return boost::str(fmt % sessionTempPath_);
   }
}

void addViewerHistoryEntry(const ViewerHistoryEntry& entry)
{
   modules::viewer::viewerHistory().add(entry);
}

} // namespace module_context

} // namesapce session

