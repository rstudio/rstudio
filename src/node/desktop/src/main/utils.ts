/*
 * utils.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import fs from 'fs';
import path from 'path';
import { app } from 'electron';

import { Xdg } from '../core/xdg';
import { getenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { getRStudioVersion } from './product-info';

export function reattachConsoleIfNecessary(): void {
  // TODO Windows-only
}

export function userLogPath(): FilePath {
  return Xdg.userDataDir().completeChildPath('log');
}

export function userWebCachePath(): FilePath {
  return Xdg.userDataDir().completeChildPath('web-cache');
}

export function devicePixelRatio(/*QMainWindow * pMainWindow*/): number {
  // TODO
  return 1.0;
}

export function randomString(): string {
  return Math.trunc(Math.random() * 2147483647).toString();
}

export function initializeLang(): void {
  if (process.platform !== 'darwin') {
    return;
  }

  let lang: string | null = null;

  // We try to simulate the behavior of R.app.

  // Highest precedence: force.LANG. If it has a value, use it.

  // TODO: native, see https://github.com/electron/electron/issues/17031
  // ---------------------------------------------------------------------
  // NSUserDefaults * defaults =[NSUserDefaults standardUserDefaults];
  // [defaults addSuiteNamed:@"org.R-project.R"];
  // lang = [defaults stringForKey:@"force.LANG"];
  // if (lang && ![lang length]) {
  // }

  // Next highest precedence: ignore.system.locale. If it has a value,
  // hardcode to en_US.UTF-8.
  // if (!lang && [defaults boolForKey:@"ignore.system.locale"])
  // {
  //   lang = @"en_US.UTF-8";
  // }
  // ---------------------------------------------------------------------

  // Next highest precedence: LANG environment variable.
  if (!lang) {
    const envLang = getenv('LANG');
    if (!envLang) {
      lang = envLang;
    }
  }

  // // Next highest precedence: Try to figure out language from the current
  // // locale.
  // if (!lang) {
  //   lang = readSystemLocale();
  // }

  // // None of the above worked. Just hard code it.
  // if (!lang) {
  //   lang = @"en_US.UTF-8";
  // }

  // const char* clang =[lang cStringUsingEncoding: NSASCIIStringEncoding];
  // core:: system:: setenv("LANG", clang);
  // core:: system:: setenv("LC_CTYPE", clang);

  // initializeSystemPrefs();
}

// NSString* createAliasedPath(NSString* path);
// NSString* resolveAliasedPath(NSString* path);

// QString runFileDialog(NSSavePanel* panel);

// } // namespace desktop
// } // namespace rstudio

// #endif // DESKTOP_UTILS_MAC_HPP

// /***********************************************************************************/

// /*
//  * DesktopUtilsMac.mm
//  *
//  * Copyright (C) 2021 by RStudio, PBC
//  *
//  * Unless you have received this program directly from RStudio pursuant
//  * to the terms of a commercial license agreement with RStudio, then
//  * this program is licensed to you under the terms of version 3 of the
//  * GNU Affero General Public License. This program is distributed WITHOUT
//  * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
//  * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
//  * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
//  *
//  */

// #include "DesktopUtilsMac.hpp"

// #include <boost/algorithm/string/predicate.hpp>

// #include <core/StringUtils.hpp>
// #include <core/system/Environment.hpp>

// #import <Foundation/NSString.h>
// #import <AppKit/NSFontManager.h>
// #import <AppKit/NSView.h>
// #import <AppKit/NSWindow.h>
// #import <AppKit/NSOpenPanel.h>

// #include "DockMenu.hpp"

// using namespace rstudio::core;

// namespace rstudio {
// namespace desktop {

// QString getFixedWidthFontList()
// {
//    NSArray* fonts = [[NSFontManager sharedFontManager]
//                          availableFontNamesWithTraits: NSFixedPitchFontMask];
//    return QString::fromNSString([fonts componentsJoinedByString: @"\n"]);
// }

// namespace {

// NSWindow* nsWindowForMainWindow(QMainWindow* pMainWindow)
// {
//    NSView *nsview = (NSView *) pMainWindow->winId();
//    return [nsview window];
// }

// static DockMenu* s_pDockMenu;

// void initializeSystemPrefs()
// {
//    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
//    [defaults setBool:NO forKey: @"NSFunctionBarAPIEnabled"];

