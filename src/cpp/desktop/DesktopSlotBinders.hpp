/*
 * DesktopSlotBinders.hpp
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

#ifndef DESKTOP_STRING_SLOT_BINDER_HPP
#define DESKTOP_STRING_SLOT_BINDER_HPP

#include <QObject>
#include <QWidget>

#include <boost/function.hpp>

namespace rstudio {
namespace desktop {

class StringSlotBinder : public QObject
{
   Q_OBJECT
public:
   explicit StringSlotBinder(QString arg,
                             QObject *parent = nullptr);

Q_SIGNALS:
   void triggered(QString arg);

public Q_SLOTS:
   void trigger();

private:
   QString arg_;
};

// Makes an arbitrary function available as a slot
class FunctionSlotBinder : public QObject
{
   Q_OBJECT
public:
   explicit FunctionSlotBinder(boost::function<void()> func,
                               QObject* parent = nullptr) :
      QObject(parent),
      func_(func)
   {
   }

public Q_SLOTS:
   void execute()
   {
      func_();
   }

private:
   boost::function<void()> func_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_STRING_SLOT_BINDER_HPP
