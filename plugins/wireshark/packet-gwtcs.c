/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * Note that Wireshark dissectors are pure C, not C++ -- there are also
 * restrictions on various C extensions, including things like all variables
 * must be declared at the beginning of a block, and only initialized to
 * scalar constants.
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "BrowserChannel.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <gmodule.h>
#include <epan/conversation.h>
#include <epan/prefs.h>
#include <epan/packet.h>

/*
 * Default port to follow.
 */
#define DEFAULT_GWTCS_PORT 9997

/*
 * Trim strngs at this length.
 */
#define MAX_STRING_LENGTH 100

/* forward reference */
void proto_register_gwtcs();
void proto_reg_handoff_gwtcs();

/* Define version if we are not building Wireshark statically */
#ifndef ENABLE_STATIC
G_MODULE_EXPORT const gchar version[] = "0.1";
#endif

/*
 * Data stored for a GWT-CS conversation.
 */
typedef struct _gwtcs_data {
  address       clientAddress;
  guint32       clientPort;
  int           level;
} gwtcs_data;

/*
 * Names of packet types -- must be in order.
 */
static const value_string packetTypes[] = {
  { MESSAGE_TYPE_INVOKE,                  "Invoke" },
  { MESSAGE_TYPE_RETURN,                  "Return" },
  { MESSAGE_TYPE_OLD_LOAD_MODULE,         "Old Load Module" },
  { MESSAGE_TYPE_QUIT,                    "Quit" },
  { MESSAGE_TYPE_LOADJSNI,                "Load JSNI" },
  { MESSAGE_TYPE_INVOKESPECIAL,           "Invoke Special" },
  { MESSAGE_TYPE_FREEVALUE,               "Free Value" },
  { MESSAGE_TYPE_FATAL_ERROR,             "Fatal Error" },
  { MESSAGE_TYPE_CHECK_VERSIONS,          "Check Versions" },
  { MESSAGE_TYPE_PROTOCOL_VERSION,        "Protocol Version" },
  { MESSAGE_TYPE_CHOOSE_TRANSPORT,        "Choose Transport" },
  { MESSAGE_TYPE_SWITCH_TRANSPORT,        "Switch Transport" },
  { MESSAGE_TYPE_LOAD_MODULE,             "Load Module" },
  { 0,  NULL }
};
#define MAX_PACKET_TYPE MESSAGE_TYPE_LOAD_MODULE


static const value_string valueTypes[] = {
  {VALUE_TYPE_NULL,        "null" },
  {VALUE_TYPE_BOOLEAN,     "boolean" },
  {VALUE_TYPE_BYTE,        "byte" },
  {VALUE_TYPE_CHAR,        "char" },
  {VALUE_TYPE_SHORT,       "short" },
  {VALUE_TYPE_INT,         "int" },
  {VALUE_TYPE_LONG,        "long" },
  {VALUE_TYPE_FLOAT,       "float" },
  {VALUE_TYPE_DOUBLE,      "double" },
  {VALUE_TYPE_STRING,      "string" },
  {VALUE_TYPE_JAVA_OBJECT, "Java object" },
  {VALUE_TYPE_JS_OBJECT,   "JS object" },
  {VALUE_TYPE_UNDEFINED,   "undefined" },
  { 0,  NULL }
};

/*
 * InvokeSpecial types -- must be in order.
 */
static const value_string specialTypes[] = {
  { SPECIAL_HAS_METHOD,   "hasMethod" },
  { SPECIAL_HAS_PROPERTY, "hasProperty" },
  { SPECIAL_GET_PROPERTY, "getProperty" },
  { SPECIAL_SET_PROPERTY, "setProperty" },
  { 0, NULL }
};

/*
 * Dynamically assigned protocol ID.
 */
static int proto_gwtcs = -1;

/*
 * Dynamically assigned subtree IDs.
 */
static gint ett_gwtcs = -1;
static gint ett_value = -1;
static gint ett_args = -1;

/*
 * IDs for displayed values.
 */
