/*
 * DesktopActivationOverlay.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "DesktopActivationOverlay.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {
namespace activation {

DesktopActivation& activation()
{
   static DesktopActivation singleton;
   return singleton;
}

DesktopActivation::DesktopActivation()
{
}

bool DesktopActivation::allowProductUsage()
{
   return true;
}

std::string DesktopActivation::currentLicenseStateMessage()
{
   return std::string();
}

std::string DesktopActivation::licenseStatus()
{
   return std::string();
}

bool DesktopActivation::getInitialLicense(const QStringList& arguments,
                                          const core::FilePath& installPath,
                                          bool devMode)
{
   return true;
}

void DesktopActivation::showLicenseDialog()
{
}

bool DesktopActivation::hasLicenseLostSignal() const
{
   return false;
}

} // namespace activation
} // namespace desktop
} // namespace rstudio
