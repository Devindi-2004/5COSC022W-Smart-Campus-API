package uk.ac.westminster.smartcampus.repository;

import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Using ConcurrentHashMap to handle multithreaded requests.
public class DataStore {

    private static Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private static Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Load some mock data for ease of testing
    static {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        rooms.put(r1.getId(), r1);
        
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        r1.getSensorIds().add(s1.getId());
        sensors.put(s1.getId(), s1);
        
        sensorReadings.put(s1.getId(), new ArrayList<>());
    }

    public static Map<String, Room> getRooms() {
        return rooms;
    }

    public static Map<String, Sensor> getSensors() {
        return sensors;
    }

    public static Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }
}
