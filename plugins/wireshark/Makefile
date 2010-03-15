#
# Copyright 2010 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# Modify to point to your Wireshark and glib include directories
INCS = -I/usr/include/wireshark -I/usr/include/glib-2.0 \
       -I/usr/lib/glib-2.0/include -I../common

CC   = gcc

SRCS = packet-gwtcs.c
OBJS = obj/packet-gwtcs.o

PLUGIN_NAME = packet-gwtcs
PLUGIN_DIR  = $(HOME)/.wireshark/plugins
PLUGIN      = $(PLUGIN_NAME).so

CFLAGS = -DHAVE_CONFIG_H $(INCS) -DINET6 -D_U_=__attribute__\(\(unused\)\) \
	-Wall -Wpointer-arith -g -DXTHREADS -D_REENTRANT -DXUSE_MTSAFE_API \
	-fPIC -DPIC -O2

all::		obj/$(PLUGIN)

install::	obj/$(PLUGIN)
	cp obj/$(PLUGIN) $(PLUGIN_DIR)/$(PLUGIN)

obj/$(PLUGIN) : $(OBJS)
	mkdir -p $(PLUGIN_DIR)
	$(CC) -shared $(OBJS) -o $@

$(OBJS): obj

obj:
	mkdir obj

obj/packet-gwtcs.o : packet-gwtcs.c ../common/BrowserChannel.h
	$(CC) -c $(CFLAGS) $< -o $@

clean:
	rm -rf obj
