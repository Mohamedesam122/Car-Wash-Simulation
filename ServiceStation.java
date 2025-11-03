package com.mycompany.servicestation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.InputMismatchException;


class Semaphore {
    private int value;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSem() throws InterruptedException {
        // TODO: implement wait (P) operation
    }

    public synchronized void signalSem() {
        // TODO: implement signal (V) operation
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
        // TODO: implement car arrival and queueing logic
    }
}

// ====================== PUMP (CONSUMER) ======================
class Pump extends Thread {
    private int pumpId;
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;
    private Semaphore pumps; // represents available service bays

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
        // TODO: implement pump logic (acquire, serve, release)
    }
}

// ====================== SERVICE STATION (MAIN CLASS) ======================
public class ServiceStation {
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;
    private Semaphore pumps;
    private int waitingCapacity;
    private int numberOfPumps;

    public ServiceStation(int waitingCapacity, int numberOfPumps) {
        this.waitingCapacity = waitingCapacity;
        this.numberOfPumps = numberOfPumps;

        // TODO: initialize queue and semaphores
    }

    public void startSimulation() {
        // TODO: start pump threads and create car threads dynamically
    }

    // ====================== MAIN METHOD ======================
    public static void main(String[] args) {
        java.util.Scanner input = new java.util.Scanner(System.in);

        try {
            System.out.println("=== Car Wash Simulation ===");

            System.out.print("Enter waiting area capacity (1–10): ");
            int waiting = input.nextInt();
            if (waiting < 1 || waiting > 10) {
                System.out.println("Invalid waiting area capacity. Must be between 1 and 10.");
                return;
            }

            System.out.print("Enter number of pumps (1–10): ");
            int pumps = input.nextInt();
            if (pumps < 1 || pumps > 10) {
                System.out.println(" Invalid number of pumps. Must be between 1 and 10.");
                return;
            }

            ServiceStation station = new ServiceStation(waiting, pumps);
            station.startSimulation();

        } catch (InputMismatchException e) {
            System.out.println(" Input error: please enter numeric values only.");
        } catch (Exception e) {
            System.out.println("️ An unexpected error occurred: " + e.getMessage());
        } finally {
            input.close();
            System.out.println("Simulation ended.");
        }
    }
}