/*
 * DesktopWordViewer.hpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
#ifndef DESKTOPWORDVIEWER_HPP
#define DESKTOPWORDVIEWER_HPP

#include <QString>

#include <boost/utility.hpp>
#include <core/Error.hpp>

class IDispatch;

namespace rstudio {
namespace desktop {

class WordViewer : boost::noncopyable
{
public:
   WordViewer();
   ~WordViewer();
   core::Error showDocument(QString& path);
   core::Error closeLastViewedDocument();

private:
   core::Error openDocument(QString& path, IDispatch* idispDocs,
                            IDispatch** pidispDoc);
   core::Error showWord();
   core::Error getDocumentPosition(IDispatch* idispPos, int* pxPos, int* pyPos);
   core::Error setDocumentPosition(IDispatch* idispPos, int xPos, int yPos);
   core::Error getDocumentByPath(QString& path, IDispatch** pidispDoc);
   IDispatch* idispWord_;
   int docScrollX_;
   int docScrollY_;
   QString docPath_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOPWORDVIEWER_HPP
