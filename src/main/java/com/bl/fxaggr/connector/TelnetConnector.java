package com.bl.fxaggr.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.StringTokenizer;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;

/***
 * This is a simple example of use of TelnetClient.
 ***/
public class TelnetConnector implements Runnable
{
    static TelnetClient tc = null;

    /***
     * Main for the TelnetClientExample.
     ***/
    public static void main(String[] args) throws Exception
    {
        if(args.length < 1)
        {
            System.err.println("Usage: TelnetClientExample1 <remote-ip> [<remote-port>]");
            System.exit(1);
        }

        String remoteip = args[0];

        int remoteport;

        if (args.length > 1)
        {
            remoteport = (new Integer(args[1])).intValue();
        }
        else
        {
            remoteport = 23;
        }

        tc = new TelnetClient();

        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);

        try
        {
            tc.addOptionHandler(ttopt);
            tc.addOptionHandler(echoopt);
            tc.addOptionHandler(gaopt);
        }
        catch (InvalidTelnetOptionException e)
        {
            System.err.println("Error registering option handlers: " + e.getMessage());
        }

        while (true)
        {
            boolean end_loop = false;
            try
            {
                tc.connect(remoteip, remoteport);


                Thread reader = new Thread (new TelnetConnector());
                //tc.registerNotifHandler(new TelnetConnector());
                System.out.println("TelnetConnector");
                reader.start();
                OutputStream outstr = tc.getOutputStream();

                byte[] buff = new byte[1024];
                int ret_read = 0;

                do
                {
                    try
                    {
                        ret_read = System.in.read(buff);
                        if(ret_read > 0)
                        {
                            {
                                try
                                {
                                        outstr.write(buff, 0 , ret_read);
                                        outstr.flush();
                                }
                                catch (IOException e)
                                {
                                        end_loop = true;
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        System.err.println("Exception while reading keyboard:" + e.getMessage());
                        end_loop = true;
                    }
                }
                while((ret_read > 0) && (end_loop == false));

                try
                {
                    tc.disconnect();
                }
                catch (IOException e)
                {
                          System.err.println("Exception while connecting:" + e.getMessage());
                }
            }
            catch (IOException e)
            {
                    System.err.println("Exception while connecting:" + e.getMessage());
                    System.exit(1);
            }
        }
    }

    /***
     * Reader thread.
     * Reads lines from the TelnetClient and echoes them
     * on the screen.
     ***/
//    @Override
    public void run()
    {
        InputStream instr = tc.getInputStream();

        try
        {
            byte[] buff = new byte[1024];
            int ret_read = 0;

            do
            {
                ret_read = instr.read(buff);
                if(ret_read > 0)
                {
                    System.out.print(new String(buff, 0, ret_read));
                }
            }
            while (ret_read >= 0);
        }
        catch (IOException e)
        {
            System.err.println("Exception while reading socket:" + e.getMessage());
        }

        try
        {
            tc.disconnect();
        }
        catch (IOException e)
        {
            System.err.println("Exception while closing telnet:" + e.getMessage());
        }
    }
}
