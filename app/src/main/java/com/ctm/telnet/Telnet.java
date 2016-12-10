/*********************************************************************
 *                         Version 3.0                               *
 * Copyright 2016 Steve D'Amico Licensed under the Apache License,   *
 * Version 2.0 (the "License"); you may not use this file except in  *
 * compliance with the License. You may obtain a copy of the License *
 * at http://www.apache.org/licenses/LICENSE-2.0                     *
 * Unless required by applicable law or agreed to in writing,        *
 * software distributed under the License is distributed on an "AS   *
 * IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either  *
 * express or implied. See the License for the specific language     *
 * governing permissions and limitations under the License.          *
 *                                                                   *
 * Created by Steve D'Amico and adapted in part from LightOnLightOff *
 * created by David Boesen. This version adds GPS reads and scrubs   *
 * version 2.                                                        *
 *********************************************************************/
package com.ctm.telnet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.apache.commons.net.telnet.TelnetClient;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
/*************************************
 * Main for the Telnet application.  *
 *************************************/
public class Telnet {
    private Telnet telnet = new Telnet();
    private InputStream in;
    private OutputStreamWriter out;
    public String prompt = "pi@raspberrypi ~ $";
    public String usrID = "raspberry";
    public String passwrd = "pi";
    static Telnet tc = null;
    static String ipAddr = null;
    /****************************************
     *            connect method            *
     * Construct that connects to the Pi by *
     * initiating a stream for each Pi      *
     * identified by its IP address. UserID *
     * and Password which are the same for  *
     * each PI where only the IP address is *
     * unique.                              *
     ****************************************/
    public void connect(String ipAddr) {
        try {
            // Connect to specified Pi
            telnet.connect(ipAddr, usrID, passwrd);
            // Get input and output stream references
            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());
            // Log the user in
            readUntil("login: ");
            write(usrID);
            readUntil("Password: ");
            write(passwrd);
            // Advance to a prompt
            readUntil(prompt + " ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /****************************************************
     * Method for reading and buffering characters from *
     * socket input stream.                             *
     ****************************************************/
    public String readUntil(String pattern) {
        try {
            char lastChar = pattern.charAt(pattern.length() - 1);
            StringBuffer sb = new StringBuffer();
            boolean found = false;
            char ch = (char) in.read();
            while (true) {
                System.out.print(ch);
                sb.append(ch);
                if (ch == lastChar) {
                    if (sb.toString().endsWith(pattern)) {
                        return sb.toString();
                    }
                }
                ch = (char) in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /*********************************************************
     * Method that writes a String to a socket output stream.*                             *
     *********************************************************/
    public void write(String value) {
        try {
            out.println(value);
            out.flush();
            System.out.println(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /******************************************************
     * Method sends command input to socket output stream.*                             *
     ******************************************************/
    public String sendCommand(String command) {
        try {
            write(command);
            return readUntil(prompt + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /******************************************************
     *                   disconnect method                *
     * Method ends session. Adding ipADDr for IP Address  *
     * results in compile error. As it stands, I believe  *
     * calling this method will terminate all sessions.   *                             *
     ******************************************************/
    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /******************************************************
     *                    led method                      *
     * led opens streams to Pi with IP Address and based  *
     * on the LED Color (ledClr) and state (ledState) "0" *
     * for off and "1" for on. Syntax is:                 *
     * led(String iPAddr, String ledClr, int ledState *
     * iPAddr -> ###.###.###.###                          *
     * ledClr -> red or grn or blu                        *
     * ledState -> '1' for on, '0' for off                *
     * Passes 'true' if completed, 'false' if fail        *
     ******************************************************/
    public void led(String ipAddr, String ledClr, int ledState) {
        // Configure message to Pi designating which and establishing state of
        // designated LED.
        String ledColor = " ";
        String retStr = " ";
        switch (ledClr) {
            case "red":
                ledColor = "echo \"17 = \" ledState \" \" > /dev/pi - blaster";
                break;
            case "grn":
                ledColor = "echo \"22 = \" ledState \" \" > /dev/pi - blaster";
                break;
            case "blu":
                ledColor = "echo \"24 = \" ledState \" \" > /dev/pi - blaster";
                break;
            // What is a good default for error condition?
            default:
        }
        // write requested LED color and state to Pi.
        if (prompt != null) {
            try {
                retStr = sendCommand(ledColor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }
    /***********************************************************
     *                    gps method                           *
     * gps request Pi with IP address for gps info (Lattitude  *
     * and Longitude). GPS Info is passed through Pi and       *
     * returned as coord.                                      *
     ***********************************************************/
    public Double[] gps(String ipAddr) {
        String GPScmd = "gpspipe -w -n10";
        String GPSdata = "";
        Double[] coord = new Double[2];
        char x;
        /*************************************************
         * Code to 'write' request for GPS info from Pi.   *
         * "lan": 345325643, "lon": 23456789,            *
         *************************************************/
        if (prompt != null) {
            try {
                GPSdata = sendCommand(GPScmd);
                String lat = "", lon = "";
                int plusIndex = GPSdata.indexOf("lat]\":");
                if (plusIndex != -1) {
                    lat = GPSdata.substring(plusIndex + 6);
                    int plusIndex2 = GPSdata.indexOf("lon]\":");
                    if (plusIndex2 != -1) {
                        lon = GPSdata.substring(plusIndex2 + 6);
                    }
                    lat = lat.split(",")[0];
                    lon = lon.split(",")[0];
                    coord[0] = Double.parseDouble(lat);
                    coord[1] = Double.parseDouble(lon);
                    // Use before
                    System.out.println(lat + "----" + lon);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return coord;
    }
    public static void main(String[] args) {
        try {
            Telnet telnet;
            telnet = new Telnet();
            telnet.connect("127.0.0.1", "pi", "raspberry");
            telnet.sendCommand("ps -ef");
            telnet.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

