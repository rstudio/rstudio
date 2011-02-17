/*
 * DesktopOptions.cpp
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

#include "DesktopOptions.hpp"

#include <QtGui>

#include <core/Error.hpp>
#include <core/Random.hpp>
#include <core/system/System.hpp>

#include "config.h"

using namespace core;

namespace desktop {

#ifdef _WIN32
// Defined in DesktopRVersion.cpp
QString binDirToHomeDir(QString binDir);
#endif

QString scratchPath;

Options& options()
{
   static Options singleton;
   return singleton;
}

void Options::restoreMainWindowBounds(QMainWindow* win)
{
   settings_.beginGroup("mainwindow");

   // If we restore to a smaller monitor than when we saved, let the system
   // decide where to position the window.
   QSize desktopSize = settings_.value("desktopSize", QSize(0, 0)).toSize();
   QSize curDesktopSize = QApplication::desktop()->size();
   if (desktopSize.width() != 0 && desktopSize.height() != 0
       && desktopSize.width() <= curDesktopSize.width()
       && desktopSize.height() <= curDesktopSize.height())
   {
      win->move(settings_.value("pos", QPoint(0, 0)).toPoint());
   }

   QSize size = settings_.value("size", QSize(1024, 768)).toSize()
          .expandedTo(QSize(200, 200))
          .boundedTo(QApplication::desktop()->availableGeometry(win->pos()).size());
   win->resize(size);

   if (settings_.value("fullScreen", false).toBool())
      win->setWindowState(Qt::WindowMaximized);

   settings_.endGroup();
}

void Options::saveMainWindowBounds(QMainWindow* win)
{
   settings_.setValue("mainwindow/fullScreen", win->isFullScreen());
   if (!win->isFullScreen())
   {
     settings_.setValue("mainwindow/size", win->size());
     settings_.setValue("mainwindow/pos", win->pos());
     settings_.setValue("mainwindow/desktopSize", QApplication::desktop()->size());
   }
}

QString Options::portNumber() const
{
   // lookup / generate on demand
   if (portNumber_.length() == 0)
   {
      // Use a random-ish port number to avoid collisions between different
      // instances of rdesktop-launched rsessions
      int base = std::abs(core::random::uniformRandomInteger<int>());
      portNumber_ = QString::number((base % 40000) + 8080);
   }

   return portNumber_;
}

namespace {
QString findFirstMatchingFont(const QStringList& fonts, QString defaultFont)
{
   QStringList families = QFontDatabase().families();
   for (int i = 0; i < fonts.size(); i++)
      if (families.contains(fonts.at(i)))
         return fonts.at(i);
   return defaultFont;
}
} // anonymous namespace

QString Options::proportionalFont() const
{
   QStringList fontList;
   fontList <<
           "Lucida Sans" << "DejaVu Sans" <<  // Linux
           "Lucida Grande" <<          // Mac
           "Segoe UI" << "Verdana" <<  // Windows
           "Helvetica";
   return "\"" + findFirstMatchingFont(fontList, "sans-serif") + "\"";
}

QString Options::fixedWidthFont() const
{
   // NB: Windows has "Lucida Console" and "Consolas" reversed vs.
   // the list in WebThemeFontLoader (in the GWT codebase). This is
   // because Consolas is more attractive but has a spacing issue
   // in Win/Desktop mode (U+200B shows up as a space, and we use it
   // in the History pane).
   QStringList fontList;
   fontList <<
           "Droid Sans Mono" << "DejaVu Sans Mono" << // Linux
           "Monaco" <<                      // Mac
           "Lucida Console" << "Consolas"   // Windows;
           ;
   return "\"" + findFirstMatchingFont(fontList, "monospace") + "\"";
}


#ifdef _WIN32
QString Options::rBinDir() const
{
   // HACK: If RBinDir doesn't appear at all, that means the user has never
   // specified a preference for R64 vs. 32-bit R. In this situation we should
   // accept either. We'll distinguish between this case (where preferR64
   // should be ignored) and the other case by using null for this case and
   // empty string for the other.
   if (!settings_.contains("RBinDir"))
      return QString::null;

   QString value = settings_.value("RBinDir").toString();
   return value.isNull() ? QString("") : value;
}

void Options::setRBinDir(QString path)
{
   settings_.setValue("RBinDir", path);
}

bool Options::preferR64() const
{
   if (!core::system::isWin64())
      return false;

   if (!settings_.contains("PreferR64"))
      return true;
   return settings_.value("PreferR64").toBool();
}

void Options::setPreferR64(bool preferR64)
{
   settings_.setValue("PreferR64", preferR64);
}
#endif

QString Options::rHome() const
{
#ifdef _WIN32
   return binDirToHomeDir(rBinDir());
#else
   return CONFIG_R_HOME_PATH;
#endif
}

QString Options::rDocPath() const
{
#ifdef _WIN32
   QString home = rHome();
   if (!home.isEmpty())
      return QDir(home).absoluteFilePath("doc");
   else
      return QString();
#else
   return CONFIG_R_DOC_PATH;
#endif
}


FilePath Options::supportingFilePath() const
{
   if (supportingFilePath_.empty())
   {
      // default to install path
      core::system::installPath("..",
                                QApplication::argc(),
                                QApplication::argv(),
                                &supportingFilePath_);

      // adapt for OSX resource bundles
#ifdef __APPLE__
         if (supportingFilePath_.complete("Info.plist").exists())
            supportingFilePath_ = supportingFilePath_.complete("Resources");
#endif
   }
   return supportingFilePath_;
}

QString Options::defaultCRANmirrorName() const
{
   return settings_.value("CRANMirrorName").toString();
}

QString Options::defaultCRANmirrorURL() const
{
   return settings_.value("CRANMirrorURL").toString();
}

void Options::setDefaultCRANmirror(QString name, QString url)
{
   settings_.setValue("CRANMirrorName", name);
   settings_.setValue("CRANMirrorURL", url);
}

// Returns SAVE_YES, SAVE_NO, or SAVE_ASK
int Options::saveWorkspaceOnExit() const
{
   return settings_.value("saveWorkspace", 2).toInt();
}

void Options::setSaveWorkspaceOnExit(int value)
{
   settings_.setValue("saveWorkspace", value);
}

QStringList Options::ignoredUpdateVersions() const
{
   return settings_.value("ignoredUpdateVersions", QStringList()).toStringList();
}

void Options::setIgnoredUpdateVersions(const QStringList& ignoredVersions)
{
   settings_.setValue("ignoredUpdateVersions", ignoredVersions);
}

core::FilePath Options::scratchTempDir(core::FilePath defaultPath)
{
   core::FilePath dir(scratchPath.toStdString());

   if (!dir.empty() && dir.exists())
   {
      dir = dir.childPath("tmp");
      core::Error error = dir.ensureDirectory();
      if (!error)
         return dir;
   }
   return defaultPath;
}

void Options::cleanUpScratchTempDir()
{
   core::FilePath temp = scratchTempDir(core::FilePath());
   if (!temp.empty())
      temp.removeIfExists();
}

} // namespace desktop
