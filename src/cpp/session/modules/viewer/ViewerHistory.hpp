/*
 * ViewerHistory.hpp
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

#ifndef SESSION_VIEWER_HISTORY_HPP
#define SESSION_VIEWER_HISTORY_HPP

#include <boost/utility.hpp>
#include <boost/circular_buffer.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace viewer {

class ViewerHistory;
ViewerHistory& viewerHistory();

class ViewerHistory : boost::noncopyable
{
private:
   ViewerHistory();
   friend ViewerHistory& viewerHistory();

public:
   void add(const module_context::ViewerHistoryEntry& entry);
   void clear();

   module_context::ViewerHistoryEntry current() const;
   void clearCurrent();

   bool hasNext() const;
   module_context::ViewerHistoryEntry goForward();

   bool hasPrevious() const;
   module_context::ViewerHistoryEntry goBack();

   void saveTo(const core::FilePath& serializationPath) const;
   void restoreFrom(const core::FilePath& serializationPath);

private:
   int currentIndex_;
   boost::circular_buffer<module_context::ViewerHistoryEntry> entries_;

};
                       
} // namespace viewer
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_VIEWER_HISTORY_HPP
