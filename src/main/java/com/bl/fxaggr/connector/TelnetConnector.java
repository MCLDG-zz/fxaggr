package com.bl.fxaggr.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;

import com.bl.fxaggr.util.KafkaPublisher;

/***
 * This is a simple example of use of TelnetClient.
 ***/
public class TelnetConnector {
    static TelnetClient tc = null;

    /***
     * Main for the TelnetClientExample.
     ***/
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: TelnetClientExample1 <remote-ip> [<remote-port>]");
            System.exit(1);
        }

        String remoteip = args[0];

        int remoteport;

        if (args.length > 1) {
            remoteport = (new Integer(args[1])).intValue();
        }
        else {
            remoteport = 23;
        }

        tc = new TelnetClient();

        while (true) {
            boolean end_loop = false;
            try {
                tc.connect(remoteip, remoteport);

                System.out.println("TelnetConnector connecting to IP and Port:" + remoteip + " " + remoteport);
                OutputStream outstr = tc.getOutputStream();
                //Enter a blank username and password
                outstr.write("\r\n".getBytes());
                outstr.write("\r\n".getBytes());
                outstr.flush();

                byte[] buff = new byte[1024];
                int ret_read = 1;

                InputStream instr = tc.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(instr));
                KafkaPublisher publisher = new KafkaPublisher();
                StringBuffer sbuf = new StringBuffer();

                try {
                    do {
                        String s = br.readLine();
                        if (s.contains("Login") || s.contains("Password")) {
                            System.out.println("TelnetConnector read Login or Password. Auto sending CR/LF" + s);
                            continue;
                        }
                        //Publish quote as string
                        publisher.publish(s);
                        
                        //Publish quote as object
                        PriceQuote pq = new PriceQuote();
                        String[] quote = s.split(" ");
                        pq.setSymbol(quote[0]);
                        pq.setBid(quote[1]);
                        pq.setAsk(quote[2]);
                        //publisher.publish(pq);
                        
                    }
                    while (true);
                }
                catch (IOException e) {
                    System.err.println("TelnetConnector - Exception while reading socket:" + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    System.out.println("TelnetConnector tc.disconnect");
                    tc.disconnect();
                }
                catch (IOException e) {
                    System.err.println("TelnetConnector - Exception while closing telnet:" + e.getMessage());
                    e.printStackTrace();
                }
            }
            catch (IOException e) {
                System.err.println("Exception while connecting:" + e.getMessage());
                System.exit(1);
            }
        }
    }
}
