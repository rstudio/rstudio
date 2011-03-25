/*
 * DesktopPosixApplication.cpp
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

#include "DesktopPosixApplication.hpp"


#include <QFileOpenEvent>

namespace desktop {

bool PosixApplication::event(QEvent* pEvent)
{
   switch(pEvent->type())
   {
   case QEvent::FileOpen:
   {
      // get filename
      QString filename = static_cast<QFileOpenEvent*>(pEvent)->file();

      if (activationWindow() == NULL)
      {
         // if we don't yet have an activation window then this is a startup
         // request -- save it so DesktopMain can pull it out later
         openFileRequest_ = filename;
      }
      else
      {
         // otherwise we are already running so this is an apple event
         // targeted at opening a file in an existing instance
         QtSingleApplication::sendMessage(filename);
      }

      return true;
   }

   default:
      return QtSingleApplication::event(pEvent);
   }
}


} // namespace desktop
