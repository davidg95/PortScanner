/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portscanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 *
 * @author David
 */
public class ConnectionThread extends Thread {

    private final String HOST;
    private final int PORT;
    private final int LOOPS;
    private final int TIMEOUT_VALUE;
    private final List<String> ports;
    private final List<String> possiblePorts;
    private final Semaphore sem;
    private final Semaphore semPoss;
    private final Socket s;

    /**
     * Constructor method for the thread.
     *
     * @param HOST the host which is being checked
     * @param PORT the port it is being checked on
     * @param LOOPS the number of ports getting checked.
     * @param TIMEOUT_VALUE the timeout value for the connection
     * @param ports the list for storing ports which have servers running.
     * @param possiblePorts the list for storing ports which have possible
     * servers, a port is added to the list of no reply was received but the
     * connection gets blocked
     * @param sem the semaphore to protect the servers list
     * @param semPoss the semaphore to protect the possible servers list
     */
    public ConnectionThread(String HOST, int PORT, int LOOPS, int TIMEOUT_VALUE, List<String> ports, List<String> possiblePorts, Semaphore sem, Semaphore semPoss) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.LOOPS = LOOPS;
        this.TIMEOUT_VALUE = TIMEOUT_VALUE;
        this.ports = ports;
        this.possiblePorts = possiblePorts;
        this.sem = sem;
        this.semPoss = semPoss;
        s = new Socket();
        this.setDaemon(true);
    }

    /**
     * Main thread logic.
     */
    @Override
    public void run() {
        try {
            s.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_VALUE); //Try make a connection

            //Flow of excecution only reaches this point if a connection was successful.
            sem.acquire();
            PortScanner.portsChecked++;
            if (PortScanner.gui) {
                if (PortScanner.portsChecked % (double) (LOOPS / 100) == 0) { //Check how many addresses have been scanend and add anohter segment to the progress bar.
                    PortScanner.g.bar(1);
                }
                PortScanner.g.addPort(PORT);
            }
            if (PortScanner.portsChecked % (double) (LOOPS / 20) == 0) { //Check how many addresses have been scanend and add anohter dot to the progress bar.
                System.out.print(".");
            }
            ports.add(Integer.toString(PORT));
            sem.release();

            s.close();
        } catch (IOException e) { //If full connection was not established.
            try {
                sem.acquire();
            } catch (InterruptedException ex) {
            }
            try {
                PortScanner.portsChecked++;
                sem.release();
                if (PortScanner.gui) {
                    if (PortScanner.portsChecked % (double) (LOOPS / 100) == 0) { //Check how many addresses have been scanend and add anohter segment to the progress bar.
                        PortScanner.g.bar(1);
                    }
                }
                if (PortScanner.portsChecked % (double) (LOOPS / 20) == 0) {
                    if (!PortScanner.basic_output) {
                        System.out.print(".");
                    }
                }
                if (e.getMessage().equals("Connection refused: connect")) { //Check if the connection was forcibly blocked.
                    try {
                        semPoss.acquire();
                    } catch (InterruptedException ex) {
                    }
                    if (PortScanner.gui) {
                        PortScanner.g.addPossiblePort(PORT);
                    }
                    possiblePorts.add(Integer.toString(PORT));
                    semPoss.release();
                }
            } catch (NullPointerException en) {
            }
        } catch (InterruptedException ex) {
        }
    }
}
