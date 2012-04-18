/*
 * DesktopUpdateAvailableDialog.hpp
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

#ifndef DESKTOPUPDATEAVAILABLEDIALOG_HPP
#define DESKTOPUPDATEAVAILABLEDIALOG_HPP

#include <QDialog>

namespace Ui {
   class DesktopUpdateAvailableDialog;
}

struct DesktopUpdateInfo
{
   DesktopUpdateInfo() :
         isUrgent(false)
   {
   }

   QString currentVersion;
   QString updatedVersion;
   QString updateURL;
   QString updateMessage;
   bool isUrgent;
};

class DesktopUpdateAvailableDialog : public QDialog
{
   Q_OBJECT

public:
   explicit DesktopUpdateAvailableDialog(const DesktopUpdateInfo& updateInfo,
                                         QWidget *parent = 0);
   ~DesktopUpdateAvailableDialog();

protected slots:
   void permanentlyIgnoreUpdate();

public:
   static const int Ignored = 2;

private:
   DesktopUpdateInfo updateInfo_;
   Ui::DesktopUpdateAvailableDialog *ui;
};

#endif // DESKTOPUPDATEAVAILABLEDIALOG_HPP
