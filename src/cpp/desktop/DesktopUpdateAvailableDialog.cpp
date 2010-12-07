/*
 * DesktopUpdateAvailableDialog.cpp
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
   QPushButton* pDownload = ui->buttonBox->addButton(
         "Download...",
         QDialogButtonBox::AcceptRole);
   pDownload->setAutoDefault(false);
   pDownload->setDefault(true);

   QPushButton* pRemindLater = ui->buttonBox->addButton(
         "Remind Later",
         QDialogButtonBox::RejectRole);
   pRemindLater->setAutoDefault(false);

   QPushButton* pIgnoreUpdate = ui->buttonBox->addButton(
         "Ignore Update",
         QDialogButtonBox::DestructiveRole);
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