//    // Remove (disable) the "Start Dictation..." and "Emoji & Symbols" menu items from the "Edit" menu
//    [defaults setBool:YES forKey:@"NSDisabledDictationMenuItem"];
//    [defaults setBool:YES forKey:@"NSDisabledCharacterPaletteMenuItem"];

//    // Remove the "Enter Full Screen" menu item from the "View" menu
//    [defaults setBool:NO forKey:@"NSFullScreenMenuItemEverywhere"];
// }

// } // anonymous namespace

// double devicePixelRatio(QMainWindow* pMainWindow)
// {
//    NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);

//    if ([pWindow respondsToSelector:@selector(backingScaleFactor)])
//    {
//       return [pWindow backingScaleFactor];
//    }
//    else
//    {
//       return 1.0;
//    }
// }

export function isMacOS(): boolean {
  return process.platform === 'darwin';
}

// bool isCentOS()
// {
//    return false;
// }

// namespace {

// bool supportsFullscreenMode(NSWindow* pWindow)
// {
//    return [pWindow respondsToSelector:@selector(toggleFullScreen:)];
// }

// } // anonymous namespace

// bool supportsFullscreenMode(QMainWindow* pMainWindow)
// {
//    NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);
//    return supportsFullscreenMode(pWindow);
// }

// // see: https://bugreports.qt-project.org/browse/QTBUG-21607
// // see: https://developer.apple.com/library/mac/#documentation/General/Conceptual/MOSXAppProgrammingGuide/FullScreenApp/FullScreenApp.html
// void enableFullscreenMode(QMainWindow* pMainWindow, bool primary)
// {
//    NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);

//    if (supportsFullscreenMode(pWindow))
//    {
//       NSWindowCollectionBehavior behavior = [pWindow collectionBehavior];
//       behavior = behavior | (primary ?
//                              NSWindowCollectionBehaviorFullScreenPrimary :
//                              NSWindowCollectionBehaviorFullScreenAuxiliary);
//       [pWindow setCollectionBehavior:behavior];
//    }
// }

// void toggleFullscreenMode(QMainWindow* pMainWindow)
// {
//    NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);
//    if (supportsFullscreenMode(pWindow))
//       [pWindow toggleFullScreen:nil];
// }

// namespace {

// NSString* readSystemLocale()
// {
//    using namespace core;
//    using namespace core::system;
//    Error error;

//    // First, read all available locales so we can validate whether we've received
//    // a valid locale.
//    ProcessResult localeResult;
//    error = runCommand("/usr/bin/locale -a", ProcessOptions(), &localeResult);
//    if (error)
//       LOG_ERROR(error);

//    std::string allLocales = localeResult.stdOut;

//    // Now, try looking for the active locale using NSLocale.
//    std::string localeIdentifier = [[[NSLocale currentLocale] localeIdentifier] UTF8String];

//    // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
//    auto idx = localeIdentifier.find('@');
//    if (idx != std::string::npos)
//       localeIdentifier = localeIdentifier.substr(0, idx);

//    // Enforce a UTF-8 locale.
//    localeIdentifier += ".UTF-8";

//    if (allLocales.find(localeIdentifier) != std::string::npos)
//       return [NSString stringWithCString: localeIdentifier.c_str()];

//    // If that failed, fall back to reading the defaults value. Note that Mojave
//    // (at least with 10.14) reports the wrong locale above and so we rely on this
//    // as a fallback.
//    ProcessResult defaultsResult;
//    error = runCommand("defaults read NSGlobalDomain AppleLocale", ProcessOptions(), &defaultsResult);
//    if (error)
//       LOG_ERROR(error);

//    std::string defaultsLocale = string_utils::trimWhitespace(defaultsResult.stdOut);

//    // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
//    idx = defaultsLocale.find('@');
//    if (idx != std::string::npos)
//       defaultsLocale = defaultsLocale.substr(0, idx);

//    // Enforce a UTF-8 locale.
//    defaultsLocale += ".UTF-8";

//    if (allLocales.find(defaultsLocale) != std::string::npos)
//       return [NSString stringWithUTF8String: defaultsLocale.c_str()];

//    return nullptr;
// }

// } // end anonymous namespace

// void initializeLang()
// {
//    // Not sure what the memory management rules are here, i.e. whether an
//    // autorelease pool is active. Just let it leak, since we're only calling
//    // this once (at the time of this writing).

//    // We try to simulate the behavior of R.app.

//    NSString* lang = nil;

