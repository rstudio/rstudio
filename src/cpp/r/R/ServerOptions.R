#
# ServerOptions.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# set save defaults for high-performance (only if the administrator
# or user hasn't set them explicitly already)

if (is.null(getOption("save.defaults")))
   options(save.defaults=list(ascii=FALSE, compress=FALSE))

if (is.null(getOption("save.image.defaults")))
   options(save.image.defaults=list(ascii=FALSE, safe=TRUE, compress=FALSE))

# no support for email
options(mailer = "none")

# use internal unzip
options(unzip = "internal")

