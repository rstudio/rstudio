/*
 * DesktopActivationOverlay.cpp
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

#include "DesktopActivationOverlay.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

DesktopActivation& activation()
{
   static DesktopActivation singleton;
   return singleton;
}

DesktopActivation::DesktopActivation() = default;

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

void DesktopActivation::getInitialLicense(const QStringList& arguments,
                                          const core::FilePath& installPath,
                                          bool devMode)
{
   emit launchFirstSession();
}

void DesktopActivation::showLicenseDialog(QWidget* pParent)
{
}

void DesktopActivation::emitLicenseLostSignal()
{
   emit licenseLost(QString::fromUtf8(currentLicenseStateMessage().c_str()));
}

void DesktopActivation::emitLaunchFirstSession()
{
   emit launchFirstSession();
}

void DesktopActivation::emitLaunchError(QString message)
{
   emit launchError(message);
}

} // namespace desktop
} // namespace rstudio
