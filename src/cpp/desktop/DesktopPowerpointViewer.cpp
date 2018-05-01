/*
 * DesktopPowerpointViewer.cpp
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

#include <iostream>

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <boost/utility.hpp>
#include <boost/scoped_array.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopComUtils.hpp"
#include "DesktopPowerpointViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

PowerpointViewer::PowerpointViewer():
    OfficeViewer(L"Powerpoint.Application"),
    slideIndex_(0)
{
}

Error PowerpointViewer::showPresentation(QString& path)
{
   return Success();
}

Error PowerpointViewer::closeLastPresentation()
{
   return Success();
}

} // namespace desktop
} // namespace rstudio
