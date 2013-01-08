/*
 * DesktopUpdateAvailableDialog.cpp
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

#include "DesktopUpdateAvailableDialog.hpp"
#include "ui_DesktopUpdateAvailableDialog.h"

#include <QPushButton>

DesktopUpdateAvailableDialog::DesktopUpdateAvailableDialog(
                                    const DesktopUpdateInfo& updateInfo,
                                    QWidget *parent) :
      QDialog(parent),
      updateInfo_(updateInfo),
      ui(new Ui::DesktopUpdateAvailableDialog())
{
   ui->setupUi(this);
   ui->lblIcon->setFixedSize(QSize(64, 64));
   ui->label->setText(updateInfo.updateMessage);

   ui->buttonBox->clear();
   QPushButton* pDownload = new QPushButton(QString::fromUtf8("Download..."));
   ui->buttonBox->addButton(pDownload, QDialogButtonBox::AcceptRole);
   pDownload->setAutoDefault(false);
   pDownload->setDefault(true);

   QPushButton* pRemindLater = new QPushButton(QString::fromUtf8("Remind Later"));
   ui->buttonBox->addButton(pRemindLater, QDialogButtonBox::RejectRole);
   pRemindLater->setAutoDefault(false);

   QPushButton* pIgnoreUpdate = new QPushButton(QString::fromUtf8("Ignore Update"));
   ui->buttonBox->addButton(pIgnoreUpdate, QDialogButtonBox::DestructiveRole);
   pIgnoreUpdate->setAutoDefault(false);
   pIgnoreUpdate->setEnabled(!updateInfo.isUrgent);
   connect(pIgnoreUpdate, SIGNAL(clicked()),
           this, SLOT(permanentlyIgnoreUpdate()));

   pRemindLater->setFocus();
}

void DesktopUpdateAvailableDialog::permanentlyIgnoreUpdate()
{
   done(2);
}

DesktopUpdateAvailableDialog::~DesktopUpdateAvailableDialog()
{
    delete ui;
}