static int hf_gwtcs_pdu_type = -1;
static int hf_gwtcs_value_tag = -1;
static int hf_gwtcs_min_vers = -1;
static int hf_gwtcs_max_vers = -1;
static int hf_gwtcs_hosted_vers = -1;
static int hf_gwtcs_sel_vers = -1;
static int hf_gwtcs_lm_ua = -1;
static int hf_gwtcs_lm_tabkey = -1;
static int hf_gwtcs_lm_seskey = -1;
static int hf_gwtcs_lm_modname = -1;
static int hf_gwtcs_lm_url = -1;
static int hf_gwtcs_methname = -1;
static int hf_gwtcs_isexc = -1;
static int hf_gwtcs_dispid = -1;
static int hf_gwtcs_jsni = -1;
static int hf_gwtcs_val_hdr = -1;
static int hf_gwtcs_val_bool = -1;
static int hf_gwtcs_val_byte = -1;
static int hf_gwtcs_val_char = -1;
static int hf_gwtcs_val_short = -1;
static int hf_gwtcs_val_int = -1;
static int hf_gwtcs_val_long = -1;
static int hf_gwtcs_val_float = -1;
static int hf_gwtcs_val_double = -1;
static int hf_gwtcs_val_string = -1;
static int hf_gwtcs_val_javaobj = -1;
static int hf_gwtcs_val_jsobj = -1;
static int hf_gwtcs_val_null = -1;
static int hf_gwtcs_val_undef = -1;
static int hf_gwtcs_numargs = -1;
static int hf_gwtcs_spectype = -1;
static int hf_gwtcs_transport = -1;
static int hf_gwtcs_transargs = -1;

static dissector_handle_t gwtcs_handle;

static GMemChunk* memChunk = 0;

#ifndef ENABLE_STATIC
G_MODULE_EXPORT void plugin_register(void)
{
   /* register the new protocol, protocol fields, and subtrees */
   if (proto_gwtcs == -1) { /* execute protocol initialization only once */
      proto_register_gwtcs();
   }
}

G_MODULE_EXPORT void plugin_reg_handoff(void){
   proto_reg_handoff_gwtcs();
}
#endif

/*
 * Get a string describing a Value from the packet, and return the total length
 * (including the tag byte).
 *
 * ofs - offset into the buffer of the Value's tag byte
 * buf - buffer to write string into; on return is guaranteed to be null
 *     terminated
 * buflen - length of buf
 *
 * returns the offset after the last byte of this Value
 */
