/*
 * AskPassMain.cpp
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

#include <QtGui>
#include <iostream>
#include <boost/scoped_ptr.hpp>

int main(int argc, char** argv)
{
   boost::scoped_ptr<QApplication> app(new QApplication(argc, argv, true));

   bool ok;
   QString passphrase = QInputDialog::getText(
         NULL,
         QString::fromAscii("Passphrase"),
         QString::fromAscii("Please enter your passphrase"),
         QLineEdit::Password,
         QString(),
         &ok);

   if (ok)
      std::cout << passphrase.toLocal8Bit().data();

   return ok ? EXIT_SUCCESS : EXIT_FAILURE;
}
