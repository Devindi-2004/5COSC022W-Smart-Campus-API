package uk.ac.westminster.smartcampus.resource;

import uk.ac.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(DataStore.getSensors().values());
        
        if (type != null && !type.isEmpty()) {
            sensors = sensors.stream()
                             .filter(s -> type.equalsIgnoreCase(s.getType()))
                             .collect(Collectors.toList());
        }
        
        return Response.ok(sensors).build();
    }

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null || sensor.getId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Sensor or Sensor ID cannot be empty\"}")
                           .build();
        }
        if (DataStore.getSensors().containsKey(sensor.getId())) {
             return Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\": \"Sensor already exists\"}")
                            .build();
        }
        
        String roomId = sensor.getRoomId();
        if (roomId == null || !DataStore.getRooms().containsKey(roomId)) {
            // Exception Mapper triggers 422
            throw new LinkedResourceNotFoundException("The supplied roomId does not exist.");
        }
        
        // Link to room
        Room room = DataStore.getRooms().get(roomId);
        room.getSensorIds().add(sensor.getId());
        
        DataStore.getSensors().put(sensor.getId(), sensor);
        DataStore.getSensorReadings().put(sensor.getId(), new ArrayList<>());
        
        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    // Sub-Resource locator for Sensor Readings
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
