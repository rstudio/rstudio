/*
 * DesktopUpdateChecker.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopUpdateChecker.hpp"

#include <QDesktopServices>
#include <QMessageBox>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/http/Util.hpp>

#include "DesktopOptions.hpp"
#include "DesktopURLDownloader.hpp"
#include "DesktopUpdateAvailableDialog.hpp"

#include "config.h"

using namespace core;

namespace desktop {

QUrl UpdateChecker::checkForUpdatesURL()
{
   QUrl url("http://www.rstudio.org/links/check_for_update");
   url.addQueryItem("version", RSTUDIO_VERSION);
   return url;
}

void UpdateChecker::performCheck(bool manuallyInvoked)
{
   // build URL (specify key-value pair return)
   QUrl url = checkForUpdatesURL();
   url.addQueryItem("format", "kvp");

   // download manifest (URL downlader frees itself)
   URLDownloader* pURLDownloader = new URLDownloader(url,
                                                     10000,
                                                     manuallyInvoked,
                                                     pOwnerWindow_);
   connect(pURLDownloader, SIGNAL(downloadError(const QString&)),
           this, SLOT(manifestDownloadError(const QString&)));
   connect(pURLDownloader, SIGNAL(downloadComplete(const QByteArray&)),
           this, SLOT(manifestDownloadComplete(const QByteArray&)));
}

void UpdateChecker::manifestDownloadError(const QString &message)
{
   LOG_ERROR_MESSAGE("Error downloading manifest: " + message.toStdString());

   URLDownloader* pURLDownloader = qobject_cast<URLDownloader*>(sender());
   if (pURLDownloader && pURLDownloader->manuallyInvoked())
   {
      // WA_DeleteOnClose
      QMessageBox* pMsg = new QMessageBox(
            QMessageBox::Warning,
            "Error Checking for Updates",
            "An error occurred while checking for updates:\n\n"
            + message,
            QMessageBox::Ok,
            pOwnerWindow_,
            Qt::Sheet | Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);
      pMsg->setWindowModality(Qt::WindowModal);
      pMsg->setAttribute(Qt::WA_DeleteOnClose);
      pMsg->show();
   }
}

void UpdateChecker::manifestDownloadComplete(const QByteArray& data)
{
   // parse manifest
   std::string manifest(data.constData(), data.size());
   http::Fields fields;
   http::util::parseForm(manifest, &fields);

   // get the list of ignored updates
   QStringList ignoredVersions = options().ignoredUpdateVersions();

   // is there an update which we haven't already chosen to ignore?
   std::string stdUpdateVersion = http::util::fieldValue(fields, "update-version");
   QString updateVersion = QString::fromStdString(stdUpdateVersion);
   if ( (updateVersion.size() > 0) && !ignoredVersions.contains(updateVersion))
   {
      // get update info
      std::string updateURL = http::util::fieldValue(fields, "update-url");
      std::string updateMessage = http::util::fieldValue(fields, "update-message");
      int isUrgent = http::util::fieldValue<int>(fields, "update-urgent", 0);
      DesktopUpdateInfo updateInfo;
      updateInfo.currentVersion = RSTUDIO_VERSION;
      updateInfo.updatedVersion = updateVersion;
      updateInfo.updateURL = QString::fromStdString(updateURL);
      updateInfo.updateMessage = QString::fromStdString(updateMessage);
      updateInfo.isUrgent = isUrgent != 0;

      // invoke dialog
      DesktopUpdateAvailableDialog dialog(updateInfo, pOwnerWindow_);
      int result = dialog.exec();

      // record if we are permanently ignoring
      switch (result)
      {
      case DesktopUpdateAvailableDialog::Accepted:
         QDesktopServices::openUrl(QUrl(updateInfo.updateURL));
         break;
      case DesktopUpdateAvailableDialog::Rejected:
         break;
      case DesktopUpdateAvailableDialog::Ignored:
         ignoredVersions.append(updateVersion);
         options().setIgnoredUpdateVersions(ignoredVersions);
         break;
      }
   }
   else
   {
      URLDownloader* pURLDownloader = qobject_cast<URLDownloader*>(sender());
      if (pURLDownloader && pURLDownloader->manuallyInvoked())
      {
         // WA_DeleteOnClose
         QMessageBox* pMsg = new QMessageBox(
               QMessageBox::Warning,
               "No Update Available",
               "You're using the newest version of RStudio.",
               QMessageBox::Ok,
               pOwnerWindow_,
               Qt::Sheet | Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);
         pMsg->setWindowModality(Qt::WindowModal);
         pMsg->setAttribute(Qt::WA_DeleteOnClose);
         pMsg->show();
      }
   }
}


} // namespace desktop
