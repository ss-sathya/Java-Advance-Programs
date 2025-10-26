import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class TrafficSignalController {
    private String currentGreen = "North";
    private final List<String> roads = Arrays.asList("North", "East", "South", "West");

    public synchronized void switchSignal() {
        int index = roads.indexOf(currentGreen);
        currentGreen = roads.get((index + 1) % roads.size());
        notifyAll(); // Wake up all waiting threads
    }

    public synchronized boolean isGreen(String road) {
        return currentGreen.equals(road);
    }

    public synchronized void waitForGreen(String road) {
        while (!currentGreen.equals(road)) {
            try {
                wait(); // Wait until the signal turns green for this road
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public String getCurrentGreen() {
        return currentGreen;
    }
}

class RoadThread extends Thread {
    private final String roadName;
    private final TrafficSignalController controller;
    private final Random random = new Random();
    private final AtomicBoolean running;
    private int carsPassed = 0;

    public RoadThread(String roadName, TrafficSignalController controller, AtomicBoolean running) {
        this.roadName = roadName;
        this.controller = controller;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get()) {
            controller.waitForGreen(roadName);

            // Simulate cars passing when green
            int cars = random.nextInt(5) + 1; // 1–5 cars
            carsPassed += cars;
            System.out.println(roadName + " signal GREEN — " + cars + " cars passed.");
            printRedRoads();

            try {
                Thread.sleep((random.nextInt(3) + 2) * 1000L); // 2–5 sec green time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (controller.isGreen(roadName)) {
                controller.switchSignal();
                System.out.println("Switching signal...\n");
            }
        }
        System.out.println(roadName + " road stopped. Total cars passed: " + carsPassed);
    }

    private void printRedRoads() {
        for (String road : Arrays.asList("North", "East", "South", "West")) {
            if (!road.equals(roadName)) {
                System.out.println(road + " signal RED — cars waiting...");
            }
        }
    }

    public int getCarsPassed() {
        return carsPassed;
    }
}

public class TrafficSimulation {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚦 Starting Traffic Signal Simulation...\n");
        TrafficSignalController controller = new TrafficSignalController();
        AtomicBoolean running = new AtomicBoolean(true);

        RoadThread north = new RoadThread("North", controller, running);
        RoadThread east = new RoadThread("East", controller, running);
        RoadThread south = new RoadThread("South", controller, running);
        RoadThread west = new RoadThread("West", controller, running);

        north.start();
        east.start();
        south.start();
        west.start();

        // Run simulation for 30 seconds
        Thread.sleep(30000);
        running.set(false);

        // Wake all waiting threads to exit cleanly
        synchronized (controller) {
            controller.notifyAll();
        }

        north.join();
        east.join();
        south.join();
        west.join();

        int total = north.getCarsPassed() + east.getCarsPassed()
                  + south.getCarsPassed() + west.getCarsPassed();

        System.out.println("\n🚦 Simulation Ended.");
        System.out.println("Total cars passed: " + total);
    }
}
