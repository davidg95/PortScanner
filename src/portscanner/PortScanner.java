/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portscanner;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 *
 * @author David
 */
public class PortScanner {

    public String IP;
    public int LOOPS = 65535;
    public int TIMEOUT_VALUE;

    private long startTime;
    private long finnishTime;
    private long duration;

    public static int portsChecked = 0;

    private List<String> ports;
    private List<String> possiblePorts;

    private Semaphore sem;
    private Semaphore semPoss;

    public static GUI g;
    public static boolean run;
    public static boolean save;

    public static boolean gui = false;
    public static boolean basic_output = false;

    /**
     * Main method which takes in the arguments and creates a new ServerSniffer
     * object.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                if (!GraphicsEnvironment.isHeadless()) {
                    gui = true;
                    g = new GUI();
                    g.setVisible(true);
                } else {
                    System.out.println("Usage- [IP] [TIMEOUT] -b");
                    System.out.println("IP - The IP address to check.");
                    System.out.println("TIMEOUT - OPTIONAL. The time out value to wait for a reply from each connectin. Default value 500. Integer.");
                    System.out.println("-b - OPTIONAL. Only outputs ports and nothing else.");
                }
            } else {
                if (args[0].equals("?")) {
                    System.out.println("Usage- [IP] [TIMEOUT] -b");
                    System.out.println("IP - The IP address to check.");
                    System.out.println("TIMEOUT - OPTIONAL. The time out value to wait for a reply from each connectin. Default value 500. Integer.");
                    System.out.println("-b - OPTIONAL. Only outputs ports and nothing else.");
                } else if (args.length < 1 || args.length > 3) { //Check the arguments and generate a usage message if an invalid  number of arguments were entered.
                    throw new IllegalArgumentException("Usage- [IP] [TIMEOUT]");
                } else {
                    String IP = args[0];

                    int TIMEOUT_VALUE = 500;
                    if (args.length == 2) {
                        TIMEOUT_VALUE = Integer.parseInt(args[1]);
                    }

                    if (args.length == 3) { //Check if the basic output flag was passsed in.
                        if (args[3].equals("-b")) {
                            basic_output = true;
                        } else {
                            throw new IllegalArgumentException("Usage- [IP] [TIMEOUT] -b");
                        }
                    }

                    if (TIMEOUT_VALUE < 0) {
                        throw new IllegalArgumentException("Usage- [IP] [TIMEOUT]");
                    }

                    new PortScanner(IP, TIMEOUT_VALUE).start(); //Start running the scanner.
                }
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Blank constructor to initialise the sniffer.
     *
     * @param IP The IP Address to scan on
     * @param TIMEOUT_VALUE the timeout values for each connection
     */
    public PortScanner(String IP, int TIMEOUT_VALUE) {
        this.IP = IP;
        this.TIMEOUT_VALUE = TIMEOUT_VALUE;
    }

    /**
     * Method which handles the main logic.
     */
    public void start() {
        ports = new ArrayList<>();
        possiblePorts = new ArrayList<>();
        portsChecked = 0;
        run = true;

        sem = new Semaphore(1);
        semPoss = new Semaphore(1);

        if (!basic_output) {
            System.out.println("Checking " + LOOPS + " ports on IP address " + IP + " with a timeout value of " + TIMEOUT_VALUE + "ms");
        }

        if (gui) {
            g.log("Checking " + LOOPS + " ports on IP address " + IP + " with a timeout value of " + TIMEOUT_VALUE + "ms");
        }

        startTime = Calendar.getInstance().getTimeInMillis();

        try {
            for (int i = 1; i <= LOOPS; i++) {
                new ConnectionThread(IP, i, LOOPS, TIMEOUT_VALUE, ports, possiblePorts, sem, semPoss).start(); //Create the threads
                if (!run) {
                    break;
                }
            }

            while (portsChecked != LOOPS) { //Wait here until all threads are completed excecution.
                if (!run) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {

                }
            }
        } catch (OutOfMemoryError ex) {
            if (gui) {
                g.log("Out of memory");
                g.showDialog("There is not enough avaliable memory on your system to complete the scan.");
            }
        }
        
        finnishTime = Calendar.getInstance().getTimeInMillis();
        
        duration = finnishTime - startTime;

        if (gui) {
            g.complete();
            g.log("Scan complete in " + (duration / 1000) + "s!");
            if (ports.isEmpty() && possiblePorts.isEmpty()) {
                g.log("No servers found on IP address " + IP);
            } else {
                g.log(ports.size() + " server(s) and " + possiblePorts.size() + " possible server(s) found on IP address " + IP);
            }
        }
        
        if (!basic_output) {
            System.out.println("");
        }

        if (ports.isEmpty() && possiblePorts.isEmpty()) { //Check if any servers were found.
            if (!basic_output) {
                System.out.println("No servers found on IP address " + IP);
            }
        } else {
            if (!basic_output) {
                System.out.println(ports.size() + " server(s) and " + possiblePorts.size() + " possible server(s) found on IP address " + IP + " -");
            }
            if (!ports.isEmpty()) { //Check if any servers were found and display them.
                if (!basic_output) {
                    System.out.println("Servers-");
                }
                ports.forEach((port) -> {
                    System.out.println(port);
                });
                
                if (save) {
                    try {
                        File file = new File(IP + ".txt");
                        file.delete();
                        try (FileWriter writer = new FileWriter(file, true); PrintWriter out = new PrintWriter(writer)) {
                            ports.forEach((port) -> {
                                out.println(port);
                            });
                            possiblePorts.forEach((port) -> {
                                out.println(port);
                            });
                            out.flush();
                        }
                        g.log("Results saved to " + file.getAbsolutePath());
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {

                    }
                }
            }
            if (!possiblePorts.isEmpty()) { //Check if any possible servers were found and display them.
                if (!basic_output) {
                    System.out.println("\nPossible servers-");
                }
                possiblePorts.forEach((port) -> {
                    System.out.println(port);
                });
            }
            if (!basic_output) {
                System.out.println("\n" + (int) (ports.size() + possiblePorts.size()) + " servers found");
            }
        }
    }
}
