package test;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ServletMappingTestServiceImpl1 extends RemoteServiceServlet
    implements ServletMappingTestService {

  public int which() {
    return 1;
  }

}
