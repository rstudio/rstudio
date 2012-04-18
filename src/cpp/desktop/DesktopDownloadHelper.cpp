/*
 * DesktopDownloadHelper.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopDownloadHelper.hpp"
#include <QFile>
#include <core/Log.hpp>

#include "DesktopUtils.hpp"

namespace desktop {

DownloadHelper::DownloadHelper(QNetworkReply* pReply,
                               QString fileName) :
    QObject(pReply),
    fileName_(fileName)
{
   connect(pReply, SIGNAL(finished()), this, SLOT(onDownloadFinished()));
}

void DownloadHelper::onDownloadFinished()
{
   QNetworkReply* pReply = static_cast<QNetworkReply*>(sender());

   if (pReply->error() != QNetworkReply::NoError)
   {
      showWarning(NULL,
                  QString::fromUtf8("Download Failed"),
                  QString::fromUtf8("An error occurred during download:\n\n")
                  + pReply->errorString());
      return;
   }

   QFile file(fileName_);
   if (file.open(QFile::ReadWrite))
      file.write(pReply->readAll());

   downloadFinished(fileName_);

   pReply->close();
   pReply->deleteLater();
   deleteLater();
}

} // namespace desktop
