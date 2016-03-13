CServer {
   httpConnector {
      port = 8080;
   }
   handlerList {
      webAppHandler {
         contextPath = "/";
         war="./web";
      }
   }
}