static int getValue(tvbuff_t* tvb, int ofs, char* buf, int buflen) {
  guint8 tag;
  tag = tvb_get_guint8(tvb, ofs++);
  int len = 0;
  switch (tag) {
    case VALUE_TYPE_NULL:
      strncpy(buf, "null", buflen);
      break;
    case VALUE_TYPE_UNDEFINED:
      strncpy(buf, "undef", buflen);
      break;
    case VALUE_TYPE_BOOLEAN:
      {
        guint8 val;
        val = tvb_get_guint8(tvb, ofs);
        len = 1;
        strncpy(buf, val ? "true" : "false", buflen);
      }
      break;
    case VALUE_TYPE_BYTE:
      {
        int val;
        val = tvb_get_guint8(tvb, ofs);
        if (val & 128) {
          val -= 256;
        }
        len = 1;
        snprintf(buf, buflen, "%d", val);
      }
      break;
    case VALUE_TYPE_SHORT:
      {
        int val;
        val = tvb_get_ntohs(tvb, ofs);
        if (val & 0x8000) {
          val -= 0x10000;
        }
        len = 2;
        snprintf(buf, buflen, "%d", val);
      }
      break;
    case VALUE_TYPE_CHAR:
      {
        int val;
        val = tvb_get_ntohs(tvb, ofs);
        len = 2;
        /* show printable ASCII */
        if (val >= 0x20 && val < 0x7f) {
          snprintf(buf, buflen, "%d - %c", val, val);
        } else {
          snprintf(buf, buflen, "%d (U+%04x)", val, val);
        }
      }
      break;
    case VALUE_TYPE_INT:
      {
        int val;
        val = tvb_get_ntohl(tvb, ofs);
        len = 4;
        snprintf(buf, buflen, "%d", val);
      }
      break;
    case VALUE_TYPE_FLOAT:
      {
        float val;
        val = tvb_get_ntohieee_float(tvb, ofs);
        len = 4;
        snprintf(buf, buflen, "%g", val);
      }
      break;
    case VALUE_TYPE_JAVA_OBJECT:
      {
        int val;
        val = tvb_get_ntohl(tvb, ofs);
        len = 4;
        snprintf(buf, buflen, "Java Object %d", val);
      }
      break;
    case VALUE_TYPE_JS_OBJECT:
      {
        int val;
        val = tvb_get_ntohl(tvb, ofs);
        len = 4;
        snprintf(buf, buflen, "JS Object %d", val);
      }
      break;
    case VALUE_TYPE_LONG:
      {
        guint64 val;
        val = tvb_get_ntoh64(tvb, ofs);
        len = 8;
        /* no portable way to print guint64, so do it in two pieces */
        snprintf(buf, buflen, "0x%08x%08x", (int) ((val >> 32) & 0xFFFFFFFF),
            (int) (val & 0xFFFFFFFF));
      }
      break;
    case VALUE_TYPE_DOUBLE:
      {
        double val;
        val = tvb_get_ntohieee_double(tvb, ofs);
        len = 8;
        snprintf(buf, buflen, "%lg", val);
      }
      break;
    case VALUE_TYPE_STRING:
      {
        guint8* str;
        len = tvb_get_ntohl(tvb, ofs);
        ofs += 4;
        str = tvb_get_ephemeral_string(tvb, ofs, len);
        if (len > buflen - 3 && buflen >= 6) {
          snprintf(buf, buflen, "\"%.*s...\"", buflen - 6, (char*) str);
        } else {
          snprintf(buf, buflen, "\"%s\"", (char*) str);
        }
      }
      break;
  }
  /* ensure the buffer is null-terminated */;
  buf[buflen - 1] = 0;

  /* point to byte after this Value */
  return ofs + len;
}

/*
 * Show a labelled Value.
 *
 * hdr - name of this Value
 * ofs - offset into buffer where the Value tag byte is located.
 *
 * returns the offset after the last byte of this Value
 */
static int showValue(proto_tree* tree, char* hdr, tvbuff_t* tvb, int ofs) {
  proto_tree* subtree;
  proto_item* ti;
  guint8 tag;
  int newOffset;
  char buf[40];
  *buf = 0;
  newOffset = getValue(tvb, ofs, buf, sizeof(buf));
  tag = tvb_get_guint8(tvb, ofs);
  ti = proto_tree_add_string_format(tree, hf_gwtcs_val_hdr, tvb, ofs,
      newOffset - ofs, 0, "%s: %s", hdr, buf);
  subtree = proto_item_add_subtree(ti, ett_value);
  proto_tree_add_item(subtree, hf_gwtcs_value_tag, tvb, ofs++, 1, FALSE);
  switch (tag) {
    case VALUE_TYPE_NULL:
      proto_tree_add_item(subtree, hf_gwtcs_val_null, tvb, ofs, 0, FALSE);
      break;
    case VALUE_TYPE_UNDEFINED:
      proto_tree_add_item(subtree, hf_gwtcs_val_undef, tvb, ofs, 0, FALSE);
      break;
    case VALUE_TYPE_BOOLEAN:
      proto_tree_add_item(subtree, hf_gwtcs_val_bool, tvb, ofs, 1, FALSE);
      break;
    case VALUE_TYPE_BYTE:
      proto_tree_add_item(subtree, hf_gwtcs_val_byte, tvb, ofs, 1, FALSE);
      break;
    case VALUE_TYPE_CHAR:
      proto_tree_add_item(subtree, hf_gwtcs_val_char, tvb, ofs, 2, FALSE);
      break;
    case VALUE_TYPE_SHORT:
      proto_tree_add_item(subtree, hf_gwtcs_val_short, tvb, ofs, 2, FALSE);
      break;
    case VALUE_TYPE_INT:
      proto_tree_add_item(subtree, hf_gwtcs_val_int, tvb, ofs, 4, FALSE);
      break;
    case VALUE_TYPE_LONG:
      proto_tree_add_item(subtree, hf_gwtcs_val_long, tvb, ofs, 8, FALSE);
      break;
    case VALUE_TYPE_FLOAT:
      proto_tree_add_item(subtree, hf_gwtcs_val_float, tvb, ofs, 4, FALSE);
      break;
    case VALUE_TYPE_DOUBLE:
      proto_tree_add_item(subtree, hf_gwtcs_val_double, tvb, ofs, 8, FALSE);
      break;
    case VALUE_TYPE_STRING:
      proto_tree_add_item(subtree, hf_gwtcs_val_string, tvb, ofs, 4, FALSE);
      break;
    case VALUE_TYPE_JAVA_OBJECT:
      proto_tree_add_item(subtree, hf_gwtcs_val_javaobj, tvb, ofs, 4, FALSE);
      break;
    case VALUE_TYPE_JS_OBJECT:
      proto_tree_add_item(subtree, hf_gwtcs_val_jsobj, tvb, ofs, 4, FALSE);
      break;
  }
  return newOffset;
}

