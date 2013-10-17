/*
 * DesktopUpdateChecker.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "DesktopUpdateChecker.hpp"

#include <QMessageBox>
#include <QPushButton>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/http/Util.hpp>

#include "DesktopOptions.hpp"
#include "DesktopURLDownloader.hpp"
#include "DesktopUpdateAvailableDialog.hpp"

#include "DesktopUtils.hpp"

#include "desktop-config.h"

using namespace core;

namespace desktop {

QUrl UpdateChecker::checkForUpdatesURL()
{
   QUrl url(QString::fromAscii("http://www.rstudio.org/links/check_for_update"));
   url.addQueryItem(QString::fromAscii("version"), QString::fromAscii(RSTUDIO_VERSION));
   QString platform;
#if defined(_WIN32)
   platform = QString::fromAscii("windows");
#elif defined(__APPLE__)
   platform = QString::fromAscii("mac");
#else
   platform = QString::fromAscii("linux");
#endif
   url.addQueryItem(QString::fromAscii("os"), platform);
   return url;
}

void UpdateChecker::performCheck(bool manuallyInvoked)
{
   // build URL (specify key-value pair return)
   QUrl url = checkForUpdatesURL();
   url.addQueryItem(QString::fromAscii("format"), QString::fromAscii("kvp"));
   if (manuallyInvoked)
      url.addQueryItem(QString::fromAscii("manual"), QString::fromAscii("true"));

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
   LOG_ERROR_MESSAGE("Error downloading manifest: " + std::string(message.toUtf8().constData()));

   URLDownloader* pURLDownloader = qobject_cast<URLDownloader*>(sender());
   if (pURLDownloader && pURLDownloader->manuallyInvoked())
   {
      // WA_DeleteOnClose
      QMessageBox* pMsg = new QMessageBox(
            safeMessageBoxIcon(QMessageBox::Warning),
            QString::fromUtf8("Error Checking for Updates"),
            QString::fromUtf8("An error occurred while checking for updates:\n\n")
            + message,
            QMessageBox::NoButton,
            pOwnerWindow_,
            Qt::Sheet | Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);
      pMsg->setWindowModality(Qt::WindowModal);
      pMsg->setAttribute(Qt::WA_DeleteOnClose);
      pMsg->addButton(new QPushButton(QString::fromUtf8("OK")), QMessageBox::AcceptRole);
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

   URLDownloader* pURLDownloader = qobject_cast<URLDownloader*>(sender());

   // is there an update which we haven't already chosen to ignore?
   std::string stdUpdateVersion = http::util::fieldValue(fields, "update-version");
   QString updateVersion = QString::fromUtf8(stdUpdateVersion.c_str());
   if ( (updateVersion.size() > 0) &&
        (!ignoredVersions.contains(updateVersion) || pURLDownloader->manuallyInvoked()) )
   {
      // get update info
      std::string updateURL = http::util::fieldValue(fields, "update-url");
      std::string updateMessage = http::util::fieldValue(fields, "update-message");
      int isUrgent = http::util::fieldValue<int>(fields, "update-urgent", 0);
      DesktopUpdateInfo updateInfo;
      updateInfo.currentVersion = QString::fromUtf8(RSTUDIO_VERSION);
      updateInfo.updatedVersion = updateVersion;
      updateInfo.updateURL = QString::fromUtf8(updateURL.c_str());
      updateInfo.updateMessage = QString::fromUtf8(updateMessage.c_str());
      updateInfo.isUrgent = isUrgent != 0;

      // invoke dialog
      DesktopUpdateAvailableDialog dialog(updateInfo, pOwnerWindow_);
      int result = dialog.exec();

      // record if we are permanently ignoring
      switch (result)
      {
      case DesktopUpdateAvailableDialog::Accepted:
         desktop::openUrl(QUrl(updateInfo.updateURL));
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
      if (pURLDownloader && pURLDownloader->manuallyInvoked())
      {
         // WA_DeleteOnClose
         QMessageBox* pMsg = new QMessageBox(
               safeMessageBoxIcon(QMessageBox::Information),
               QString::fromUtf8("No Update Available"),
               QString::fromUtf8("You're using the newest version of RStudio."),
               QMessageBox::NoButton,
               pOwnerWindow_,
               Qt::Sheet | Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);
         pMsg->setWindowModality(Qt::WindowModal);
         pMsg->setAttribute(Qt::WA_DeleteOnClose);
         pMsg->addButton(new QPushButton(QString::fromUtf8("OK")), QMessageBox::AcceptRole);
         pMsg->show();
      }
   }
}


} // namespace desktop
