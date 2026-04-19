package uk.ac.westminster.smartcampus.config;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        // Register packages where resources and providers (filters, interceptors) are located
        packages("uk.ac.westminster.smartcampus.resource", "uk.ac.westminster.smartcampus.exception", "uk.ac.westminster.smartcampus.filter");
    }
}