/*
 * Initialize memchunk system.
 */
static void init() {
  if (memChunk) {
    g_mem_chunk_destroy(memChunk);
  }
  memChunk = g_mem_chunk_new("gwtcs data", sizeof(gwtcs_data),
                             20 * sizeof(gwtcs_data), G_ALLOC_AND_FREE);
}

/*
 * Dissect a single packet.
 */
static int dissect_gwtcs(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree)
{
  guint8 packetType;
  conversation_t* conv;
  gwtcs_data* data = 0;
  int isClient = 0;

  if (tvb_length(tvb) < 1) {
    return 0;
  }

  packetType = tvb_get_guint8(tvb, 0);
  if (packetType > MAX_PACKET_TYPE) {
    return 0;
  }

  if (check_col(pinfo->cinfo, COL_PROTOCOL)) {
    col_set_str(pinfo->cinfo, COL_PROTOCOL, "GWT-CS");
  }

  /* Clear the info column */
  if (check_col(pinfo->cinfo, COL_INFO)) {
    col_set_str(pinfo->cinfo, COL_INFO, "");
  }

  /* get the conversation */
  conv = find_conversation(pinfo->fd->num, &pinfo->src, &pinfo->dst,
                           pinfo->ptype, pinfo->srcport, pinfo->destport, 0);
  if (!conv) {
    conv = conversation_new(pinfo->fd->num, &pinfo->src, &pinfo->dst,
                            pinfo->ptype, pinfo->srcport, pinfo->destport, 0);
  }
  data = (gwtcs_data*) conversation_get_proto_data(conv, proto_gwtcs);
  if (!data) {
    data = (gwtcs_data*) g_mem_chunk_alloc(memChunk);
    data->clientPort = -1;
    data->level = 0;
    conversation_add_proto_data(conv, proto_gwtcs, data);
  }

  if (packetType == MESSAGE_TYPE_CHECK_VERSIONS) {
    data->clientAddress = pinfo->src;
    data->clientPort = pinfo->srcport;
  }

  if (data->clientPort == pinfo->srcport) {
    isClient = 1;
  }

  /* Set the info column */
  if (check_col(pinfo->cinfo,COL_INFO)) {
    gint32 len;
    guint8* str;
    int i;
    gint32 offset = 1;
    if (data->clientPort != -1) {
      col_add_str(pinfo->cinfo, COL_INFO, isClient ? "C->S: " : "S->C: ");
    }
    for (i = 0; i < data->level; ++i) {
      col_append_str(pinfo->cinfo, COL_INFO, " ");
    }
    col_append_str(pinfo->cinfo, COL_INFO, packetTypes[packetType].strptr);
    switch (packetType) {
      case MESSAGE_TYPE_CHECK_VERSIONS:
        {
          int minvers, maxvers;
          minvers = tvb_get_ntohl(tvb, offset);
          offset += 4;
          maxvers = tvb_get_ntohl(tvb, offset);
          col_append_fstr(pinfo->cinfo, COL_INFO, " vers=%d-%d", minvers,
              maxvers);
        }
        break;
      case MESSAGE_TYPE_PROTOCOL_VERSION:
        {
          int vers;
          vers = tvb_get_ntohl(tvb, offset);
          col_append_fstr(pinfo->cinfo, COL_INFO, " vers=%d", vers);
        }
        break;
      case MESSAGE_TYPE_LOAD_MODULE:
        data->level++;
        // URL
        len = tvb_get_ntohl(tvb, offset);
        offset += 4 + len;
        // tab key
        len = tvb_get_ntohl(tvb, offset);
        offset += 4 + len;
        // session key
        len = tvb_get_ntohl(tvb, offset);
        offset += 4 + len;
        // module name
        len = tvb_get_ntohl(tvb, offset);
        offset  += 4;
        // clip string
        if (len > MAX_STRING_LENGTH) {
          len = MAX_STRING_LENGTH;
        }
        str = tvb_get_ephemeral_string(tvb, offset, len);
        col_append_fstr(pinfo->cinfo, COL_INFO, " %s", str);
        break;
      case MESSAGE_TYPE_INVOKE:
        data->level++;
        if (data->clientPort == -1) {
          break;
        }
        if (isClient) {
          int dispId;
          dispId = tvb_get_ntohl(tvb, offset);
          offset += 4;
          col_append_fstr(pinfo->cinfo, COL_INFO, " dispid=%d", dispId);
        } else {
          // module name
          len = tvb_get_ntohl(tvb, offset);
          offset += 4;
          // clip string
          if (len > MAX_STRING_LENGTH) {
            len = MAX_STRING_LENGTH;
          }
          str = tvb_get_ephemeral_string(tvb, offset, len);
          col_append_fstr(pinfo->cinfo, COL_INFO, " %s", str);
        }
        break;
      case MESSAGE_TYPE_RETURN:
        {
          char buf[40];
          guint8 isexc;
          data->level--;
          isexc = tvb_get_guint8(tvb, 1);
          getValue(tvb, 2, buf, sizeof(buf));
          col_append_fstr(pinfo->cinfo, COL_INFO, " %s%s",
              isexc ? "** EXCEPTION ** " : "", buf);
        }
        break;
      case MESSAGE_TYPE_INVOKESPECIAL:
        {
          guint8 specialType;
          data->level++;
          specialType = tvb_get_guint8(tvb, 1);
          col_append_fstr(pinfo->cinfo, COL_INFO, " %s",
              specialTypes[specialType].strptr);
        }
        break;
    }
  }

  /*
   * If tree is non-null, then we want to get the detailed data.
   */
  if (tree) {
    proto_item *ti = 0;
    proto_tree *gwtcs_tree = 0;
    gint32 offset = 1;

    ti = proto_tree_add_item(tree, proto_gwtcs, tvb, 0 , -1, FALSE);
    gwtcs_tree = proto_item_add_subtree(ti, ett_gwtcs);
    proto_tree_add_item(gwtcs_tree, hf_gwtcs_pdu_type, tvb, 0, 1, FALSE);
    switch (packetType) {
      case MESSAGE_TYPE_CHECK_VERSIONS:
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_min_vers, tvb, 1, 4, FALSE);
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_max_vers, tvb, 5, 4, FALSE);
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_hosted_vers, tvb, 9, 4, FALSE);
        break;
      case MESSAGE_TYPE_PROTOCOL_VERSION:
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_sel_vers, tvb, 1, 4, FALSE);
        break;
      case MESSAGE_TYPE_LOAD_MODULE:
        {
          guint32 len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_lm_url, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_lm_tabkey, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_lm_seskey, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_lm_modname, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_lm_ua, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
        }
        break;
      case MESSAGE_TYPE_INVOKE:
        {
          int numArgs, len, i;
          proto_item* ti;
          proto_tree* subtree;
          if (data->clientPort == -1) {
            proto_tree_add_text(gwtcs_tree, tvb, 1, -1,
                "Can't decode - unknown direction");
            break;
          }
          if (isClient) {
            proto_tree_add_item(gwtcs_tree, hf_gwtcs_dispid, tvb, offset, 4,
                FALSE);
            offset += 4;
          } else {
            // method name
            len = tvb_get_ntohl(tvb, offset);
            proto_tree_add_item(gwtcs_tree, hf_gwtcs_methname, tvb, offset, 4,
                FALSE);
            offset += 4 + len;
          }
          offset = showValue(gwtcs_tree, "This Value", tvb, offset);
          numArgs = tvb_get_ntohl(tvb, offset);
          ti = proto_tree_add_item(gwtcs_tree, hf_gwtcs_numargs, tvb, offset, 4,
              FALSE);
          subtree = proto_item_add_subtree(ti, ett_args);
          offset += 4;
          for (i = 0; i < numArgs; ++i) {
            char argName[10];
            snprintf(argName, sizeof(argName), "arg%d", i);
            offset = showValue(subtree, argName, tvb, offset);
          }
        }
        break;
      case MESSAGE_TYPE_RETURN:
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_isexc, tvb, 1, 1, FALSE);
        showValue(gwtcs_tree, "Return Value", tvb, 2);
        break;
      case MESSAGE_TYPE_LOADJSNI:
        proto_tree_add_item(gwtcs_tree, hf_gwtcs_jsni, tvb, 1, 4, FALSE);
        break;
      case MESSAGE_TYPE_INVOKESPECIAL:
        {
          int numArgs, i;
          proto_item* ti;
          proto_tree* subtree;
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_spectype, tvb, offset++, 1,
              FALSE);
          numArgs = tvb_get_ntohl(tvb, offset);
          ti = proto_tree_add_item(gwtcs_tree, hf_gwtcs_numargs, tvb, offset, 4,
              FALSE);
          offset += 4;
          subtree = proto_item_add_subtree(ti, ett_args);
          for (i = 0; i < numArgs; ++i) {
            char argName[10];
            snprintf(argName, sizeof(argName), "arg%d", i);
            offset = showValue(subtree, argName, tvb, offset);
          }
        }
        break;
      case MESSAGE_TYPE_FREEVALUE:
        {
          int numArgs, i, label;
          proto_item* ti;
          proto_tree* subtree;
          numArgs = tvb_get_ntohl(tvb, offset);
          ti = proto_tree_add_item(gwtcs_tree, hf_gwtcs_numargs, tvb, offset, 4,
              FALSE);
          offset += 4;
          subtree = proto_item_add_subtree(ti, ett_args);
          label = isClient ? hf_gwtcs_val_jsobj : hf_gwtcs_val_javaobj;
          for (i = 0; i < numArgs; ++i) {
            proto_tree_add_item(subtree, label, tvb, offset, 4, FALSE);
            offset += 4;
          }
        }
        break;
      case MESSAGE_TYPE_CHOOSE_TRANSPORT:
        {
          int numArgs, i, len;
          proto_item* ti;
          proto_tree* subtree;
          numArgs = tvb_get_ntohl(tvb, offset);
          ti = proto_tree_add_item(gwtcs_tree, hf_gwtcs_numargs, tvb, offset, 4,
              FALSE);
          offset += 4;
          subtree = proto_item_add_subtree(ti, ett_args);
          for (i = 0; i < numArgs; ++i) {
            len = tvb_get_ntohl(tvb, offset);
            proto_tree_add_item(subtree, hf_gwtcs_transport, tvb, offset, 4,
                FALSE);
            offset += 4 + len;
          }
        }
        break;
      case MESSAGE_TYPE_SWITCH_TRANSPORT:
        {
          int len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_transport, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
          len = tvb_get_ntohl(tvb, offset);
          proto_tree_add_item(gwtcs_tree, hf_gwtcs_transargs, tvb, offset, 4,
              FALSE);
          offset += 4 + len;
        }
        break;
    }
  }

  return tvb_length(tvb);
}