//    // Highest precedence: force.LANG. If it has a value, use it.
//    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
//    [defaults addSuiteNamed:@"org.R-project.R"];
//    lang = [defaults stringForKey:@"force.LANG"];
//    if (lang && ![lang length])
//    {
//       // If force.LANG is present but empty, don't touch LANG at all.
//       return;
//    }

//    // Next highest precedence: ignore.system.locale. If it has a value,
//    // hardcode to en_US.UTF-8.
//    if (!lang && [defaults boolForKey:@"ignore.system.locale"])
//    {
//       lang = @"en_US.UTF-8";
//    }

//    // Next highest precedence: LANG environment variable.
//    if (!lang)
//    {
//       std::string envLang = core::system::getenv("LANG");
//       if (!envLang.empty())
//       {
//          lang = [NSString stringWithCString:envLang.c_str()
//                           encoding:NSASCIIStringEncoding];
//       }
//    }

//    // Next highest precedence: Try to figure out language from the current
//    // locale.
//    if (!lang)
//    {
//       lang = readSystemLocale();
//    }

//    // None of the above worked. Just hard code it.
//    if (!lang)
//    {
//       lang = @"en_US.UTF-8";
//    }

//    const char* clang = [lang cStringUsingEncoding:NSASCIIStringEncoding];
//    core::system::setenv("LANG", clang);
//    core::system::setenv("LC_CTYPE", clang);

//    initializeSystemPrefs();
// }

// void finalPlatformInitialize(MainWindow* pMainWindow)
// {
//    // https://bugreports.qt.io/browse/QTBUG-61707
//    [NSWindow setAllowsAutomaticWindowTabbing: NO];

//    if (!s_pDockMenu)
//    {
//       s_pDockMenu = new DockMenu(pMainWindow);
//    }
//    else
//    {
//       s_pDockMenu->setMainWindow(pMainWindow);
//    }
// }

// NSString* createAliasedPath(NSString* path)
// {
//    if (path == nil || [path length] == 0)
//       return @"";

//    std::string aliased = FilePath::createAliasedPath(
//       FilePath([path UTF8String]),
//       userHomePath());

//    return [NSString stringWithUTF8String: aliased.c_str()];
// }

// NSString* resolveAliasedPath(NSString* path)
// {
//    if (path == nil)
//       path = @"";

//    FilePath resolved = FilePath::resolveAliasedPath(
//       [path UTF8String],
//       userHomePath());

//    return [NSString stringWithUTF8String: resolved.getAbsolutePath().c_str()];
// }

// QString runFileDialog(NSSavePanel* panel)
// {
//    NSString* path = @"";
//    long int result = [panel runModal];
//    @try
//    {
//       if (result == NSOKButton)
//       {
//          path = [[panel URL] path];
//       }
//    }
//    @catch (NSException* e)
//    {
//       throw e;
//    }

//    return QString::fromNSString(createAliasedPath(path));
// }

// QString browseDirectory(const QString& qCaption,
//                         const QString& qLabel,
//                         const QString& qDir,
//                         QWidget* pOwner)
// {
//    NSString* caption = qCaption.toNSString();
//    NSString* label = qLabel.toNSString();
//    NSString* dir = qDir.toNSString();

//    dir = resolveAliasedPath(dir);

//    NSOpenPanel* panel = [NSOpenPanel openPanel];
//    [panel setTitle: caption];
//    [panel setPrompt: label];
//    [panel setDirectoryURL: [NSURL fileURLWithPath:
//                            [dir stringByStandardizingPath]]];
//    [panel setCanChooseFiles: false];
//    [panel setCanChooseDirectories: true];
//    [panel setCanCreateDirectories: true];

//    return runFileDialog(panel);
// }

// } // namespace desktop
// } // namespace rstudio

// /***********************************************************************************/

// /*
//  * DesktopUtils.hpp
//  *
//  * Copyright (C) 2021 by RStudio, PBC
//  *
//  * Unless you have received this program directly from RStudio pursuant
//  * to the terms of a commercial license agreement with RStudio, then
//  * this program is licensed to you under the terms of version 3 of the
//  * GNU Affero General Public License. This program is distributed WITHOUT
//  * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
//  * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
//  * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
//  *
//  */

// #ifndef DESKTOP_UTILS_HPP
// #define DESKTOP_UTILS_HPP

// #include <QFileDialog>
// #include <QMainWindow>
// #include <QMessageBox>
// #include <QUrl>
// #include <QWebEnginePage>

