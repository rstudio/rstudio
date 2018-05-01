/*
 * DesktopPowerpointViewer.hpp
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
#ifndef DESKTOP_PRESENTATION_VIEWER_HPP
#define DESKTOP_PRESENTATION_VIEWER_HPP

#include <QString>

#include <boost/utility.hpp>
#include <core/Error.hpp>

#include "DesktopOfficeViewer.hpp"

struct IDispatch;

namespace rstudio {
namespace desktop {

class PowerpointViewer : public OfficeViewer
{
public:
   PowerpointViewer();
   core::Error showPresentation(QString& path);
   core::Error closeLastPresentation();

private:
   int slideIndex_;
   QString docPath_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_PRESENTATION_VIEWER_HPP
