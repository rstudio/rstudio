/*
 * DesktopUpdateChecker.hpp
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

#ifndef DESKTOP_UPDATE_CHECKER_HPP
#define DESKTOP_UPDATE_CHECKER_HPP

#include <QUrl>
#include <QObject>
#include <QMainWindow>

namespace desktop {

class UpdateChecker : public QObject
{
   Q_OBJECT
public:
   UpdateChecker(QMainWindow* pOwnerWindow)
      : pOwnerWindow_(pOwnerWindow)
   {
   }

   QUrl checkForUpdatesURL();

   void performCheck(bool manuallyInvoked);

protected slots:
   void manifestDownloadError(const QString& message);
   void manifestDownloadComplete(const QByteArray& data);

private:
   QMainWindow* pOwnerWindow_;
};

} // namespace desktop

#endif // DESKTOP_UPDATE_CHECKER_HPP
