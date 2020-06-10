/*
 * DesktopOptions.hpp
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

#ifndef DESKTOP_OPTIONS_HPP
#define DESKTOP_OPTIONS_HPP

#include <boost/noncopyable.hpp>

#include <QDir>
#include <QMainWindow>
#include <QNetworkCookie>
#include <QSettings>
#include <QStringList>

#include <shared_core/FilePath.hpp>

#define kRunDiagnosticsOption    "--run-diagnostics"
#define kSessionServerOption     "--session-server"
#define kSessionServerUrlOption  "--session-url"
#define kTempCookiesOption       "--use-temp-cookies"

#if defined(__APPLE__)
#define FORMAT QSettings::NativeFormat
#else
#define FORMAT QSettings::IniFormat
#endif

namespace rstudio {
namespace desktop {

class Options;
Options& options();

class Options : boost::noncopyable
{
public:
   void initFromCommandLine(const QStringList& arguments);

   void restoreMainWindowBounds(QMainWindow* window);
   void saveMainWindowBounds(QMainWindow* window);
   QString portNumber() const;
   QString newPortNumber();
   std::string localPeer() const; // derived from portNumber
   
   QString desktopRenderingEngine() const;
   void setDesktopRenderingEngine(QString engine);

   QString proportionalFont() const;
   void setProportionalFont(QString font);

   QString fixedWidthFont() const;
   void setFixedWidthFont(QString font);

   double zoomLevel() const;
   void setZoomLevel(double zoomLevel);

   bool enableAccessibility() const;
   void setEnableAccessibility(bool enable);

   bool clipboardMonitoring() const;
   void setClipboardMonitoring(bool monitoring);
   
   bool ignoreGpuBlacklist() const;
   void setIgnoreGpuBlacklist(bool ignore);
   
   bool disableGpuDriverBugWorkarounds() const;
   void setDisableGpuDriverBugWorkarounds(bool disable);

   bool useFontConfigDatabase() const;
   void setUseFontConfigDatabase(bool use);

#ifdef _WIN32
   // If "", then use automatic detection
   QString rBinDir() const;
   void setRBinDir(QString path);

   bool preferR64() const;
   void setPreferR64(bool preferR64);
#endif

   // Resolves to 'desktop' sub-directory in development builds.
   // Resolves to 'bin' directory in release builds.
   core::FilePath scriptsPath() const;
   void setScriptsPath(const core::FilePath& scriptsPath);

   core::FilePath executablePath() const;

   // Resolves to 'root' install directory in both development
   // and release builds. On macOS, points to 'Resources' directory.
   core::FilePath supportingFilePath() const;

   // Resolves to 'desktop/resources' sub-directory in development builds.
   // Resolves to 'resources' sub-directory in release builds.
   core::FilePath resourcesPath() const;

   core::FilePath wwwDocsPath() const;

#ifdef _WIN32
   core::FilePath urlopenerPath() const;
   core::FilePath rsinversePath() const;
#endif

   QStringList ignoredUpdateVersions() const;
   void setIgnoredUpdateVersions(const QStringList& ignoredVersions);

   core::FilePath scratchTempDir(core::FilePath defaultPath=core::FilePath());
   void cleanUpScratchTempDir();

   bool runDiagnostics() { return runDiagnostics_; }

   QString lastRemoteSessionUrl(const QString& serverUrl);
   void setLastRemoteSessionUrl(const QString& serverUrl, const QString& sessionUrl);

   QList<QNetworkCookie> authCookies() const;
   QList<QNetworkCookie> tempAuthCookies() const;
   void setAuthCookies(const QList<QNetworkCookie>& cookies);
   void setTempAuthCookies(const QList<QNetworkCookie>& cookies);

private:
   Options();
   friend Options& options();

   void setFont(QString key, QString font);
   QStringList cookiesToList(const QList<QNetworkCookie>& cookies) const;
   QList<QNetworkCookie> cookiesFromList(const QStringList& cookieStrs) const;

   QSettings settings_;
   core::FilePath scriptsPath_;
   mutable core::FilePath executablePath_;
   mutable core::FilePath supportingFilePath_;
   mutable core::FilePath resourcesPath_;
   mutable QString portNumber_;
   mutable std::string localPeer_;
   bool runDiagnostics_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_OPTIONS_HPP
