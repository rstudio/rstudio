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

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <QFileOpenEvent>

namespace desktop {

bool PosixApplication::event(QEvent* pEvent)
{
   switch(pEvent->type())
   {
   case QEvent::FileOpen:
   {
      openFileRequest_ = static_cast<QFileOpenEvent*>(pEvent)->file();
      return true;
   }

   default:
      return QtSingleApplication::event(pEvent);
   }
}


} // namespace desktop
