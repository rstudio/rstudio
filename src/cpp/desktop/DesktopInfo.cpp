/*
 * DesktopInfo.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "DesktopInfo.hpp"

#include <core/system/Process.hpp>
#include <core/FileSerializer.hpp>

#include "DesktopOptions.hpp"
#include "DesktopSynctex.hpp"

#define kLsbRelease    "/etc/lsb-release"
#define kRedhatRelease "/etc/redhat-release"
#define kOsRelease     "/etc/os-release"

#define kUnknown QStringLiteral("unknown")

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {

QString s_platform          = kUnknown;
QString s_version           = kUnknown;
QString s_sumatraPdfExePath = kUnknown;

#ifdef Q_OS_LINUX

void readEntry(
      const std::map<std::string, std::string>& entries,
      const char* key,
      QString* pOutput)
{
   if (entries.count(key))
   {
      *pOutput = QString::fromStdString(entries.at(key)).toLower();
   }
}

void initializeLsbRelease()
{
   std::map<std::string, std::string> entries;
   Error error = core::readStringMapFromFile(FilePath(kLsbRelease), &entries);

   if (error)
      LOG_ERROR(error);

   readEntry(entries, "DISTRIB_ID", &s_platform);
   readEntry(entries, "DISTRIB_RELEASE", &s_version);
}

void initializeRedhatRelease()
{
   std::string contents;
   Error error = core::readStringFromFile(
            FilePath(kRedhatRelease),
            &contents);
   if (error)
      LOG_ERROR(error);

   if (contents.find("CentOS") != std::string::npos)
      s_platform = QStringLiteral("centos");
   else if (contents.find("Red Hat Enterprise Linux"))
      s_platform = QStringLiteral("rhel");
}

void initializeOsRelease()
{
   std::map<std::string, std::string> entries;
   Error error = core::readStringMapFromFile(
            FilePath(kOsRelease),
            &entries);

   if (error)
      LOG_ERROR(error);

   readEntry(entries, "ID", &s_platform);
   readEntry(entries, "VERSION_ID", &s_version);
}

void initialize()
{
   if (FilePath(kLsbRelease).exists())
      initializeLsbRelease();

   if (FilePath(kRedhatRelease).exists())
      initializeRedhatRelease();

   if (FilePath(kOsRelease).exists())
      initializeOsRelease();
}

#endif // Q_OS_LINUX

#ifdef Q_OS_WIN32

void initialize()
{
}

#endif

#ifdef Q_OS_MAC

void initialize()
{
}

#endif

} // end anonymous namespace

DesktopInfo::DesktopInfo(QObject* parent)
   : QObject(parent)
{
   initialize();

   QObject::connect(this, &DesktopInfo::sumatraPdfExePathChanged, &DesktopInfo::setSumatraPdfExePath);
}

QString DesktopInfo::getPlatform()
{
   return s_platform;
}

QString DesktopInfo::getVersion()
{
   return s_version;
}

QString DesktopInfo::getScrollingCompensationType()
{
#if defined(Q_OS_WIN32)
   return QStringLiteral("Win");
#elif defined(Q_OS_MAC)
   return QStringLiteral("Mac");
#else
   return QStringLiteral("None");
#endif
}

QString DesktopInfo::getFixedWidthFontList()
{
   // TODO: read from QFont database
   return QStringLiteral("None");
}

QString DesktopInfo::getFixedWidthFont()
{
   return options().fixedWidthFont();
}

QString DesktopInfo::getProportionalFont()
{
   return options().proportionalFont();
}

QString DesktopInfo::getDesktopSynctexViewer()
{
   // TODO: this isn't really constant
   return Synctex::desktopViewerInfo().name;
}

bool DesktopInfo::desktopHooksAvailable()
{
   return true;
}

QString DesktopInfo::getSumatraPdfExePath()
{
   return s_sumatraPdfExePath;
}

void DesktopInfo::setSumatraPdfExePath(QString path)
{
   s_sumatraPdfExePath = path;
}

} // end namespace desktop
} // end namespace rstudio