// #include <core/system/Process.hpp>

// namespace rstudio {
// namespace core {
//    class FilePath;
// }
// }

// namespace rstudio {
// namespace desktop {

// class MainWindow;

// void reattachConsoleIfNecessary();

// rstudio::core::FilePath userLogPath();
// rstudio::core::FilePath userWebCachePath();

// double devicePixelRatio(QMainWindow* pMainWindow);

// bool isWindows();
// bool isMacOS();
// bool isCentOS();
// bool isGnomeDesktop();

// QString getFixedWidthFontList();

// void applyDesktopTheme(QWidget* window, bool isDark);

// void raiseAndActivateWindow(QWidget* pWindow);

// void moveWindowBeneath(QWidget* pTop, QWidget* pBottom);

// void closeWindow(QWidget* pWindow);

// QMessageBox::Icon safeMessageBoxIcon(QMessageBox::Icon icon);

// bool showYesNoDialog(QMessageBox::Icon icon,
//                      QWidget *parent,
//                      const QString &title,
//                      const QString& text,
//                      const QString& informativeText,
//                      bool yesDefault);

// void showMessageBox(QMessageBox::Icon icon,
//                     QWidget *parent,
//                     const QString &title,
//                     const QString& text,
//                     const QString& informativeText);

// void showError(QWidget *parent,
//                const QString &title,
//                const QString& text,
//                const QString& informativeText);

// void showWarning(QWidget *parent,
//                  const QString &title,
//                  const QString& text,
//                  const QString& informativeText);

// void showInfo(QWidget* parent,
//               const QString& title,
//               const QString& text,
//               const QString& informativeText);

// void showFileError(const QString& action,
//                    const QString& file,
//                    const QString& error);

// bool isFixedWidthFont(const QFont& font);

// void openFile(const QString& file);
// void openUrl(const QUrl& url);

// void enableFullscreenMode(QMainWindow* pMainWindow, bool primary);
// void toggleFullscreenMode(QMainWindow* pMainWindow);
// bool supportsFullscreenMode(QMainWindow* pMainWindow);

// void initializeLang();

// // Platform-specific initialization requiring main window object.
// void finalPlatformInitialize(MainWindow* pMainWindow);

// double getDpiZoomScaling();
// int getDpi();

// QFileDialog::Options standardFileDialogOptions();

// QString browseDirectory(const QString& caption,
//                         const QString& label,
//                         const QString& dir,
//                         QWidget* pOwner = nullptr);

// core::FilePath userHomePath();
// QString createAliasedPath(const QString& path);
// QString resolveAliasedPath(const QString& path);

// } // namespace desktop
// } // namespace rstudio

// #endif // DESKTOP_UTILS_HPP

// /***********************************************************************************/

// /*
//  * DesktopUtils.cpp
//  *
//  * Copyright (C) 2021 by RStudio, PBC
//  *
//  * Unless you have received this program directly from RStudio pursuant
//  * to the terms of a commercial license agreement with RStudio, then
//  * this program is licensed to you under the terms of version 3 of the
//  * GNU Affero General Public License. This program is distributed WITHOUT
//  * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
//  * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
//  * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
//  *
//  */

// #include "DesktopUtils.hpp"

// #include <set>

// #include <QPushButton>
// #include <QTimer>
// #include <QDesktopServices>

// #include <core/FileSerializer.hpp>
// #include <core/system/Environment.hpp>
// #include <core/system/Xdg.hpp>

// #include "DesktopOptions.hpp"
// #include "DesktopMainWindow.hpp"

// #ifdef Q_OS_WIN
// #include <windows.h>
// #endif

// using namespace rstudio::core;

// namespace rstudio {
// namespace desktop {

// #ifdef Q_OS_WIN

// void reattachConsoleIfNecessary()
// {
//    if (::AttachConsole(ATTACH_PARENT_PROCESS))
//    {
//       freopen("CONOUT$","wb",stdout);
//       freopen("CONOUT$","wb",stderr);
//       freopen("CONIN$","rb",stdin);
//       std::ios::sync_with_stdio();
//    }
// }

// #else

// void reattachConsoleIfNecessary()
// {

// }

// #endif

// // NOTE: this code is duplicated in diagnostics as well (and also in
// // SessionOptions.hpp although the code path isn't exactly the same)
// FilePath userLogPath()
// {
//    return core::system::xdg::userDataDir().completeChildPath("log");
// }

