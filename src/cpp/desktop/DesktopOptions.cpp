/*
 * DesktopOptions.cpp
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

#include "DesktopOptions.hpp"

#include <QtGui>
#include <QApplication>
#include <QDesktopWidget>

#include <core/Random.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopInfo.hpp"
#include "DesktopUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

#ifdef _WIN32
// Defined in DesktopRVersion.cpp
QString binDirToHomeDir(QString binDir);
#endif

#define kMainWindowGeometry (QStringLiteral("mainwindow/geometry"))
#define kFixedWidthFont     (QStringLiteral("font.fixedWidth"))
#define kProportionalFont   (QStringLiteral("font.proportional"))

QString scratchPath;

Options& options()
{
   static Options singleton;
   return singleton;
}

Options::Options() :
   settings_(FORMAT,
             QSettings::UserScope,
             QString::fromUtf8("RStudio"),
             QString::fromUtf8("desktop")),
   runDiagnostics_(false)
{
#ifndef _WIN32
   // ensure that the options file is only readable by this user
   FilePath optionsFile(settings_.fileName().toStdString());
   if (!optionsFile.exists())
   {
      // file doesn't yet exist - QT can lazily write to the settings file
      // create an empty file so we can set its permissions before it's created by QT
      std::shared_ptr<std::ostream> pOfs;
      Error error = optionsFile.openForWrite(pOfs, false);
      if (error)
         LOG_ERROR(error);
   }

   Error error = optionsFile.changeFileMode(FileMode::USER_READ_WRITE);
   if (error)
      LOG_ERROR(error);
#endif
}

void Options::initFromCommandLine(const QStringList& arguments)
{
   for (int i=1; i<arguments.size(); i++)
   {
      const QString &arg = arguments.at(i);
      if (arg == QString::fromUtf8(kRunDiagnosticsOption))
         runDiagnostics_ = true;
   }

   // synchronize zoom level with desktop frame
   desktopInfo().setZoomLevel(zoomLevel());
}

void Options::restoreMainWindowBounds(QMainWindow* win)
{
   // NOTE: on macOS, if the display configuration has changed, the attempt to
   // restore window geometry can fail and the use can be left with a tiny
   // RStudio window.
   //
   // to avoid this, we always first attempt to resize to a 'good' size, and
   // then restore a saved window geometry (which may just silently fail if the
   // display configuration has indeed changed)
   //
   // https://github.com/rstudio/rstudio/issues/3498
   // https://github.com/rstudio/rstudio/issues/3159
   //
   
   QSize size = QSize(1200, 900).boundedTo(
            QApplication::primaryScreen()->availableGeometry().size());
   if (size.width() > 800 && size.height() > 500)
   {
      // Only use default size if it seems sane; otherwise let Qt set it
      win->resize(size);
   }

   if (settings_.contains(kMainWindowGeometry))
   {
      // try to restore the geometry
      win->restoreGeometry(settings_.value(kMainWindowGeometry).toByteArray());

      // double-check that we haven't accidentally restored a geometry that
      // places the Window off-screen (can happen if the screen configuration
      // changes between the time geometry was saved and loaded)
      QRect desktopRect = QApplication::primaryScreen()->availableGeometry();
      QRect winRect = win->geometry();
      
      // shrink the window rectangle a bit just to capture cases like RStudio
      // too close to edge of window and hardly showing at all
      QRect checkRect(
               winRect.topLeft() + QPoint(5, 5),
               winRect.bottomRight() - QPoint(5, 5));
      
      // check for intersection
      if (!desktopRect.intersects(checkRect))
      {
         // restore size and center the window
         win->resize(size);
         win->move(
                  desktopRect.width() / 2 - size.width() / 2,
                  desktopRect.height() / 2 - size.height() / 2);
      }
   }
   
   // ensure a minimum width, height for the window on restore
   win->resize(
            std::max(300, win->width()),
            std::max(200, win->height()));
      
}

void Options::saveMainWindowBounds(QMainWindow* win)
{
   settings_.setValue(kMainWindowGeometry, win->saveGeometry());
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
      localPeer_ = localPeer.toUtf8().constData();
      core::system::setenv("RS_LOCAL_PEER", localPeer_);
#endif
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

QString Options::desktopRenderingEngine() const
{
   return settings_.value(QStringLiteral("desktop.renderingEngine")).toString();
}

void Options::setDesktopRenderingEngine(QString engine)
{
   settings_.setValue(QStringLiteral("desktop.renderingEngine"), engine);
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

void Options::setFont(QString key, QString font)
{
   if (font.isEmpty())
      settings_.remove(key);
   else
      settings_.setValue(key, font);
}

void Options::setProportionalFont(QString font)
{
   setFont(kProportionalFont, font);
}

QString Options::proportionalFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(kProportionalFont).toString();
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
           QString::fromUtf8("Lucida Sans") << QString::fromUtf8("DejaVu Sans") <<  // Linux
           QString::fromUtf8("Lucida Grande") <<          // Mac
           QString::fromUtf8("Segoe UI") << QString::fromUtf8("Verdana") <<  // Windows
           QString::fromUtf8("Helvetica");
#endif

   QString sansSerif = QStringLiteral("sans-serif");
   QString selectedFont = findFirstMatchingFont(fontList, sansSerif, false);

   // NOTE: browsers will refuse to render a default font if the name is in
   // quotes; e.g. "sans-serif" is a signal to look for a font called sans-serif
   // rather than use the default sans-serif font!
   if (selectedFont == sansSerif)
      return sansSerif;
   else
      return QStringLiteral("\"%1\"").arg(selectedFont);
}

void Options::setFixedWidthFont(QString font)
{
   setFont(kFixedWidthFont, font);
}

QString Options::fixedWidthFont() const
{
   static QString detectedFont;

   QString font =
         settings_.value(kFixedWidthFont).toString();
   if (!font.isEmpty())
   {
      return QString::fromUtf8("\"") + font + QString::fromUtf8("\"");
   }

   if (!detectedFont.isEmpty())
      return detectedFont;

   QStringList fontList;
   fontList <<
#if defined(Q_OS_MAC)
           QString::fromUtf8("Monaco")
#elif defined (Q_OS_LINUX)
           QString::fromUtf8("Ubuntu Mono") << QString::fromUtf8("Droid Sans Mono") << QString::fromUtf8("DejaVu Sans Mono") << QString::fromUtf8("Monospace")
#else
           QString::fromUtf8("Lucida Console") << QString::fromUtf8("Consolas") // Windows;
#endif
           ;

   // NOTE: browsers will refuse to render a default font if the name is in
   // quotes; e.g. "monospace" is a signal to look for a font called monospace
   // rather than use the default monospace font!
   QString monospace = QStringLiteral("monospace");
   QString matchingFont = findFirstMatchingFont(fontList, monospace, true);
   if (matchingFont == monospace)
      return monospace;
   else
      return QStringLiteral("\"%1\"").arg(matchingFont);
}


double Options::zoomLevel() const
{
   QVariant zoom = settings_.value(QString::fromUtf8("view.zoomLevel"), 1.0);
   return zoom.toDouble();
}

void Options::setZoomLevel(double zoomLevel)
{
   desktopInfo().setZoomLevel(zoomLevel);
   settings_.setValue(QString::fromUtf8("view.zoomLevel"), zoomLevel);
}

bool Options::enableAccessibility() const
{
   QVariant accessibility = settings_.value(QString::fromUtf8("view.accessibility"), false);
   return accessibility.toBool();
}

void Options::setEnableAccessibility(bool enable)
{
   settings_.setValue(QString::fromUtf8("view.accessibility"), enable);
}

bool Options::clipboardMonitoring() const
{
   QVariant monitoring = settings_.value(QString::fromUtf8("clipboard.monitoring"), true);
   return monitoring.toBool();
}

void Options::setClipboardMonitoring(bool monitoring)
{
   settings_.setValue(QString::fromUtf8("clipboard.monitoring"), monitoring);
}

bool Options::ignoreGpuBlacklist() const
{
   QVariant ignore = settings_.value(QStringLiteral("general.ignoreGpuBlacklist"), false);
   return ignore.toBool();
}

void Options::setIgnoreGpuBlacklist(bool ignore)
{
   settings_.setValue(QStringLiteral("general.ignoreGpuBlacklist"), ignore);
}

bool Options::disableGpuDriverBugWorkarounds() const
{
   QVariant disable = settings_.value(QStringLiteral("general.disableGpuDriverBugWorkarounds"), false);
   return disable.toBool();
}

void Options::setDisableGpuDriverBugWorkarounds(bool disable)
{
   settings_.setValue(QStringLiteral("general.disableGpuDriverBugWorkarounds"), disable);
}

bool Options::useFontConfigDatabase() const
{
   QVariant use = settings_.value(QStringLiteral("general.useFontConfigDatabase"), true);
   return use.toBool();
}

void Options::setUseFontConfigDatabase(bool use)
{
   settings_.setValue(QStringLiteral("general.useFontConfigDatabase"), use);
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
      return QString();

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
   if (executablePath_.isEmpty())
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
   if (supportingFilePath_.isEmpty())
   {
      // default to install path
      core::system::installPath("..",
                                QApplication::arguments().at(0).toUtf8(),
                                &supportingFilePath_);

      // adapt for OSX resource bundles
#ifdef __APPLE__
         if (supportingFilePath_.completePath("Info.plist").exists())
            supportingFilePath_ = supportingFilePath_.completePath("Resources");
#endif
   }
   return supportingFilePath_;
}

FilePath Options::resourcesPath() const
{
   if (resourcesPath_.isEmpty())
   {
#ifdef RSTUDIO_PACKAGE_BUILD
      // release configuration: the 'resources' folder is
      // part of the supporting files folder
      resourcesPath_ = supportingFilePath().completePath("resources");
#else
      // developer configuration: the 'resources' folder is
      // a sibling of the RStudio executable
      resourcesPath_ = scriptsPath().completePath("resources");
#endif
   }

   return resourcesPath_;
}

FilePath Options::wwwDocsPath() const
{
   FilePath supportingFilePath = desktop::options().supportingFilePath();
   FilePath wwwDocsPath = supportingFilePath.completePath("www/docs");
   if (!wwwDocsPath.exists())
      wwwDocsPath = supportingFilePath.completePath("../gwt/www/docs");
#ifdef __APPLE__
   if (!wwwDocsPath.exists())
      wwwDocsPath = supportingFilePath.completePath("../../../../../gwt/www/docs");
#endif
   return wwwDocsPath;
}

#ifdef _WIN32

FilePath Options::urlopenerPath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.getFilename() == "desktop")
      parentDir = parentDir.completePath("urlopener");

   return parentDir.completePath("urlopener.exe");
}

FilePath Options::rsinversePath() const
{
   FilePath parentDir = scriptsPath();

   // detect dev configuration
   if (parentDir.getFilename() == "desktop")
      parentDir = parentDir.completePath("synctex/rsinverse");

   return parentDir.completePath("rsinverse.exe");
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

   if (!dir.isEmpty() && dir.exists())
   {
      dir = dir.completeChildPath("tmp");
      core::Error error = dir.ensureDirectory();
      if (!error)
         return dir;
   }
   return defaultPath;
}

void Options::cleanUpScratchTempDir()
{
   core::FilePath temp = scratchTempDir(core::FilePath());
   if (!temp.isEmpty())
      temp.removeIfExists();
}

QString Options::lastRemoteSessionUrl(const QString& serverUrl)
{
   settings_.beginGroup(QString::fromUtf8("remote-sessions-list"));
   QString sessionUrl = settings_.value(serverUrl).toString();
   settings_.endGroup();
   return sessionUrl;
}

void Options::setLastRemoteSessionUrl(const QString& serverUrl, const QString& sessionUrl)
{
   settings_.beginGroup(QString::fromUtf8("remote-sessions-list"));
   settings_.setValue(serverUrl, sessionUrl);
   settings_.endGroup();
}

QList<QNetworkCookie> Options::cookiesFromList(const QStringList& cookieStrs) const
{
   QList<QNetworkCookie> cookies;

   for (const QString& cookieStr : cookieStrs)
   {
      QByteArray cookieArr = QByteArray::fromStdString(cookieStr.toStdString());
      QList<QNetworkCookie> innerCookies = QNetworkCookie::parseCookies(cookieArr);
      for (const QNetworkCookie& cookie : innerCookies)
      {
         cookies.push_back(cookie);
      }
   }

   return cookies;
}

QList<QNetworkCookie> Options::authCookies() const
{
   QStringList cookieStrs = settings_.value(QString::fromUtf8("cookies"), QStringList()).toStringList();
   return cookiesFromList(cookieStrs);
}

QList<QNetworkCookie> Options::tempAuthCookies() const
{
   QStringList cookieStrs = settings_.value(QString::fromUtf8("temp-auth-cookies"), QStringList()).toStringList();
   return cookiesFromList(cookieStrs);
}

QStringList Options::cookiesToList(const QList<QNetworkCookie>& cookies) const
{
   QStringList cookieStrs;
   for (const QNetworkCookie& cookie : cookies)
   {
      cookieStrs.push_back(QString::fromStdString(cookie.toRawForm().toStdString()));
   }

   return cookieStrs;
}

void Options::setAuthCookies(const QList<QNetworkCookie>& cookies)
{
   settings_.setValue(QString::fromUtf8("cookies"), cookiesToList(cookies));
}

void Options::setTempAuthCookies(const QList<QNetworkCookie>& cookies)
{
   settings_.setValue(QString::fromUtf8("temp-auth-cookies"), cookiesToList(cookies));
}

} // namespace desktop
} // namespace rstudio

