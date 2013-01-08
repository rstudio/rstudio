/*
 * DesktopChooseRHome.hpp
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
#ifndef DESKTOPCHOOSERHOME_HPP
#define DESKTOPCHOOSERHOME_HPP

#include <QDialog>
#include <QStringListModel>
#include <QCloseEvent>

#include "DesktopRVersion.hpp"
#include "DesktopOptions.hpp"

namespace Ui {
   class ChooseRHome;
}

class ChooseRHome : public QDialog
{
   Q_OBJECT

public:
   explicit ChooseRHome(QList<desktop::RVersion> list, QWidget *parent = 0);
   ~ChooseRHome();

   // "" means auto-detect
   desktop::RVersion value();
   bool preferR64();
   void setValue(const desktop::RVersion& value, bool preferR64);

protected slots:
   void chooseOther();
   void validateSelection();
   void onModeChanged();

protected:
   void done(int r);

private:
   Ui::ChooseRHome *ui;
   QPushButton* pOK_;
   QString lastDir_;
};

#endif // DESKTOPCHOOSERHOME_HPP