// FilePath userWebCachePath()
// {
//    return core::system::xdg::userDataDir().completeChildPath("web-cache");
// }

// bool isWindows()
// {
// #ifdef Q_OS_WIN
//    return true;
// #else
//    return false;
// #endif
// }

// #ifndef Q_OS_MAC
// double devicePixelRatio(QMainWindow* pMainWindow)
// {
//    return 1.0;
// }

// bool isMacOS()
// {
//    return false;
// }

// // NOTE: also RHEL
// bool isCentOS()
// {
//    FilePath redhatRelease("/etc/redhat-release");
//    if (!redhatRelease.exists())
//       return false;

//    std::string contents;
//    Error error = readStringFromFile(redhatRelease, &contents);
//    if (error)
//       return false;

//    return contents.find("CentOS") != std::string::npos ||
//           contents.find("Red Hat Enterprise Linux") != std::string::npos;
// }

// QString browseDirectory(const QString& caption,
//                         const QString& label,
//                         const QString& dir,
//                         QWidget* pOwner)
// {
//    QFileDialog dialog(
//             pOwner,
//             caption,
//             resolveAliasedPath(dir));

//    dialog.setLabelText(QFileDialog::Accept, label);
//    dialog.setFileMode(QFileDialog::Directory);
//    dialog.setOption(QFileDialog::ShowDirsOnly, true);
//    dialog.setWindowModality(Qt::WindowModal);

//    QString result;
//    if (dialog.exec() == QDialog::Accepted)
//       result = dialog.selectedFiles().value(0);

//    if (pOwner)
//       raiseAndActivateWindow(pOwner);

//    return createAliasedPath(result);
// }

// #endif

// bool isGnomeDesktop()
// {
//    if (core::system::getenv("DESKTOP_SESSION") == "gnome")
//       return true;

//    std::string desktop = core::system::getenv("XDG_CURRENT_DESKTOP");
//    if (desktop.find("GNOME") != std::string::npos)
//       return true;

//    return false;
// }

// #ifndef Q_OS_MAC

// QString getFixedWidthFontList()
// {
//    return desktopInfo().getFixedWidthFontList();
// }

// #endif

// void applyDesktopTheme(QWidget* window, bool isDark)
// {
// #ifndef Q_OS_MAC
//    std::string lightSheetName = isWindows()
//          ? "rstudio-windows-light.qss"
//          : "rstudio-gnome-light.qss";

//    std::string darkSheetName = isWindows()
//          ? "rstudio-windows-dark.qss"
//          : "rstudio-gnome-dark.qss";

//    FilePath stylePath = isDark
//          ? options().resourcesPath().completePath("stylesheets").completePath(darkSheetName)
//          : options().resourcesPath().completePath("stylesheets").completePath(lightSheetName);

//    std::string stylesheet;
//    Error error = core::readStringFromFile(stylePath, &stylesheet);
//    if (error)
//       LOG_ERROR(error);

//    window->setStyleSheet(QString::fromStdString(stylesheet));
// #endif
// }

// #ifndef Q_OS_MAC

// void enableFullscreenMode(QMainWindow* pMainWindow, bool primary)
// {

// }

// void toggleFullscreenMode(QMainWindow* pMainWindow)
// {

// }

// bool supportsFullscreenMode(QMainWindow* pMainWindow)
// {
//    return false;
// }

// void initializeLang()
// {
// }

// void finalPlatformInitialize(MainWindow* pMainWindow)
// {

// }

// #endif

// void raiseAndActivateWindow(QWidget* pWindow)
// {
//    // WId wid = pWindow->effectiveWinId(); -- gets X11 window id
//    // gtk_window_present_with_time(GTK_WINDOW, timestamp)

//    if (pWindow->isMinimized())
//    {
//       pWindow->setWindowState(
//                      pWindow->windowState() & ~Qt::WindowMinimized);
//    }

//    pWindow->raise();
//    pWindow->activateWindow();
// }

// void moveWindowBeneath(QWidget* pTop, QWidget* pBottom)
// {
// #ifdef WIN32
//    HWND hwndTop = reinterpret_cast<HWND>(pTop->winId());
//    HWND hwndBottom = reinterpret_cast<HWND>(pBottom->winId());
//    ::SetWindowPos(hwndBottom, hwndTop, 0, 0, 0, 0,
//                   SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
// #endif
//    // not currently supported on Linux--Qt doesn't provide a way to view or
//    // change the window stacking order
// }

