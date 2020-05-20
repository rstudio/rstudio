/*
 * DesktopUtils.hpp
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

#ifndef DESKTOP_UTILS_HPP
#define DESKTOP_UTILS_HPP

#include <QFileDialog>
#include <QMainWindow>
#include <QMessageBox>
#include <QUrl>
#include <QWebEnginePage>

#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
   class FilePath;
}
}

namespace rstudio {
namespace desktop {

class MainWindow;

void reattachConsoleIfNecessary();

rstudio::core::FilePath userLogPath();
rstudio::core::FilePath userWebCachePath();

double devicePixelRatio(QMainWindow* pMainWindow);

bool isWindows();
bool isMacOS();
bool isCentOS();
bool isGnomeDesktop();

QString getFixedWidthFontList();

void applyDesktopTheme(QWidget* window, bool isDark);

void raiseAndActivateWindow(QWidget* pWindow);

void moveWindowBeneath(QWidget* pTop, QWidget* pBottom);

void closeWindow(QWidget* pWindow);

QMessageBox::Icon safeMessageBoxIcon(QMessageBox::Icon icon);

bool showYesNoDialog(QMessageBox::Icon icon,
                     QWidget *parent,
                     const QString &title,
                     const QString& text,
                     const QString& informativeText,
                     bool yesDefault);

void showMessageBox(QMessageBox::Icon icon,
                    QWidget *parent,
                    const QString &title,
                    const QString& text,
                    const QString& informativeText);

void showError(QWidget *parent,
               const QString &title,
               const QString& text,
               const QString& informativeText);

void showWarning(QWidget *parent,
                 const QString &title,
                 const QString& text,
                 const QString& informativeText);

void showInfo(QWidget* parent,
              const QString& title,
              const QString& text,
              const QString& informativeText);

void showFileError(const QString& action,
                   const QString& file,
                   const QString& error);

bool isFixedWidthFont(const QFont& font);

void openFile(const QString& file);
void openUrl(const QUrl& url);

void enableFullscreenMode(QMainWindow* pMainWindow, bool primary);
void toggleFullscreenMode(QMainWindow* pMainWindow);
bool supportsFullscreenMode(QMainWindow* pMainWindow);

void initializeLang();

// Platform-specific initialization requiring main window object.
void finalPlatformInitialize(MainWindow* pMainWindow);

double getDpiZoomScaling();
int getDpi();

QFileDialog::Options standardFileDialogOptions();

QString browseDirectory(const QString& caption,
                        const QString& label,
                        const QString& dir,
                        QWidget* pOwner = nullptr);

core::FilePath userHomePath();
QString createAliasedPath(const QString& path);
QString resolveAliasedPath(const QString& path);

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_UTILS_HPP
