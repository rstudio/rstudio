/*
 * DesktopOfficeViewer.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
#include <core/Error.hpp>

struct IDispatch;

namespace rstudio {
namespace desktop {

class OfficeViewer : boost::noncopyable
{
public:
   OfficeViewer(const std::wstring& progId);
   ~OfficeViewer();
   core::Error ensureInterface();
   core::Error showApp();
   IDispatch* idispApp();

private:
   IDispatch* idisp_;
   std::wstring progId_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_OFFICE_VIEWER_HPP

