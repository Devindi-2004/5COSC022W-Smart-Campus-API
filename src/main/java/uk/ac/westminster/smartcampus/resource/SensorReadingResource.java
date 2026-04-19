package uk.ac.westminster.smartcampus.resource;

import uk.ac.westminster.smartcampus.exception.SensorUnavailableException;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;
import uk.ac.westminster.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        if (!DataStore.getSensors().containsKey(sensorId)) {
             return Response.status(Response.Status.NOT_FOUND)
                     .entity("{\"error\": \"Sensor not found\"}")
                     .build();
        }
        List<SensorReading> readings = DataStore.getSensorReadings().get(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Sensor not found\"}")
                    .build();
        }
        
        // Constraint from Part 5: "MAINTENANCE" stops new readings -> trigger 403 Forbidden
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()) || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor is currently offline or in maintenance.");
        }
        
        // Update the reading
        if (reading.getId() == null) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }
        
        // Side Effect (Part 4): A successful POST must update the current value on the parent Sensor
        sensor.setCurrentValue(reading.getValue());
        
        DataStore.getSensorReadings().get(sensorId).add(reading);
        
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
