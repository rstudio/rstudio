/*
 * DesktopUtils.cpp
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

#include "DesktopUtils.hpp"

namespace desktop {

QMessageBox::Icon safeMessageBoxIcon(QMessageBox::Icon icon)
{
   // if a gtk theme has a missing or corrupt icon for one of the stock
   // dialog images, qt crashes when attempting to show the dialog
#ifdef Q_WS_X11
   return QMessageBox::NoIcon;
#else
   return icon;
#endif
}

void showMessageBox(QMessageBox::Icon icon,
                    QWidget *parent,
                    const QString &title,
                    const QString& text)
{
   QMessageBox messageBox(safeMessageBoxIcon(icon),
                          title,
                          text,
                          QMessageBox::Ok,
                           parent);
   messageBox.setWindowModality(Qt::WindowModal);
   messageBox.show();
}

void showWarning(QWidget *parent,
                 const QString &title,
                 const QString& text)
{
   showMessageBox(QMessageBox::Warning, parent, title, text);
}




} // namespace desktop
