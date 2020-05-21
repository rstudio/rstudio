/*
 * DesktopActivationOverlay.hpp
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

#ifndef DESKTOP_ACTIVATION_HPP
#define DESKTOP_ACTIVATION_HPP

#include <QObject>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace desktop {

class DesktopActivation;
DesktopActivation& activation();

class DesktopActivation : public QObject
{
   Q_OBJECT
public:
   DesktopActivation();

   void getInitialLicense(const QStringList& arguments,
                          const core::FilePath& installPath,
                          bool devMode);
   bool allowProductUsage();

   // Description of license state if expired or within certain time window before expiring,
   // otherwise empty string
   std::string currentLicenseStateMessage();

   // Description of license state
   std::string licenseStatus();

   // Set main window, so we can supply it as default parent of message boxes
   void setMainWindow(QWidget* pWidget);

   void showLicenseDialog(bool showQuitButton);

   void releaseLicense();

   // Name of product edition, for use in UI
   QString editionName() const;

Q_SIGNALS:
   void licenseLost(QString licenseMessage);
   void launchFirstSession();
   void launchError(QString message);
   void detectLicense();
   void updateLicenseWarningBar(QString message);

public:

   // license has been lost while using the program
   void emitLicenseLostSignal();

   // no longer need to show a license warning bar
   void emitUpdateLicenseWarningBarSignal(QString message);

   // start a session after validating initial license
   void emitLaunchFirstSession();

   // show a messagebox then exit the program
   void emitLaunchError(QString message);

   // exit the program
   void emitLaunchError();

   // detect (or re-detect) license status
   void emitDetectLicense();
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_ACTIVATION_HPP
