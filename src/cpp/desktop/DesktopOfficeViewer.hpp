/*
 * DesktopOfficeViewer.hpp
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

#ifndef DESKTOP_OFFICE_VIEWER_HPP
#define DESKTOP_OFFICE_VIEWER_HPP

#include <QString>

#include <boost/utility.hpp>
#include <shared_core/Error.hpp>

struct IDispatch;

namespace rstudio {
namespace desktop {

// OfficeViewer represents a viewer for Microsoft Office file formats. It is
// used as the base class for the Word document viewer (WordViewer) and the
// Powerpoint presentation viewer (PowerpointViewer).
class OfficeViewer : boost::noncopyable
{
public:
   OfficeViewer(const std::wstring& progId, const std::wstring& collection,
                int readOnlyPos);
   ~OfficeViewer();

   // Public interface
   core::Error showItem(const std::wstring& path);
   core::Error closeLastViewedItem();

   // Pure virtual functions for implementation by individual formats
   virtual core::Error savePosition(IDispatch* source) = 0;
   virtual core::Error restorePosition(IDispatch* target) const = 0;
   virtual void resetPosition() = 0;
   virtual bool hasPosition() const = 0;

protected:
   core::Error ensureInterface();
   core::Error showApp();
   IDispatch* idispApp();

   core::Error openFile(const std::wstring& path, IDispatch** pidispOut);
   core::Error getItemFromPath(const std::wstring& path, IDispatch** pidispOut);

   std::wstring path();
   void setPath(const std::wstring& path);

private:
   IDispatch* idisp_;         // Pointer to running application
   std::wstring progId_;      // ID of the viewer application
   std::wstring collection_;  // Name of collection to search for items
   std::wstring path_;        // Path of currently open item
   int readOnlyPos_;          // Position of read-only flag in open call
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_OFFICE_VIEWER_HPP

