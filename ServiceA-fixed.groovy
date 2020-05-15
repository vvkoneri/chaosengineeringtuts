@Grapes([
    @Grab('org.eclipse.jetty:jetty-server:9.4.3.v20170317'),
    @Grab('org.eclipse.jetty:jetty-servlet:9.4.3.v20170317'),
    @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
])

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.HttpResponseDecorator

import groovy.json.JsonSlurper

class JettyServer {
    Server server;
 
    public void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8093);
        server.addConnector(connector);
        
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        servletHandler.addServletWithMapping(ServiceAServlet.class, "/employees");
        
        server.start()
        
    }
}

class ServiceAServlet extends HttpServlet {
    final String hostname = "http://localhost:8091"
    final String log_file_location = "/Users/vekoneri/Documents/transaction.log"
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
  
        def department = request.getParameter("dept")
        def lastname = request.getParameter("lastname")
        lastname = lastname ?: '%'

        response.setContentType("application/json");
        // Validates to ensure department is passed in request else throws 400 error
        if(!department) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{ \"message\": \"No Department Id Specified\"}");
            return
        }
        
        try {
            String responseJson = callServiceB(department,lastname)
            
            writeToTransaction(responseJson,request.getRemoteAddr())
        
            response.getWriter().println(responseJson)
        } catch (HttpResponseException ex) {
            HttpResponseDecorator responseDecorator = ex.getResponse()
            response.setStatus(responseDecorator.getStatus());
            response.getWriter().println(new groovy.json.JsonBuilder(responseDecorator.getData()).toString())
        }
    }
    
    /** Calls service B 
        to retrieve employee records based on the department and last name
    **/
    private String callServiceB(String department, String lastname) {
        def serviceB = new RESTClient(hostname)
        def params = [dept : department, ln : lastname]
        def json = serviceB.get ( path : '/search', query : params ) { response , json -> 
                new groovy.json.JsonBuilder(json).toString()
        }   
    }
    /**
        Does line count to assign id to the next record 
        and updates the retrieved records to the transaction log file
    **/
    private void writeToTransaction(String json, String remoteAddress) {
        File logFile = new File(log_file_location)
        def lineList = []
        if (logFile.exists()) {
            FileReader reader = new FileReader(logFile)
            lineList = logFile.readLines()
        }
        
        def currentTime = new Date()
        logFile.append("{'id' : ${ lineList ? lineList.size() + 1 : 1}, 'IP' : $remoteAddress, 'time' : ${currentTime}, 'data' : ${json} } \n")
        
    }     
}

JettyServer server = new JettyServer()

server.start()