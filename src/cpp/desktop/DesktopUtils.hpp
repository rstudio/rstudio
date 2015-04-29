/*
 * DesktopUtils.hpp
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

#ifndef DESKTOP_UTILS_HPP
#define DESKTOP_UTILS_HPP

#include <QUrl>
#include <QMessageBox>
#include <QMainWindow>
#include <QFileDialog>

namespace rstudio {
namespace core {
   class FilePath;
}
}

namespace rstudio {
namespace desktop {

void reattachConsoleIfNecessary();

core::FilePath userLogPath();

double devicePixelRatio(QMainWindow* pMainWindow);

bool isOSXMavericks();

void raiseAndActivateWindow(QWidget* pWindow);

void closeWindow(QWidget* pWindow);

QMessageBox::Icon safeMessageBoxIcon(QMessageBox::Icon icon);

bool showYesNoDialog(QMessageBox::Icon icon,
                     QWidget *parent,
                     const QString &title,
                     const QString& text);

void showMessageBox(QMessageBox::Icon icon,
                    QWidget *parent,
                    const QString &title,
                    const QString& text);

void showWarning(QWidget *parent, const QString &title, const QString& text);

void showInfo(QWidget* parent, const QString& title, const QString& text);

void showFileError(const QString& action,
                   const QString& file,
                   const QString& error);

void launchProjectInNewInstance(QString projectFilename);

bool isFixedWidthFont(const QFont& font);

void openUrl(const QUrl& url);

void enableFullscreenMode(QMainWindow* pMainWindow, bool primary);
void toggleFullscreenMode(QMainWindow* pMainWindow);
bool supportsFullscreenMode(QMainWindow* pMainWindow);

void initializeLang();

double getDpiZoomScaling();
int getDpi();

QFileDialog::Options standardFileDialogOptions();

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_UTILS_HPP
