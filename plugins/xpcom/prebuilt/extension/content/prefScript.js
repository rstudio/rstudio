var GwtDeveloperPlugin = {

// Add a new entry when the Add Entry button is clicked.
addEntry: function() {
  var prefs = this.getAccessList();
  var hostname = document.getElementById("hostname").value;
  if (!hostname || hostname.length == 0) {
    alert("No host name provided");
    return;
  }
  if (hostname.indexOf(",") >=0 || hostname.indexOf("!") >= 0 || hostname.indexOf("/") >= 0) {
    alert("Host name must not contain ',', '!', or '/'");
    return;
  }
  var codeserver = document.getElementById("codeserver").value;
  if (!codeserver || codeserver.length == 0) {
    alert("No code server provided");
    return;
  }
  if (codeserver.indexOf(",") >=0 || codeserver.indexOf("!") >= 0 || codeserver.indexOf("/") >= 0) {
    alert("Code server must not contain ',', '!', or '/'");
    return;
  }
  var exclude = document.getElementById("exclude");
  var incText;
  var prefix = "";
  if (exclude.selected) {
    incText = "Exclude";
    prefix = "!";
  } else {
    incText = "Include";
  }
  var listboxEntry = this.makeLBE(incText, hostname, codeserver);
  var prefsEntry = prefix + hostname + '/' + codeserver;
  var listbox = document.getElementById("accessListListbox");
  listbox.appendChild(listboxEntry);
  prefs.push(prefsEntry.toString());
  this.saveAccessList(prefs);
},

// Remove the selected entry when the Remove Entry button is clicked.
removeEntry: function() {
  var listbox = document.getElementById("accessListListbox");
  var idx = listbox.selectedIndex;
  if (idx >= 0) {
    listbox.removeItemAt(idx);
    var prefs = this.getAccessList();
    prefs.splice(idx, 1);
    this.saveAccessList(prefs);
  }
},

// Populate the listbox when the dialog window is loaded
onload: function() {
  var listbox = document.getElementById("accessListListbox");
  var prefs = this.getAccessList();
  for (var i = 0 ; i < prefs.length; ++i) {
    var pref = prefs[i];
    var hostname = pref;
    var incexc = "Include";
    if (pref.length > 0 && pref.charAt(0) == "!") {
      hostname = hostname.substr(1);
      incexc = "Exclude";
    }  
    var codeserver = "localhost";
    var slash = hostname.indexOf("/");
    if( slash >= 0 )
    {
      codeserver = hostname.substr(slash+1);
      hostname   = hostname.substr(0,slash);
    }
    var listboxEntry = this.makeLBE(incexc, hostname, codeserver);
    listbox.appendChild(listboxEntry);
  }
},

// Internal - create a entry for the list box
makeLBE: function(inc, hostname, codeserver) {
  var listboxEntry = document.createElement("listitem");
  var lbeInc = document.createElement("listcell");
  lbeInc.setAttribute("label", inc);
  listboxEntry.appendChild(lbeInc);
  var lbeHost = document.createElement("listcell");
  lbeHost.setAttribute("label", hostname);
  listboxEntry.appendChild(lbeHost);
  var lbeCode = document.createElement("listcell");
  lbeCode.setAttribute("label", codeserver);
  listboxEntry.appendChild(lbeCode);
  return listboxEntry;
},

// Internal - load the access list from the gwt-dev-plugin.accessList
// preference
getAccessList: function() {
  var prefServ = Components.classes["@mozilla.org/preferences-service;1"]
                  .getService(Components.interfaces.nsIPrefService);
  var prefs = prefServ.getBranch("gwt-dev-plugin.");
  var pref = prefs.getCharPref("accessList");
  if (!pref) {
    return [];
  }
  return pref.split(",");
},

// Internal - save the access list to the gwt-dev-plugin.accessList
// preference
saveAccessList: function(list) {
  var prefServ = Components.classes["@mozilla.org/preferences-service;1"]
                  .getService(Components.interfaces.nsIPrefService);
  var prefs = prefServ.getBranch("gwt-dev-plugin.");
  prefs.setCharPref("accessList", list.join(","));
  prefServ.savePrefFile(null);
}

};
