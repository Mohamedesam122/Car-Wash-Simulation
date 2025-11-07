package com.mycompany.servicestation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;


class Semaphore {
    private int value = 0;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSem() throws InterruptedException {
        value--;
        if (value < 0) {
            wait();
        }
    }

    public synchronized void signalSem() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}


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

           
            if (ServiceStation.gui != null) {
                List<String> snapshot = new ArrayList<>(queue);
                ServiceStation.gui.updateQueue(snapshot);
            }

            if (queue.size() > 1)
                System.out.println(" " + carId + " arrived and waiting");

            mutex.signalSem();
            full.signalSem();

        } catch (InterruptedException e) {
            System.out.println(carId + " was interrupted.");
        }
    }
}


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
                    System.out.println(" Pump " + pumpId + ": " + carId + " login");
                    System.out.println(" Pump " + pumpId + ": " + carId + " begins service at Bay " + pumpId);

                    if (ServiceStation.gui != null) {
                        ServiceStation.gui.updatePump(pumpId - 1, "Busy - " + carId, true);
                        List<String> snapshot = new ArrayList<>(queue);
                        ServiceStation.gui.updateQueue(snapshot);
                    }
                }

                mutex.signalSem();
                empty.signalSem();

                Thread.sleep(2000);

                if (carId != null)
                    System.out.println(" Pump " + pumpId + ": " + carId + " finishes service");
                System.out.println(" Pump " + pumpId + ": Bay " + pumpId + " is now free");

                if (ServiceStation.gui != null) {
                    ServiceStation.gui.updatePump(pumpId - 1, "Free", false);
                }

                pumps.signalSem();
            }
        } catch (InterruptedException e) {
            System.out.println("Pump " + pumpId + " interrupted");
        }
    }
}

 class ServiceStation {
    private Queue<String> queue;
    private Semaphore empty;
    private Semaphore full;
    private Semaphore mutex;
    private Semaphore pumps;
    private int waitingCapacity;
    private int numberOfPumps;
    private String[] arrivingCars;

    public static CarWashGUI gui = null;

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

        for (String car : arrivingCars)
            System.out.println(" " + car + " arrived");

        int min = Math.min(numberOfPumps, arrivingCars.length);
        for (int i = 0; i < min; i++)
            System.out.println(" Pump " + (i + 1) + ": " + arrivingCars[i] + " Occupied");

        for (int i = numberOfPumps; i < arrivingCars.length; i++)
            System.out.println(" " + arrivingCars[i] + " arrived and waiting");

        if (gui != null) {
            gui.buildDynamicPanels(waitingCapacity, numberOfPumps);
            for (int i = 0; i < numberOfPumps && i < arrivingCars.length; i++)
                gui.updatePump(i, "Occupied - " + arrivingCars[i], true);

            List<String> initialQueue = new ArrayList<>();
            for (int i = numberOfPumps; i < arrivingCars.length; i++)
                initialQueue.add(arrivingCars[i]);
            gui.updateQueue(initialQueue);
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

        try {
            Thread.sleep(arrivingCars.length * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(" All cars processed; simulation ends");
        if (gui == null)
            System.exit(0);
        else
            SwingUtilities.invokeLater(() -> gui.enableStartAgain());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CarWashGUI().setVisible(true));
    }

    public static void mainWithGUI(int waitingCapacity, int pumpsCount, String[] cars, CarWashGUI cg) {
        gui = cg;
        ServiceStation station = new ServiceStation(waitingCapacity, pumpsCount, cars);
        station.startSimulation();
    }
}

class CarWashGUI extends JFrame {
    private JTextField capacityField, pumpsField, carsField;
    private JButton startButton, resetButton;
    private JTextArea logArea;
    private JPanel pumpsPanel, queuePanel, centerPanel;
    private List<JLabel> pumpLabels = new ArrayList<>();
    private List<JLabel> queueLabels = new ArrayList<>();
    private static CarWashGUI instance = null;

    public CarWashGUI() {
        instance = this;
        setTitle("🚗 Car Wash Simulation");
        setSize(950, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(230, 240, 255));
        setLayout(new BorderLayout(10, 10));


        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        inputPanel.setBackground(new Color(200, 220, 255));
        inputPanel.setBorder(new TitledBorder("Simulation Setup"));
        inputPanel.add(new JLabel("Waiting Area Capacity:", SwingConstants.RIGHT));
        capacityField = new JTextField("5");
        inputPanel.add(capacityField);
        inputPanel.add(new JLabel("Number of Pumps:", SwingConstants.RIGHT));
        pumpsField = new JTextField("3");
        inputPanel.add(pumpsField);
        inputPanel.add(new JLabel("Cars (e.g., C1 C2 C3 C4 C5):", SwingConstants.RIGHT));
        carsField = new JTextField("C1 C2 C3 C4 C5");
        inputPanel.add(carsField);

        startButton = new JButton("▶ Start Simulation");
        startButton.setBackground(new Color(100, 200, 100));
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        inputPanel.add(startButton);

        resetButton = new JButton("🔄 Reset");
        resetButton.setBackground(new Color(255, 150, 150));
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        inputPanel.add(resetButton);
        add(inputPanel, BorderLayout.NORTH);

        
        centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.setBackground(new Color(230, 240, 255));
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(centerPanel, BorderLayout.CENTER);

        buildDynamicPanels(5, 3); 

        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(new TitledBorder("📜 Simulation Log"));
        add(scrollPane, BorderLayout.SOUTH);

        
        PrintStream originalOut = System.out;
        PrintStream ps = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char) b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
                originalOut.print((char) b);
            }
        }, true);
        System.setOut(ps);
        System.setErr(ps);

        
        startButton.addActionListener(e -> startSimulation());
        resetButton.addActionListener(e -> resetSimulation());
    }

   
    public void buildDynamicPanels(int queueCount, int pumpCount) {
        if (centerPanel != null) centerPanel.removeAll();
        pumpLabels.clear();
        queueLabels.clear();

       
        queuePanel = new JPanel(new GridLayout(Math.max(queueCount, 3), 1, 5, 5));
        queuePanel.setBorder(new TitledBorder("🚙 Waiting Area (Queue)"));
        for (int i = 0; i < Math.max(queueCount, 3); i++) {
            JLabel label = new JLabel("Empty", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(new Color(255, 255, 180));
            label.setBorder(new LineBorder(Color.DARK_GRAY, 1));
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setPreferredSize(new Dimension(400, 40));
            queuePanel.add(label);
            queueLabels.add(label);
        }

        
        pumpsPanel = new JPanel(new GridLayout(Math.max(pumpCount, 3), 1, 5, 5));
        pumpsPanel.setBorder(new TitledBorder("⛽ Pumps (Service Bays)"));
        for (int i = 0; i < Math.max(pumpCount, 3); i++) {
            JLabel label = new JLabel("Pump " + (i + 1) + " - Free", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(new Color(180, 255, 180));
            label.setBorder(new LineBorder(Color.GRAY, 1));
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setPreferredSize(new Dimension(400, 40));
            pumpsPanel.add(label);
            pumpLabels.add(label);
        }

        centerPanel.add(queuePanel);
        centerPanel.add(pumpsPanel);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void startSimulation() {
        try {
            int capacity = Integer.parseInt(capacityField.getText().trim());
            int pumpsCount = Integer.parseInt(pumpsField.getText().trim());
            String[] cars = carsField.getText().trim().replace(",", " ").split("\\s+");
            logArea.append("Starting simulation with " + capacity + " capacity, " + pumpsCount + " pumps, and " + cars.length + " cars...\n");

            new Thread(() -> {
                try {
                    ServiceStation.mainWithGUI(capacity, pumpsCount, cars, this);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            startButton.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! Please enter correct values.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetSimulation() {
        logArea.setText("");
        buildDynamicPanels(5, 3);
        startButton.setEnabled(true);
        logArea.append("Simulation reset. Ready for new input!\n");
    }

    public void enableStartAgain() {
        SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
    }

    public void updatePump(int index, String text, boolean busy) {
        if (index < pumpLabels.size()) {
            JLabel label = pumpLabels.get(index);
            SwingUtilities.invokeLater(() -> {
                label.setText("Pump " + (index + 1) + " - " + text);
                label.setBackground(busy ? new Color(255, 120, 120) : new Color(180, 255, 180));
            });
        }
    }

    public void updateQueue(List<String> queue) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < queueLabels.size(); i++) {
                JLabel label = queueLabels.get(i);
                if (i < queue.size()) {
                    label.setText(queue.get(i));
                    label.setBackground(new Color(255, 200, 120));
                } else {
                    label.setText("Empty");
                    label.setBackground(new Color(255, 255, 180));
                }
            }
        });
    }

    public static CarWashGUI getInstance() {
        return instance;
    }

    public static boolean exists() {
        return instance != null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CarWashGUI().setVisible(true));
    }
}

