/*
 * DesktopPosixApplication.hpp
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

#ifndef DESKTOP_POSIX_APPLICATION_HPP
#define DESKTOP_POSIX_APPLICATION_HPP

#include "3rdparty/qtsingleapplication/QtSingleApplication"

#include "DesktopApplicationLaunch.hpp"

namespace rstudio {
namespace desktop {

class PosixApplication : public QtSingleApplication
{
    Q_OBJECT
public:

   PosixApplication(const QString& appName, int& argc, char* argv[])
    : QtSingleApplication(appName, argc, argv),
      pAppLauncher_(nullptr)
   {
   }

   QString startupOpenFileRequest() const
   {
      return startupOpenFileRequest_;
   }

   void setAppLauncher(ApplicationLaunch* pAppLauncher)
   {
       pAppLauncher_ = pAppLauncher;
   }

Q_SIGNALS:
    void openFileRequest(QString filename);

protected:
    bool event(QEvent* pEvent) override;

private:
    QString startupOpenFileRequest_;
    ApplicationLaunch* pAppLauncher_;
};


} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_POSIX_APPLICATION_HPP
