/*
 * DesktopSynctex.cpp
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

#include "DesktopSynctex.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <core/Error.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/Process.hpp>

#include "DesktopUtils.hpp"

// per-platform synctex implemetnations
#if defined(Q_OS_DARWIN)

#elif defined(Q_OS_WIN)

#elif defined(Q_OS_LINUX)
#include "synctex/evince/EvinceSynctex.hpp"
#endif

using namespace core;

namespace desktop {

namespace {

struct SynctexViewer
{
   SynctexViewer()
      : versionMajor(0), versionMinor(0), versionPatch(0)
   {
   }

   QString name;

   bool empty() const { return name.isEmpty(); }

   // NOTE: use QT_VERSION_CHECK macro for comparisons
   int versionMajor;
   int versionMinor;
   int versionPatch;
};

SynctexViewer s_viewer;


#if defined(Q_OS_WIN)

SynctexViewer discoverViewer()
{
   return SynctexViewer();
}

#elif defined(Q_OS_LINUX)

SynctexViewer discoverViewer()
{
   // probe for evince version
   core::system::ProcessResult result;
   Error error = core::system::runCommand("evince --version",
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
   {
      LOG_ERROR(error);
      return SynctexViewer();
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      return SynctexViewer();
   }

   // default to version 2.0.0 (which is pre-synctex)
   SynctexViewer viewer;
   viewer.name = QString::fromAscii("Evince");
   viewer.versionMajor = 2;
   viewer.versionMinor = 0;
   viewer.versionPatch = 0;

   // trim output
   std::string stdOut = boost::algorithm::trim_copy(result.stdOut);

   // extract version
   boost::smatch match;
   boost::regex re("^.*(\\d+)\\.(\\d+)\\.(\\d)+$");
   if (boost::regex_match(stdOut, match, re))
   {
      viewer.versionMajor = safe_convert::stringTo<int>(match[1],
                                                        viewer.versionMajor);
      viewer.versionMinor = safe_convert::stringTo<int>(match[2],
                                                        viewer.versionMinor);
      viewer.versionPatch = safe_convert::stringTo<int>(match[3],
                                                        viewer.versionMajor);
   }

   return viewer;
}

#else

SynctexViewer discoverViewer()
{
   return SynctexViewer();
}

#endif




} // anonymous namespace

QString Synctex::desktopViewerName()
{
   if (s_viewer.empty())
      s_viewer = discoverViewer();

   return s_viewer.name;
}

Synctex* Synctex::create(MainWindow* pMainWindow)
{
   // per-platform synctex implemetnations
#if defined(Q_OS_DARWIN)
   return new Synctex(pMainWindow);
#elif defined(Q_OS_WIN)
   return new Synctex(pMainWindow);
#elif defined(Q_OS_LINUX)
   return new synctex::EvinceSynctex(pMainWindow);
#else
   return new Synctex(pMainWindow);
#endif
}

void Synctex::onClosed(const QString& pdfFile)
{
   pMainWindow_->onPdfViewerClosed(pdfFile);
}

void Synctex::onSyncSource(const QString &srcFile, const QPoint &srcLoc)
{
   desktop::raiseAndActivateWindow(pMainWindow_);

   pMainWindow_->onPdfViewerSyncSource(srcFile, srcLoc.x(), srcLoc.y());
}

} // namespace desktop
