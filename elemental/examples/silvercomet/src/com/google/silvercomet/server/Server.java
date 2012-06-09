package com.google.silvercomet.server;

import com.google.common.flags.Flags;
import com.google.gse.GoogleServletEngine;
import com.google.gwt.gserver.GwtResourceServlet;
import com.google.net.base.IpScanners;

import java.io.IOException;

public class Server {
  public static void main(String[] args) throws IOException {
    Flags.parse(args);
    final GoogleServletEngine gse = new GoogleServletEngine();
    gse.configure();
    gse.setServerType("what_the_fuck_is_all_this_random_stuff");
    gse.getPathDispatcher().addServlet("/*", new GwtResourceServlet(
        Server.class.getClassLoader(),
        IpScanners.ALLOW_ALL,
        "SilverComet"));
    gse.run();
  }
}
