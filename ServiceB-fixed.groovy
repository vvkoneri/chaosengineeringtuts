@Grapes([
    @Grab('org.eclipse.jetty:jetty-server:9.4.3.v20170317'),
    @Grab('org.eclipse.jetty:jetty-servlet:9.4.3.v20170317'),
    @Grab('mysql:mysql-connector-java:8.0.17')
])
@GrabConfig(systemClassLoader = true)

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import groovy.sql.Sql
import groovy.json.JsonSlurper

class JettyServer {
    Server server;
 
    public void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8091);
        server.addConnector(connector);
        
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        servletHandler.addServletWithMapping(ServiceBServlet.class, "/search");
        
        server.start()
        
    }
}

class ServiceBServlet extends HttpServlet {
    
    def username = 'root'
    def password = 'password123'
    
    
    protected void doGet(
      HttpServletRequest request, 
      HttpServletResponse response)
      throws ServletException, IOException {
  
        def department = request.getParameter("dept")
        def lastname = request.getParameter("ln")   
        def employee = []
        response.setContentType("application/json");

        def sql = null;
        try {
           sql = Sql.newInstance("jdbc:mysql://localhost:3306/company",username,password,"com.mysql.jdbc.Driver")
           employee = sql.rows("select * from employees where department_id = ${department} and last_name LIKE ${lastname}")
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            response.getWriter().println("{ \"error\": \"We are currently facing some difficulties... Please try after sometime \"} ")
            return
        } finally {
            if(sql) {
                sql.close()
            }
            
        }
        
        def jsonBuilder = new groovy.json.JsonBuilder()
        
        jsonBuilder {
            employees employee.collect {
                [
                    'firstname' : it.get('first_name'),
                    'lastname' : it.get('last_name'),
                    'department_name' : it.get('department_name'),
                    'department_id' : it.get('department_id'),
                    'age' : it.get('age')
                    
                ]
            }
        }
        
           
        response.getWriter().println(jsonBuilder.toString())
    }
    
   
    
       
}

JettyServer server = new JettyServer()
server.start()