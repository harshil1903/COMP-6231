package com.server;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server {

    private static int clientId;

    private final ArrayList<ClientHandler> clientList;

    private static final int portNumber = 7777;

    private ServerSocket serverSocket;

    //Constructor
    Server()
    {
        clientList = new ArrayList<>();
    }


    //Initiate Socket Server Creation
    public void start()
    {
        try
        {
            serverSocket = new ServerSocket(portNumber);

            System.out.println("Server is now active on port " + portNumber);

            //Keep on creating new Socket for each new Client request made
            while (true)
            {
                Socket socket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(socket);

                clientList.add(clientHandler);

                clientHandler.start();

            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    //To stop the server
    public void stop()
    {
        //Stop the server
        try
        {
            serverSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("SERVER is now closing due to no clients remaining in the room");

        System.exit(0);
    }


    //To broadcast message to all the active user in chat room
    private synchronized void broadcastMessage(String message, String username)
    {
        for(int i = clientList.size() ; i > 0 ; )
        {
            --i;
            ClientHandler clientHandler = clientList.get(i);

            if(clientHandler.getUsername().equals(username))
            {
                continue;
            }

            clientHandler.writeMessageToClient(message);
//            if(!clientHandler.writeMessageToClient(message))
//            {
//                clientList.remove(i);
//                System.out.println(clientHandler.username + " removed from the Client List");
//            }
        }
    }


    //To remove client from Client List by matching ID
    private synchronized void remove(int id) {

        String removedClient = "";

        for(int i = 0; i < clientList.size(); ++i)
        {
            ClientHandler clientHandler = clientList.get(i);

            if(clientHandler.id == id)
            {
                removedClient = clientHandler.getUsername();
                clientList.remove(i);
                break;
            }
        }
        broadcastMessage(removedClient + " has left the chat room.", removedClient);


        //Ask the server to stop once all clients leave the chat room
        if(clientList.isEmpty())
        {
            stop();
        }
    }


    public static void main(String[] args)
    {

        Server server = new Server();

        server.start();

    }



    // One instance of this thread will run for each client
    class ClientHandler extends Thread {

        Socket socket;
        DataInputStream dataIn;
        DataOutputStream dataOut;

        int id;
        String username;
        String message;


        // Constructor
        ClientHandler(Socket socket)
        {

            id = ++clientId;
            this.socket = socket;

            try
            {
                dataOut = new DataOutputStream(this.socket.getOutputStream());
                dataIn = new DataInputStream(this.socket.getInputStream());
                // read the username
                username = dataIn.readUTF();
                broadcastMessage(username + " has joined the chat room.", username);
                System.out.println(username + " has joined the chat room.");
            }
            catch (IOException e)
            {
                System.out.println("Exception creating new Input/output Streams: " + e);
            }

        }

        public String getUsername()
        {
            return username;
        }

        // infinite loop to read and forward message
        public void run()
        {
            // to loop until client wants to LEAVE
            boolean endChat = false;
            while (!endChat)
            {
                try
                {
                    message = dataIn.readUTF();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    endChat = true;
                }

                System.out.println(username + " : " + message );

                // Quit Loop if Client wants to LEAVE
                if(message.equals("LEAVE"))
                {
                    System.out.println(username + " has left the room");
                    endChat = true;
                }
                else if(message.equals("ACTIVE"))
                {
                    //Print Active Members
                    writeMessageToClient("\n> Active Members ");
                    for (ClientHandler clientHandler : clientList)
                    {
                        writeMessageToClient(clientHandler.username);
                    }

                }
                else
                {
                    broadcastMessage(username + ": " + message, username);
                }

            }

            //Only reaches here when client wants to LEAVE
            remove(id);
            close();

        }

        // To Disconnect
        private void close()
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

        // write a message to the Client output stream
        private void writeMessageToClient(String messageToClient)
        {

            // write the message to the  client's stream
            try
            {
                dataOut.writeUTF(messageToClient);
            }
            catch (IOException e)
            {
                System.out.println("Error sending message to " + username);
            }
            //return true;

        }
    }

}

