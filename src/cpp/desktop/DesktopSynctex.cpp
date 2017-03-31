/*
 * DesktopSynctex.cpp
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

#include "DesktopSynctex.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <core/Error.hpp>
#include <core/RegexUtils.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/Process.hpp>

#include "DesktopUtils.hpp"

// per-platform synctex implemetnations
#if defined(Q_OS_MAC)

#elif defined(Q_OS_WIN)
#include "synctex/sumatra/SumatraSynctex.hpp"
#elif defined(Q_OS_LINUX)
#include "synctex/evince/EvinceSynctex.hpp"
#endif

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {

#if defined(Q_OS_WIN)

SynctexViewerInfo discoverViewer()
{
  SynctexViewerInfo sv;
  sv.name = QString::fromUtf8("Sumatra");
  sv.versionMajor = 2;
  sv.versionMinor = 0;
  sv.versionPatch = 1;
  return sv;
}

#elif defined(Q_OS_LINUX)

SynctexViewerInfo discoverViewer()
{
   // probe for evince version
   core::system::ProcessResult result;
   Error error = core::system::runCommand("evince --version",
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
   {
      LOG_ERROR(error);
      return SynctexViewerInfo();
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      return SynctexViewerInfo();
   }

   // trim output
   std::string stdOut = boost::algorithm::trim_copy(result.stdOut);

   // extract version
   boost::smatch match;
   boost::regex re("^.*(\\d+)\\.(\\d+)\\.(\\d)+$");
   if (regex_utils::match(stdOut, match, re))
   {
      SynctexViewerInfo sv;
      sv.name = QString::fromUtf8("Evince");
      sv.versionMajor = safe_convert::stringTo<int>(match[1], 2);
      sv.versionMinor = safe_convert::stringTo<int>(match[2], 0);
      sv.versionPatch = safe_convert::stringTo<int>(match[3], 0);

      // the synctex dBus interface changed to include a timestamp parameter
      // in evince 2.91.3 -- we therefore require this version or greater
      // in order to work with evince
      if (sv.version() >= QT_VERSION_CHECK(2, 91, 3))
      {
         return sv;
      }
      else
      {
         return SynctexViewerInfo();
      }
   }
   else
   {
      return SynctexViewerInfo();
   }
}

#else

SynctexViewerInfo discoverViewer()
{
   return SynctexViewerInfo();
}

#endif

// static cache for discoverd desktop viewer
SynctexViewerInfo s_viewer;

} // anonymous namespace

SynctexViewerInfo Synctex::desktopViewerInfo()
{
   if (s_viewer.empty())
      s_viewer = discoverViewer();

   return s_viewer;
}

Synctex* Synctex::create(MainWindow* pMainWindow)
{
   // per-platform synctex implemetnations
#if defined(Q_OS_MAC)
   return new Synctex(pMainWindow);
#elif defined(Q_OS_WIN)
   return new synctex::SumatraSynctex(pMainWindow);
#elif defined(Q_OS_LINUX)
   return new synctex::EvinceSynctex(desktopViewerInfo(), pMainWindow);
#else
   return new Synctex(pMainWindow);
#endif
}

void Synctex::onClosed(const QString& pdfFile)
{
   // don't notify the main window that the viewer closed -- this mechanism
   // was originallly designed for the RStudio Viewer because after it closes
   // there is no way for synctex to work. With desktop viewers there are
   // two other considerations:
   //
   //  (1) there might be multiple viewers active and we don't want to
   //      disable the command for the other viewers
   //
   //  (2) they can actually bring back the pdf viewer to do the sync
   //      without triggering a compile
   //
   // if we want to take this all they way we could:
   //
   //   - Enable/disable the forward search on a per-editor basis (note
   //     below that we pass the PDF file to the onClosed handler so
   //     we have the context to do this
   //
   //   - Allow the RStudio Viewer to be opened without kicking off a compile
   //

   /*
   pMainWindow_->onPdfViewerClosed(pdfFile);
   */
}

void Synctex::onSyncSource(const QString &srcFile, const QPoint &srcLoc)
{
   desktop::raiseAndActivateWindow(pMainWindow_);

   pMainWindow_->onPdfViewerSyncSource(srcFile, srcLoc.x(), srcLoc.y());
}

} // namespace desktop
} // namespace rstudio
