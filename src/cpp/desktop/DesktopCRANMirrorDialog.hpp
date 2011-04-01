/*
 * DesktopCRANMirrorDialog.hpp
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

#ifndef DESKTOPCRANMIRRORDIALOG_HPP
#define DESKTOPCRANMIRRORDIALOG_HPP

#include <QDialog>

namespace Ui {
    class CRANMirrorDialog;
}

class CRANMirrorDialog : public QDialog
{
    Q_OBJECT

public:
    explicit CRANMirrorDialog(QWidget *parent = 0);
    ~CRANMirrorDialog();

    QString selectedName();
    QString selectedURL();

 protected slots:
    void loadMirrorCsv(const QByteArray& data);
    void showNetworkError(const QString& errorString);
    void manageButtons();
    void requestTimeout();

private:
    Ui::CRANMirrorDialog *ui;
    QPushButton* pOK_;
    std::vector<std::string> headers_;
};

#endif // DESKTOPCRANMIRRORDIALOG_HPP
