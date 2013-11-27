/*
 * DesktopOptions.cpp
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

#include "DesktopOptions.hpp"

#include <QtGui>
#include <QApplication>
#include <QDesktopWidget>

#include <core/Error.hpp>
#include <core/Random.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopUtils.hpp"

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

void Options::initFromCommandLine(const QStringList& arguments)
{
   for (int i=1; i<arguments.size(); i++)
   {
      QString arg = arguments.at(i);
      if (arg == QString::fromUtf8(kRunDiagnosticsOption))
         runDiagnostics_ = true;
   }
}

void Options::restoreMainWindowBounds(QMainWindow* win)
{
   QString key = QString::fromUtf8("mainwindow/geometry");
   if (settings_.contains(key))
      win->restoreGeometry(settings_.value(key).toByteArray());
   else
   {
      QSize size = QSize(1024, 768).boundedTo(
            QApplication::desktop()->availableGeometry().size());
      if (size.width() > 800 && size.height() > 500)
      {
         // Only use default size if it seems sane; otherwise let Qt set it
         win->resize(size);
      }
   }
}

void Options::saveMainWindowBounds(QMainWindow* win)
{
   settings_.setValue(QString::fromUtf8("mainwindow/geometry"),
                      win->saveGeometry());
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

      // recalculate the local peer and set RS_LOCAL_PEER so that
      // rsession and it's children can use it
#ifdef _WIN32
      QString localPeer = QString::fromUtf8("\\\\.\\pipe\\") +
                          portNumber_ + QString::fromUtf8("-rsession");
#else
      QString localPeer = QDir(QDir::tempPath()).absolutePath() +
                          QString::fromUtf8("/") + portNumber_ +
                          QString::fromUtf8("-rsession");
#endif
      localPeer_ = localPeer.toUtf8().constData();
      core::system::setenv("RS_LOCAL_PEER", localPeer_);
   }

   return portNumber_;
}

QString Options::newPortNumber()
{
   portNumber_.clear();
   return portNumber();
}

std::string Options::localPeer() const
{
   return localPeer_;
}


namespace {
QString findFirstMatchingFont(const QStringList& fonts,
                              QString defaultFont,
                              bool fixedWidthOnly)
{
   for (int i = 0; i < fonts.size(); i++)
   {
      QFont font(fonts.at(i));
      if (font.exactMatch())
         if (!fixedWidthOnly || isFixedWidthFont(QFont(fonts.at(i))))
            return fonts.at(i);
   }
   return defaultFont;
}
} // anonymous namespace

QString Options::proportionalFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(QString::fromUtf8("font.proportional")).toString();
   if (!font.isEmpty())
   {
      return font;
   }

   if (!detectedFont.isEmpty())
      return detectedFont;

   QStringList fontList;
#if defined(_WIN32)
   fontList <<
           QString::fromUtf8("Segoe UI") << QString::fromUtf8("Verdana") <<  // Windows
           QString::fromUtf8("Lucida Sans") << QString::fromUtf8("DejaVu Sans") <<  // Linux
           QString::fromUtf8("Lucida Grande") <<          // Mac
           QString::fromUtf8("Helvetica");
#elif defined(__APPLE__)
   fontList <<
           QString::fromUtf8("Lucida Grande") <<          // Mac
           QString::fromUtf8("Lucida Sans") << QString::fromUtf8("DejaVu Sans") <<  // Linux
           QString::fromUtf8("Segoe UI") << QString::fromUtf8("Verdana") <<  // Windows
           QString::fromUtf8("Helvetica");
#else
   fontList <<
           QString::fromUtf8("Ubuntu") << // Ubuntu
           QString::fromUtf8("Lucida Sans") << QString::fromUtf8("DejaVu Sans") <<  // Linux
           QString::fromUtf8("Lucida Grande") <<          // Mac
           QString::fromUtf8("Segoe UI") << QString::fromUtf8("Verdana") <<  // Windows
           QString::fromUtf8("Helvetica");
#endif
   return QString::fromUtf8("\"") +
         findFirstMatchingFont(fontList, QString::fromUtf8("sans-serif"), false) +
         QString::fromUtf8("\"");
}

void Options::setFixedWidthFont(QString font)
{
   if (font.isEmpty())
      settings_.remove(QString::fromUtf8("font.fixedWidth"));
   else
      settings_.setValue(QString::fromUtf8("font.fixedWidth"),
                         font);
}

QString Options::fixedWidthFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(QString::fromUtf8("font.fixedWidth")).toString();
   if (!font.isEmpty())
   {
      return font;
   }

   if (!detectedFont.isEmpty())
      return detectedFont;

   QStringList fontList;
   fontList <<
#if defined(Q_OS_MACX)
           QString::fromUtf8("Monaco")
#elif defined (Q_OS_LINUX)
           QString::fromUtf8("Ubuntu Mono") << QString::fromUtf8("Droid Sans Mono") << QString::fromUtf8("DejaVu Sans Mono") << QString::fromUtf8("Monospace")
#else
           QString::fromUtf8("Lucida Console") << QString::fromUtf8("Consolas") // Windows;
#endif
           ;

   // The fallback font is Courier, not monospace, because QtWebKit doesn't
   // actually provide a monospace font (appears to use Helvetica)

   return detectedFont = QString::fromUtf8("\"") +
         findFirstMatchingFont(fontList, QString::fromUtf8("Courier"), true) +
         QString::fromUtf8("\"");
}


double Options::zoomLevel() const
{
   QVariant zoom = settings_.value(QString::fromUtf8("view.zoomLevel"), 1.0);
   return zoom.toDouble();
}

void Options::setZoomLevel(double zoomLevel)
{
   settings_.setValue(QString::fromUtf8("view.zoomLevel"), zoomLevel);
}



#ifdef _WIN32
QString Options::rBinDir() const
{
   // HACK: If RBinDir doesn't appear at all, that means the user has never
   // specified a preference for R64 vs. 32-bit R. In this situation we should
   // accept either. We'll distinguish between this case (where preferR64
   // should be ignored) and the other case by using null for this case and
   // empty string for the other.
   if (!settings_.contains(QString::fromUtf8("RBinDir")))
      return QString::null;

   QString value = settings_.value(QString::fromUtf8("RBinDir")).toString();
   return value.isNull() ? QString() : value;
}

void Options::setRBinDir(QString path)
{
   settings_.setValue(QString::fromUtf8("RBinDir"), path);
}

bool Options::preferR64() const
{
   if (!core::system::isWin64())
      return false;

   if (!settings_.contains(QString::fromUtf8("PreferR64")))
      return true;
   return settings_.value(QString::fromUtf8("PreferR64")).toBool();
}

void Options::setPreferR64(bool preferR64)
{
   settings_.setValue(QString::fromUtf8("PreferR64"), preferR64);
}
#endif

FilePath Options::scriptsPath() const
{
   return scriptsPath_;
}


void Options::setScriptsPath(const FilePath& scriptsPath)
{
   scriptsPath_ = scriptsPath;
}

FilePath Options::executablePath() const
{
   if (executablePath_.empty())
   {
      Error error = core::system::executablePath(QApplication::arguments().at(0).toUtf8(),
                                                 &executablePath_);
      if (error)
         LOG_ERROR(error);
   }
   return executablePath_;
}

FilePath Options::supportingFilePath() const
{
   if (supportingFilePath_.empty())
   {
      // default to install path
      core::system::installPath("..",
                                QApplication::arguments().at(0).toUtf8(),
                                &supportingFilePath_);

      // adapt for OSX resource bundles
#ifdef __APPLE__
         if (supportingFilePath_.complete("Info.plist").exists())
            supportingFilePath_ = supportingFilePath_.complete("Resources");
#endif
   }
   return supportingFilePath_;
}

FilePath Options::wwwDocsPath() const
{
   FilePath supportingFilePath = desktop::options().supportingFilePath();
   FilePath wwwDocsPath = supportingFilePath.complete("www/docs");
   if (!wwwDocsPath.exists())
      wwwDocsPath = supportingFilePath.complete("../gwt/www/docs");
#ifdef __APPLE__
   if (!wwwDocsPath.exists())
      wwwDocsPath = supportingFilePath.complete("../../../../../gwt/www/docs");
#endif
   return wwwDocsPath;
}

#ifdef _WIN32

FilePath Options::urlopenerPath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.filename() == "desktop")
      parentDir = parentDir.complete("urlopener");

   return parentDir.complete("urlopener.exe");
}

FilePath Options::rsinversePath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.filename() == "desktop")
      parentDir = parentDir.complete("synctex/rsinverse");

   return parentDir.complete("rsinverse.exe");
}

#endif

QStringList Options::ignoredUpdateVersions() const
{
   return settings_.value(QString::fromUtf8("ignoredUpdateVersions"), QStringList()).toStringList();
}

void Options::setIgnoredUpdateVersions(const QStringList& ignoredVersions)
{
   settings_.setValue(QString::fromUtf8("ignoredUpdateVersions"), ignoredVersions);
}

core::FilePath Options::scratchTempDir(core::FilePath defaultPath)
{
   core::FilePath dir(scratchPath.toUtf8().constData());

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

bool Options::webkitDevTools()
{
   return settings_.value(QString::fromUtf8("webkitDevTools"), false).toBool();
}

} // namespace desktop
