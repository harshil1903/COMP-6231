package com.client;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    private static final int serverPort = 7777;

    private final String username;

    private Socket socket;

    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    //Constructor
    Client(String username)
    {
        this.username = username;
    }


    //Initiate Socket connection to server
    public void start() throws IOException
    {
        try
        {
            InetAddress ip = InetAddress.getByName("localhost");
            socket = new Socket(ip, serverPort);
        }
        catch (Exception e)
        {
            System.out.println("Error Connecting to Server " +  e);
            e.printStackTrace();
        }

        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());

        new ReceiveMessagesFromServer().start();

        //Send Username for Server to keep record of Active Users
        dataOut.writeUTF(username);

    }


    //To Send Messages to server for broadcasting them to other users
    public boolean writeMessageToServer()
    {
        Scanner scanner = new Scanner(System.in);
        String message;

        //Run an infinite loop to get input from the user
        while (true)
        {
            System.out.print("$ ");
            message = scanner.nextLine();

            //Check whether user wants to leave the chatroom
            if(message.equals("LEAVE"))
            {
                try
                {
                    dataOut.writeUTF(message);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return true;
            }

            //Send Message to Server
            try
            {
                dataOut.writeUTF(message);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }

    }


    //To Disconnect
    public void disconnect()
    {
        try
        {
            dataOut.close();
            dataIn.close();
            socket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException
    {
        Scanner scanner = new Scanner(System.in);
        boolean endChat = false;
        String username;

        //If the user wants to provide username as Command Line Argument: java Client "username"
        if (args.length == 1)
        {
            username = args[0];
        }
        else
        {
            System.out.print("Enter the username : ");
            username = scanner.nextLine();
        }

        if(username.equals(""))
        {
            username = "Anonymous";
        }

        //Create a Client Object
        Client client = new Client(username);

        //Try connecting to the server
        client.start();

        System.out.println("\nWelcome to the chatroom.");
        System.out.println("* Type a message to chat with all active clients");
        System.out.println("* Type 'ACTIVE' to see list of active clients");
        System.out.println("* Type 'LEAVE' to leave the server");


        endChat = client.writeMessageToServer();

        scanner.close();

        client.disconnect();

        System.out.println("\nYou have left the room.");
        System.exit(0);

    }



    //Class to receive messages from Server actively
    class ReceiveMessagesFromServer extends Thread
    {
        public void run()
        {
            while (true)
            {
                try
                {
                    String messageFromServer = dataIn.readUTF();
                    System.out.println(messageFromServer);
                    System.out.print("$ ");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

            }
        }
    }


}

