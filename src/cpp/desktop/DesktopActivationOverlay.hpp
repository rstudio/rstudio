/*
 * DesktopActivationOverlay.hpp
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

#ifndef DESKTOP_ACTIVATION_HPP
#define DESKTOP_ACTIVATION_HPP

#include <QObject>
#include <QProcess>

#include <core/FilePath.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace desktop {
namespace activation {

class DesktopActivation;
DesktopActivation& activation();

class DesktopActivation : public QObject
{
   Q_OBJECT
public:
   DesktopActivation();

   bool getInitialLicense(const core::FilePath& installPath, bool devMode);
   bool allowProductUsage();

   // Description of license state if expired or within certain time window before expiring,
   // otherwise empty string
   std::string currentLicenseStateMessage();

   // Description of license state
   std::string licenseStatus();

   void showLicenseDialog();

public slots:
   void onLicenseManagerExited(int exitCode, QProcess::ExitStatus exitStatus);
   void onUpdateLicenseState();

signals:
   void licenseLost(QString licenseMessage);
};

} // namespace activation
} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_ACTIVATION_HPP
