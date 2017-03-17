package io.pivotal.pde.sample.airline.ui;

import org.apache.geode.cache.Region;
import org.springframework.context.ApplicationContext;

import com.vaadin.server.VaadinServlet;

import io.pivotal.pde.sample.airline.domain.Airport;

public class AppContext {
     static final String SPRING_CONTEXT_ATTRIBUTE = "SPRING_CONTEXT";
	
     private static ApplicationContext getSpringApplicationContext(){
    	 ApplicationContext ctx = (ApplicationContext) VaadinServlet.getCurrent().getServletContext().getAttribute(SPRING_CONTEXT_ATTRIBUTE);
    	 return ctx;
     }
     
     public static Region<String,Airport> getAirportRegion(){
    	 return (Region<String, Airport>) getSpringApplicationContext().getBean("airportRegion");
     }
     
}