void proto_register_gwtcs(void)
{
  /*
   * List of subtree identifiers to be allocated.
   */
  static gint *ett[] = {
    &ett_gwtcs,
    &ett_value,
    &ett_args,
  };

  /*
   * List of display identifiers to be allocated.
   */
  static hf_register_info hf[] = {
    {
      &hf_gwtcs_pdu_type,
      {
        "Packet Type", "gwtcs.type",
        FT_UINT8, BASE_DEC, VALS(packetTypes), 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_spectype,
      {
        "Type", "gwtcs.spectype",
        FT_UINT8, BASE_DEC, VALS(specialTypes), 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_value_tag,
      {
        "Value Tag", "gwtcs.value.tag",
        FT_UINT8, BASE_DEC, VALS(valueTypes), 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_min_vers,
      {
        "Minimum version", "gwtcs.minvers",
        FT_UINT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_max_vers,
      {
        "Maximum version", "gwtcs.maxvers",
        FT_UINT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_hosted_vers,
      {
        "hosted.html version", "gwtcs.hostedvers",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_sel_vers,
      {
        "Selected version", "gwtcs.selvers",
        FT_UINT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_lm_url,
      {
        "URL", "gwtcs.lm.url",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_lm_modname,
      {
        "Module Name", "gwtcs.lm.modname",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_lm_tabkey,
      {
        "Tab Key", "gwtcs.lm.tabkey",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_lm_seskey,
      {
        "Session Key", "gwtcs.lm.seskey",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_lm_ua,
      {
        "User Agent", "gwtcs.lm.ua",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_methname,
      {
        "Method Name", "gwtcs.lm.methname",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_isexc,
      {
        "Is Exception", "gwtcs.isexc",
        FT_BOOLEAN, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_dispid,
      {
        "Dispatch ID", "gwtcs.dispid",
        FT_UINT32, BASE_HEX, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_jsni,
      {
        "JSNI Source", "gwtcs.jsni",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_hdr,
      {
        "Value", "gwtcs.val.hdr",
        FT_STRINGZ, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_numargs,
      {
        "# of Arguments", "gwtcs.val.numargs",
        FT_UINT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_null,
      {
        "Null Value", "gwtcs.val.null",
        FT_NONE, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_undef,
      {
        "Undef Value", "gwtcs.val.undef",
        FT_NONE, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_bool,
      {
        "Boolean Value", "gwtcs.val.bool",
        FT_BOOLEAN, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_byte,
      {
        "Byte Value", "gwtcs.val.byte",
        FT_INT8, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_char,
      {
        "Char Value", "gwtcs.val.char",
        FT_UINT16, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_short,
      {
        "Short Value", "gwtcs.val.short",
        FT_INT16, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_int,
      {
        "Int Value", "gwtcs.val.int",
        FT_INT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_long,
      {
        "Long Value", "gwtcs.val.long",
        FT_INT64, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_float,
      {
        "Float Value", "gwtcs.val.float",
        FT_FLOAT, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_double,
      {
        "Double Value", "gwtcs.val.double",
        FT_DOUBLE, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_javaobj,
      {
        "Java Object Id", "gwtcs.val.javaobj",
        FT_INT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_jsobj,
      {
        "JS Object Id", "gwtcs.val.jsobj",
        FT_INT32, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_val_string,
      {
        "String Value", "gwtcs.val.string",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_transport,
      {
        "Transport Name", "gwtcs.transport",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
    {
      &hf_gwtcs_transargs,
      {
        "Transport Args", "gwtcs.transargs",
        FT_UINT_STRING, BASE_DEC, NULL, 0x0, NULL, HFILL,
      }
    },
  };

  if (proto_gwtcs == -1)
  {
     register_init_routine(&init);
     proto_gwtcs = proto_register_protocol (
        "GWT Code Server Protocol", /* name */
        "GWT-CS",          /* short name */
        "gwtcs"           /* abbrev */
     );

     proto_register_field_array(proto_gwtcs, hf, array_length(hf));
     proto_register_subtree_array(ett, array_length(ett));
  }
}

void proto_reg_handoff_gwtcs(void)
{
   static int Initialized=FALSE;

   /* register with wireshark to dissect tdp packets on port 9997 */
   if (!Initialized) {
      gwtcs_handle = new_create_dissector_handle(dissect_gwtcs, proto_gwtcs);
      dissector_add("tcp.port", DEFAULT_GWTCS_PORT, gwtcs_handle);
      Initialized = TRUE;
   }
}
