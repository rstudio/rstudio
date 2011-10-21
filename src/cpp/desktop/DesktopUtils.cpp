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

#include <QProcess>
#include <QPushButton>

#include "DesktopOptions.hpp"

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


bool showYesNoDialog(QMessageBox::Icon icon,
                     QWidget *parent,
                     const QString &title,
                     const QString& text)
{
   // basic message box attributes
   QMessageBox messageBox(safeMessageBoxIcon(icon),
                          title,
                          text,
                          QMessageBox::NoButton,
                          parent);
   messageBox.setWindowModality(Qt::WindowModal);

   // initialize buttons
   QPushButton* pYes = new QPushButton(QString::fromUtf8("Yes"));
   messageBox.addButton(pYes, QMessageBox::YesRole);
   messageBox.addButton(new QPushButton(QString::fromUtf8("No")), QMessageBox::NoRole);
   messageBox.setDefaultButton(pYes);

   // show the dialog modally
   messageBox.exec();

   // return true if the user clicked yes
   return messageBox.clickedButton() == pYes;
}

void showMessageBox(QMessageBox::Icon icon,
                    QWidget *parent,
                    const QString &title,
                    const QString& text)
{
   // basic message box attributes
   QMessageBox messageBox(safeMessageBoxIcon(icon),
                          title,
                          text,
                          QMessageBox::NoButton,
                          parent);
   messageBox.setWindowModality(Qt::WindowModal);
   messageBox.addButton(new QPushButton(QString::fromUtf8("OK")), QMessageBox::AcceptRole);
   messageBox.exec();
}

void showWarning(QWidget *parent, const QString &title, const QString& text)
{
   showMessageBox(QMessageBox::Warning, parent, title, text);
}

void showInfo(QWidget* parent, const QString& title, const QString& text)
{
   showMessageBox(QMessageBox::Information, parent, title, text);
}

void launchProjectInNewInstance(QString projectFilename)
{
   // launch the new instance
   QStringList args;
   args.append(projectFilename);
   QString exePath = QString::fromUtf8(
      desktop::options().executablePath().absolutePath().c_str());
   QProcess::startDetached(exePath, args);
}


} // namespace desktop
