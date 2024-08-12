package FlightTracks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Main <flight_data_file> <requested_flights_file> <output_file>");
            return;
        }
        FlightGraph flightGraph = new FlightGraph();
        flightGraph.loadFlightData(args[0]);
        flightGraph.processFlightRequests(args[1], args[2]);
    }
}

class FlightGraph {
    private List<City> cities = new LinkedList<>();

    public void loadFlightData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // Skip the first line as it contains the count
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\\|");
                String origin = data[0];
                String destination = data[1];
                int cost = Integer.parseInt(data[2]);
                int duration = Integer.parseInt(data[3]);
                City originCity = findOrCreateCity(origin);
                City destinationCity = findOrCreateCity(destination);
                Flight flight = new Flight(destinationCity, cost, duration);
                Flight reverseFlight = new Flight(originCity, cost, duration);
                originCity.addNeighbor(flight);
                destinationCity.addNeighbor(reverseFlight);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private City findOrCreateCity(String cityName) {
        for (City city : cities) {
            if (city.name.equals(cityName)) {
                return city;
            }
        }
        City newCity = new City(cityName);
        cities.add(newCity);
        return newCity;
    }

    public void processFlightRequests(String inputFile, String outputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             FileWriter writer = new FileWriter(outputFile)) {
            reader.readLine(); // Skip the count
            int flightCount = 1;
            String request;
            while ((request = reader.readLine()) != null) {
                String[] parts = request.split("\\|");
                String origin = parts[0];
                String destination = parts[1];
                boolean sortByTime = parts[2].equals("T");
                String sortCriteria = sortByTime ? "Time" : "Cost";
                City originCity = findOrCreateCity(origin);
                City destinationCity = findOrCreateCity(destination);
                List<Route> routes = RouteFinderUtils.findRoutes(originCity, destinationCity, sortByTime);
                if (routes.isEmpty()) {
                    writer.write("No routes found for " + origin + " to " + destination + "\n");
                } else {
                    writer.write("Flight " + flightCount + ": " + origin + ", " + destination + " (" + sortCriteria + ")\n");
                    for (int i = 0; i < Math.min(3, routes.size()); i++) {
                        writer.write("Path " + (i + 1) + ": " + routes.get(i) + "\n");
                    }
                    flightCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class City {
    String name;
    List<Flight> neighbors;
    public City(String name) {
        this.name = name;
        neighbors = new LinkedList<>();
    }
    public void addNeighbor(Flight flight) {
        neighbors.add(flight);
    }
}

class Flight {
    City destination;
    int cost;
    int duration;
    public Flight(City destination, int cost, int duration) {
        this.destination = destination;
        this.cost = cost;
        this.duration = duration;
    }
}

class Route {
    List<City> cities;
    int totalCost;
    int totalDuration;
    public Route() {
        cities = new LinkedList<>();
        totalCost = 0;
        totalDuration = 0;
    }
    public Route(Route other) { // Copy constructor for deep copying
        this.cities = new LinkedList<>(other.cities);
        this.totalCost = other.totalCost;
        this.totalDuration = other.totalDuration;
    }
    public void addCity(City city) {
        cities.add(city);
    }
    public void addFlight(Flight flight) {
        addCity(flight.destination);
        totalCost += flight.cost;
        totalDuration += flight.duration;
    }
    @Override
    public String toString() {
        return cities.stream()
                     .map(city -> city.name)
                     .collect(Collectors.joining(" -> "))
              + ". Time: " + totalDuration + " Cost: " + String.format("%.2f", (float)totalCost);
    }
}

class RouteFinderUtils {
    public static List<Route> findRoutes(City origin, City destination, boolean sortByTime) {
        List<Route> routes = new ArrayList<>();
        Stack<Route> routeStack = new Stack<>();
        Route startRoute = new Route();
        startRoute.addCity(origin);
        routeStack.push(startRoute);
        while (!routeStack.isEmpty()) {
            Route currentRoute = routeStack.pop();
            City lastCity = currentRoute.cities.get(currentRoute.cities.size() - 1);
            if (lastCity.equals(destination)) {
                routes.add(new Route(currentRoute)); 
                continue;
            }
            for (Flight flight : lastCity.neighbors) {
                if (!currentRoute.cities.contains(flight.destination)) { 
                    Route newRoute = new Route(currentRoute);  
                    newRoute.addFlight(flight);
                    routeStack.push(newRoute);
                }
            }
        }
       
        if (sortByTime) {
            routes.sort(Comparator.comparingInt(r -> r.totalDuration));
        } else {
            routes.sort(Comparator.comparingInt(r -> r.totalCost));
        }
        return routes.stream().limit(3).collect(Collectors.toList());
    }
}




