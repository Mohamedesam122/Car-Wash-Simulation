package com.mycompany.servicestation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.InputMismatchException;
import java.util.Scanner;

class Semaphore {
    private int value = 0;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSem() throws InterruptedException {
        value--;
        if (value < 0) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized void signalSem() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}

// ====================== CAR (PRODUCER) ======================
class Car extends Thread {
    private String carId;
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;

    public Car(String carId, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex) {
        this.carId = carId;
        this.queue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
    }

    @Override
    public void run() {
        try {
            empty.waitSem();
            mutex.waitSem();
            queue.add(carId);

            if (queue.size() <=1) {

            } else
            {
                System.out.println("• " + carId + " arrived and waiting");
            }

            mutex.signalSem();
            full.signalSem();

        } catch (InterruptedException e) {
            System.out.println(carId + " was interrupted.");
        }
    }
}

// ====================== PUMP (CONSUMER) ======================
class Pump extends Thread {
    private int pumpId;
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;
    private Semaphore pumps;

    public Pump(int pumpId, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex, Semaphore pumps) {
        this.pumpId = pumpId;
        this.queue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.pumps = pumps;
    }

    @Override
    public void run() {
        try {
            while (true) {
                full.waitSem();
                pumps.waitSem();
                mutex.waitSem();

                String carId = null;

                if (!queue.isEmpty()) {
                    carId = queue.remove();
                    System.out.println("• Pump " + pumpId + ": " + carId + " login");
                    System.out.println("• Pump " + pumpId + ": " + carId + " begins service at Bay " + pumpId);
                }

                mutex.signalSem();
                empty.signalSem();

                Thread.sleep(2000);   

                if (carId != null) {
                    System.out.println("• Pump " + pumpId + ": " + carId + " finishes service");
                }
                System.out.println("• Pump " + pumpId + ": Bay " + pumpId + " is now free");

                pumps.signalSem();
            }
        } catch (InterruptedException e) {
            System.out.println("Pump " + pumpId + " interrupted");
        }
    }

}

// ====================== SERVICE STATION ======================

public class ServiceStation {
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;
    private Semaphore pumps;
    private int waitingCapacity;
    private int numberOfPumps;
    private String[] arrivingCars;

    public ServiceStation(int waitingCapacity, int numberOfPumps, String[] arrivingCars) {
        this.waitingCapacity = waitingCapacity;
        this.numberOfPumps = numberOfPumps;
        this.arrivingCars = arrivingCars;

        this.queue = new LinkedList<>();
        this.empty = new Semaphore(waitingCapacity);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);
        this.pumps = new Semaphore(numberOfPumps);
    }

    public void startSimulation() {
        for (int i = 1; i <= numberOfPumps; i++) {
            Pump p = new Pump(i, queue, empty, full, mutex, pumps);
            p.start();
        }

        for (String car : arrivingCars) {
            System.out.println("• " + car + " arrived");
        }

        int min = Math.min(numberOfPumps, arrivingCars.length);
        for (int i = 0; i < min; i++) {
            System.out.println("• Pump " + (i + 1) + ": " + arrivingCars[i] + " Occupied");
        }

        for (int i = numberOfPumps; i < arrivingCars.length; i++) {
            System.out.println("• " + arrivingCars[i] + " arrived and waiting");
        }

        for (String car : arrivingCars) {
            Car c = new Car(car, queue, empty, full, mutex);
            c.start();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Let simulation run
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("• All cars processed; simulation ends");
        System.exit(0);
    }

    // ======================= MAIN =======================
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        while (true) {
            try {
                System.out.println("\n=== Car Wash / Service Station Simulation ===");

                System.out.print("• Waiting area capacity (1–10): ");
                int waiting = input.nextInt();
                if (waiting < 1 || waiting > 10) {
                    System.out.println("Invalid input. Must be between 1 and 10.");
                    continue;
                }

                System.out.print("• Number of service bays (pumps) (1–10): ");
                int pumps = input.nextInt();
                if (pumps < 1 || pumps > 10) {
                    System.out.println(" Invalid input. Must be between 1 and 10.");
                    continue;
                }

                input.nextLine();
                System.out.print("• Cars arriving (order): ");
                String carsLine = input.nextLine().trim();

                if (carsLine.isEmpty()) {
                    System.out.println("Please enter car names like C1, C2, C3 ...");
                    continue;
                }

                String[] cars = carsLine.replace(",", " ").trim().split("\\s+");

                ServiceStation station = new ServiceStation(waiting, pumps, cars);
                System.out.println();
                station.startSimulation();
                break;

            } catch (InputMismatchException e) {
                System.out.println(" Input error: please enter numeric values only.");
                input.nextLine(); // clear buffer
            } catch (Exception e) {
                System.out.println(" Unexpected error: " + e.getMessage());
            }
        }

        input.close();
    }
}

