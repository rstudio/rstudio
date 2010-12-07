/*
 * DesktopOptions.hpp
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

#ifndef DESKTOP_OPTIONS_HPP
#define DESKTOP_OPTIONS_HPP

#include <boost/noncopyable.hpp>

#include <QDir>
#include <QMainWindow>
#include <QSettings>

#include <core/FilePath.hpp>

#if defined(__APPLE__)
#define FORMAT QSettings::NativeFormat
#else
#define FORMAT QSettings::IniFormat
#endif

namespace desktop {

#define SAVE_YES 0
#define SAVE_NO 1
#define SAVE_ASK 2

class Options;
Options& options();

class Options : boost::noncopyable
{
public:
   void restoreMainWindowBounds(QMainWindow* window);
   void saveMainWindowBounds(QMainWindow* window);
   QString portNumber() const;

   QString proportionalFont() const;
   QString fixedWidthFont() const;

#ifdef _WIN32
   // If "", then use automatic detection
   QString rBinDir() const;
   void setRBinDir(QString path);

   bool preferR64() const;
   void setPreferR64(bool preferR64);
#endif

   QString rHome() const;

   QString rDocPath() const;

   core::FilePath supportingFilePath() const;

   QString defaultCRANmirrorName() const;
   QString defaultCRANmirrorURL() const;
   void setDefaultCRANmirror(QString name,
                             QString url);

   int saveWorkspaceOnExit() const;
   void setSaveWorkspaceOnExit(int value);

   QStringList ignoredUpdateVersions() const;
   void setIgnoredUpdateVersions(const QStringList& ignoredVersions);

   core::FilePath scratchTempDir(core::FilePath defaultPath=core::FilePath());
   void cleanUpScratchTempDir();

private:
   Options() : settings_(FORMAT, QSettings::UserScope, "RStudio", "desktop")
   {
   }
   friend Options& options();

   QSettings settings_;
   mutable core::FilePath supportingFilePath_;
};

} // namespace desktop

#endif // DESKTOP_OPTIONS_HPP