// void closeWindow(QWidget* pWindow)
// {
//    pWindow->close();
// }

// QMessageBox::Icon safeMessageBoxIcon(QMessageBox::Icon icon)
// {
//    // if a gtk theme has a missing or corrupt icon for one of the stock
//    // dialog images, qt crashes when attempting to show the dialog
// #ifdef Q_OS_LINUX
//    return QMessageBox::NoIcon;
// #else
//    return icon;
// #endif
// }

// bool showYesNoDialog(QMessageBox::Icon icon,
//                      QWidget *parent,
//                      const QString &title,
//                      const QString& text,
//                      const QString& informativeText,
//                      bool yesDefault)
// {
//    // basic message box attributes
//    QMessageBox messageBox(parent);
//    messageBox.setIcon(safeMessageBoxIcon(icon));
//    messageBox.setWindowTitle(title);
//    messageBox.setText(text);
//    if (informativeText.length() > 0)
//       messageBox.setInformativeText(informativeText);
//    messageBox.setWindowModality(Qt::WindowModal);
//    messageBox.setWindowFlag(Qt::WindowContextHelpButtonHint, false);

//    // initialize buttons
//    QPushButton* pYes = messageBox.addButton(QMessageBox::Yes);
//    QPushButton* pNo = messageBox.addButton(QMessageBox::No);
//    if (yesDefault)
//       messageBox.setDefaultButton(pYes);
//    else
//       messageBox.setDefaultButton(pNo);

//    // show the dialog modally
//    messageBox.exec();

//    // return true if the user clicked yes
//    return messageBox.clickedButton() == pYes;
// }

// void showMessageBox(QMessageBox::Icon icon,
//                     QWidget *parent,
//                     const QString &title,
//                     const QString& text,
//                     const QString& informativeText)
// {
//    QMessageBox messageBox(parent);
//    messageBox.setIcon(safeMessageBoxIcon(icon));
//    messageBox.setWindowTitle(title);
//    messageBox.setText(text);
//    if (informativeText.length() > 0)
//       messageBox.setInformativeText(informativeText);
//    messageBox.setWindowModality(Qt::WindowModal);
//    messageBox.setWindowFlag(Qt::WindowContextHelpButtonHint, false);
//    messageBox.addButton(new QPushButton(QString::fromUtf8("OK")), QMessageBox::AcceptRole);
//    messageBox.exec();
// }

// void showError(QWidget *parent,
//                const QString &title,
//                const QString& text,
//                const QString& informativeText)
// {
//    showMessageBox(QMessageBox::Critical, parent, title, text, informativeText);
// }

// void showWarning(QWidget *parent,
//                  const QString &title,
//                  const QString& text,
//                  const QString& informativeText)
// {
//    showMessageBox(QMessageBox::Warning, parent, title, text, informativeText);
// }

// void showInfo(QWidget* parent,
//               const QString& title,
//               const QString& text,
//               const QString& informativeText)
// {
//    showMessageBox(QMessageBox::Information, parent, title, text, informativeText);
// }

// void showFileError(const QString& action,
//                    const QString& file,
//                    const QString& error)
// {
//    QString msg = QString::fromUtf8("Error ") + action +
//                  QString::fromUtf8(" ") + file +
//                  QString::fromUtf8(" - ") + error;
//    showMessageBox(QMessageBox::Critical,
//                   nullptr,
//                   QString::fromUtf8("File Error"),
//                   msg,
//                   QString());
// }

// bool isFixedWidthFont(const QFont& font)
// {
//    QFontMetrics metrics(font);
//    int width = metrics.horizontalAdvance(QChar::fromLatin1(' '));
//    char chars[] = {'m', 'i', 'A', '/', '-', '1', 'l', '!', 'x', 'X', 'y', 'Y'};
//    for (char i : chars)
//    {
//       if (metrics.horizontalAdvance(QChar::fromLatin1(i)) != width)
//          return false;
//    }
//    return true;
// }

// int getDpi()
// {
//    // TODO: we may need to tweak this to ensure that the DPI
//    // discovered respects the screen a particular instance
//    // that RStudio lives on (e.g. for users with multiple
//    // displays with different DPIs)
//    return (int) qApp->primaryScreen()->logicalDotsPerInch();
// }

