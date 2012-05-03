/*
 * DesktopOptions.hpp
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

class Options;
Options& options();

class Options : boost::noncopyable
{
public:
   void restoreMainWindowBounds(QMainWindow* window);
   void saveMainWindowBounds(QMainWindow* window);
   QString portNumber() const;
   QString newPortNumber();

   QString proportionalFont() const;
   QString fixedWidthFont() const;
   void setFixedWidthFont(QString font);

#ifdef _WIN32
   // If "", then use automatic detection
   QString rBinDir() const;
   void setRBinDir(QString path);

   bool preferR64() const;
   void setPreferR64(bool preferR64);
#endif

   core::FilePath scriptsPath() const;
   void setScriptsPath(const core::FilePath& scriptsPath);

   core::FilePath executablePath() const;
   core::FilePath supportingFilePath() const;

#ifdef _WIN32
   core::FilePath urlopenerPath() const;
   core::FilePath rsinversePath() const;
#endif

   QStringList ignoredUpdateVersions() const;
   void setIgnoredUpdateVersions(const QStringList& ignoredVersions);

   core::FilePath scratchTempDir(core::FilePath defaultPath=core::FilePath());
   void cleanUpScratchTempDir();

   bool webkitDevTools();

private:
   Options() : settings_(FORMAT, QSettings::UserScope,
                         QString::fromAscii("RStudio"),
                         QString::fromAscii("desktop"))
   {
   }
   friend Options& options();

   QSettings settings_;
   core::FilePath scriptsPath_;
   mutable core::FilePath executablePath_;
   mutable core::FilePath supportingFilePath_;
   mutable QString portNumber_;
};

} // namespace desktop

#endif // DESKTOP_OPTIONS_HPP
