package test;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ServletMappingTestServiceImpl3 extends RemoteServiceServlet
    implements ServletMappingTestService {

  public int which() {
    return 3;
  }

}