// double getDpiZoomScaling()
// {
//    // TODO: because Qt is already high-DPI aware and automatically
//    // scales in most scenarios, we no longer need to detect and
//    // apply a custom scale -- but more testing is warranted
//    return 1.0;
// }

// #ifdef _WIN32

// void openFile(const QString& file)
// {
//    return openUrl(QUrl::fromLocalFile(file));
// }

// // on Win32 open urls using our special urlopener.exe -- this is
// // so that the shell exec is made out from under our windows "job"
// void openUrl(const QUrl& url)
// {
//    // we allow default handling for  mailto and file schemes because qt
//    // does custom handling for them and they aren't affected by the chrome
//    //job object issue noted above
//    if (url.scheme() == QString::fromUtf8("mailto") ||
//        url.scheme() == QString::fromUtf8("file"))
//    {
//       QDesktopServices::openUrl(url);
//    }
//    else
//    {
//       core::system::ProcessOptions options;
//       options.breakawayFromJob = true;
//       options.detachProcess = true;

//       std::vector<std::string> args;
//       args.push_back(url.toString().toStdString());

//       core::system::ProcessResult result;
//       Error error = core::system::runProgram(
//             desktop::options().urlopenerPath().getAbsolutePath(),
//             args,
//             "",
//             options,
//             &result);

//       if (error)
//          LOG_ERROR(error);
//       else if (result.exitStatus != EXIT_SUCCESS)
//          LOG_ERROR_MESSAGE(result.stdErr);
//    }
// }

// // Qt 4.8.3 on Win7 (32-bit) has problems with opening the ~ directory
// // (it attempts to navigate to the "Documents library" and then hangs)
// // So we use the Qt file dialog implementations when we are running
// // on Win32
// QFileDialog::Options standardFileDialogOptions()
// {
//    return 0;
// }

// #else

// void openFile(const QString& file)
// {
//    QDesktopServices::openUrl(QUrl::fromLocalFile(file));
// }

// void openUrl(const QUrl& url)
// {
//    QDesktopServices::openUrl(url);
// }

// QFileDialog::Options standardFileDialogOptions()
// {
//    return nullptr;
// }

// #endif

// FilePath userHomePath()
// {
//    return core::system::userHomePath("R_USER|HOME");
// }

// QString createAliasedPath(const QString& path)
// {
//    std::string aliased = FilePath::createAliasedPath(
//          FilePath(path.toUtf8().constData()), desktop::userHomePath());
//    return QString::fromUtf8(aliased.c_str());
// }

// QString resolveAliasedPath(const QString& path)
// {
//    FilePath resolved(FilePath::resolveAliasedPath(path.toUtf8().constData(),
//                                                   userHomePath()));
//    return QString::fromUtf8(resolved.getAbsolutePath().c_str());
// }

// } // namespace desktop
// } // namespace rstudio

export interface VersionInfo  {
  electron: string;
  rstudio?: string;
  node: string;
  v8: string;
}

export function getComponentVersions(): string {
  const componentVers: VersionInfo = process.versions;
  componentVers['rstudio'] = getRStudioVersion();
  return JSON.stringify(componentVers, null, 2);
}

/**
 * Pass additional Chromium arguments set by user via RSTUDIO_CHROMIUM_ARGUMENTS
 * environment variable.
 */
export function augmentCommandLineArguments(): void {
  const user = getenv('RSTUDIO_CHROMIUM_ARGUMENTS');
  if (!user) {
    return;
  }

  const pieces = user.split(' ');
  pieces.forEach((piece) => {
    if (piece.startsWith('-')) {
      app.commandLine.appendSwitch(piece);
    } else {
      app.commandLine.appendArgument(piece);
    }
  });
}

/**
 * Attempt to remove stale lockfiles that might inhibit
 * RStudio startup (currently Windows only). Throws
 * an error only when a stale lockfile exists, but
 * we could not successfully remove it
 */
export function removeStaleOptionsLockfile(): void {
  if (process.platform !== 'win32') {
    return;
  }

  const appData = getenv('APPDATA');
  if (!appData) {
    return;
  }

  const lockFilePath = path.join(appData, 'RStudio/desktop.ini.lock');
  if (!fs.existsSync(lockFilePath)) {
    return;
  }

  const diff = (Date.now() - fs.statSync(lockFilePath).mtimeMs) / 1000;
  if (diff < 10) {
    return;
  }

  fs.unlinkSync(lockFilePath);
}
