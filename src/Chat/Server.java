package Chat;

/*----------------------------------------------------------------------
                        Importation de la librairie
-----------------------------------------------------------------------*/
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Server {

    /*----------------------------------------------------------------------
                               VARIABLES SERVER
    -----------------------------------------------------------------------*/
    private static String server_ip = "127.0.0.1";
    private static int server_port = 65001;
    private static int uniqueId;
    private SimpleDateFormat simpleDateFormat;
    private ArrayList<ClientHandler> arrayListClientHandler;
    private ArrayList<String> arrayListNameTopic;
    private boolean isRunning;

    /*----------------------------------------------------------------------
                        CLASS THREAD FOREACH CLIENT
    -----------------------------------------------------------------------*/

    /**
     * List of function in the class ClientHandler :
     * 1. ClientHandler(Socket socket)
     * 2. getTopic()
     * 3. getUsername()
     * 4. run()
     * 5. selectTypeMessage()
     * 6. writeMsg()
     * 7. close()
     */

    class ClientHandler extends Thread
    {
        /*----------------------------------------------------------------------
                               VARIABLES CLASS ClientHandler
        -----------------------------------------------------------------------*/
        Socket socket;
        Account account = new Account();
        ObjectOutputStream objectOutputStream;
        ObjectInputStream objectInputStream;
        int id;
        String username, password, date, topic_name;
        Message message;


        /**
         * @fn ClientHandler()
         * @param socket .......: Socket of the client
         * \brief ..............: Constructor of the class ClientHandler
         *                        1. Initialize I/O Stream
         *                        2. Get the current date when the Thread is launched
         */
        ClientHandler(Socket socket)
        {
            this.socket = socket;
            this.topic_name = new String("Home");
            id = ++uniqueId;
            date = new Date().toString() + "\n";
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream  = new ObjectInputStream(socket.getInputStream());
            }
            catch (IOException e)
            {
                display("Exception creating new I/O Streams: " + e);
                return;
            }
        }

        /**
         * @fn setTopic_name
         * @param topic_name ...: the topic who the client is connected
         */
        public void setTopic_name(String topic_name) {
            this.topic_name = topic_name;
        }


        /**
         * @fn getUsername()
         * @return .........: get the username of the Thread ClientHandler
         */
        public String getUsername()
        {
            return username;
        }

        /**
         * @fn run()
         * \brief .....: 1. Authentification of the client
         *               2. Listen for a message from client
         *               3. Treatment of the message from the client
         */
        public void run()
        {
            boolean isRunning = false;
            String response;

            // 1. Ask the client if he has an account
            try
            {
                response = (String) objectInputStream.readObject();
                username = (String) objectInputStream.readObject();
                password = (String) objectInputStream.readObject();
                if(response.equals("y"))
                {
                    if(account.authentification(username,password))
                    {
                        isRunning = true;
                    }
                    else
                    {
                        isRunning = false;
                    }
                }
                else if(response.equals("n"))
                {
                    account.createAccount(username, password);
                    isRunning = true;
                }
            }catch(IOException e)
            {
                e.printStackTrace();
            }catch(ClassNotFoundException c)
            {
                c.printStackTrace();
            }

            // 2. if the client has the right login, send "true" to client
            if(isRunning == true)
            {
                try
                {
                    objectOutputStream.writeObject("true");
                    broadcast("*** " + username + " has joined the chat room. ***", this);
                }catch (IOException i)
                {
                    i.printStackTrace();
                }
            }
            else if(isRunning == false)
            {
                try
                {
                    objectOutputStream.writeObject("false");
                }catch (IOException i)
                {
                    i.printStackTrace();
                }
            }

            // 3. if client is authentified, wait for message from client
            while(isRunning)
            {
                // 3.1 Receive the object message
                try
                {
                    message = (Message) objectInputStream.readObject();
                }catch (IOException e)
                {
                    display(username + " Exception reading Streams: " + e);
                    isRunning = false;
                    break;
                }catch (ClassNotFoundException c)
                {
                    c.printStackTrace();
                    break;
                }

                // 3.2 Convert the message from the Message object received to string
                String reception = message.getMessage();
                // 3.3 getType of message and execute the command
                selectTypeMessage(reception);
            }
            remove(id, this);
            close();
        }

        /**
         * @fn selectTypeMessage()
         * @param reception .......: message received from the client for treatment into broadcast() function
         * \brief .................: according to the form of the message (reception), send the correct function to the client
         */
        public void selectTypeMessage(String reception)
        {
            switch(message.getType())
            {
                case Message.WHOISIN:
                    writeMsg("List of the users connected at " + simpleDateFormat.format(new Date()) + "\n");
                    for(int i = 0; i < arrayListClientHandler.size(); ++i)
                    {
                        ClientHandler clientHandlerConnected = arrayListClientHandler.get(i);
                        writeMsg((i+1) + ". " + clientHandlerConnected.username + " since " + clientHandlerConnected.date);
                    }
                    break;
                case Message.MESSAGE:
                    if(broadcast(username + ": " + reception, this) == false)
                    {
                        String msg = "*** Sorry. user not found. ***";
                        writeMsg(msg);
                    }
                    break;
                case Message.EXIT:
                    display(username + " disconnected with command EXIT.");
                    isRunning = false;
                    break;
                case Message.TOPIC:
                    writeMsg("List of Topics");
                    for(int i = 0; i < arrayListNameTopic.size(); i++)
                    {
                        String topic = arrayListNameTopic.get(i);
                        writeMsg((i+1) + ". #" + topic);
                    }
                    break;
            }
        }

        /**
         * @fn writeMsg()
         * @param msg ....: Message to send
         * @return .......: return true if the message is sended
         * \brief ........: send message to a client
         */
        public boolean writeMsg(String msg)
        {
            // if Client is still connected send the message to it
            if(!socket.isConnected())
            {
                close();
                return false;
            }
            try
            {
                objectOutputStream.writeObject(msg);
            }
            catch(IOException e)
            {
                display("*** Error sending message to " + username + "***");
                display(e.toString());
            }
            return true;
        }

        /**
         * @fn close()
         * \brief .....:  close I/O stream , and the socket of the client who are connected
         */
        public void close()
        {
            try
            {
                if(objectOutputStream != null)
                    objectOutputStream.close();
            }
            catch(Exception e)
            {

            }
            try
            {
                if(objectInputStream != null)
                    objectInputStream.close();
            }
            catch(Exception e)
            {

            }
            try
            {
                if(socket != null)
                    socket.close();
            }
            catch (Exception e)
            {

            }
        }
    }



    public Server()
    {
        // display hh:mm:ss
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        arrayListClientHandler = new ArrayList<ClientHandler>();
        arrayListNameTopic = new ArrayList<String>();
    }

    /**
     * @fn start()
     * \brief .....: start the server
     */
    public void start()
    {
        isRunning = true;
        boolean isFound = false;
        try
        {
            // 1. Create socket for server
            ServerSocket serverSocket = new ServerSocket(server_port, 10, InetAddress.getByName(server_ip));

            // 2. infinite loop to wait for connections
            while(isRunning)
            {
                display("Server is listenning on " + server_ip +":" +server_port);
                Socket socket = serverSocket.accept();
                if(!isRunning)
                    break;

                // if client is connected, create it thread
                ClientHandler clientHandler = new ClientHandler(socket);

                //add this client to arraylist
                arrayListClientHandler.add(clientHandler);

                //add the topic in the arraylist if not exist
                for(String topic: arrayListNameTopic)
                {
                    if(topic.equals(clientHandler.topic_name))
                    {
                        isFound = true;
                        break;
                    }
                }
                if(isFound == false)
                {
                    arrayListNameTopic.add(clientHandler.topic_name);
                }

                // Start thread
                clientHandler.start();
            }

            // 3. if isRunning = false, first stop server then close thread/stream
            try
            {
                serverSocket.close();
                for(int i = 0; i < arrayListClientHandler.size(); ++i)
                {
                    ClientHandler tc = arrayListClientHandler.get(i);
                    try
                    {
                        // close all data streams and socket
                        tc.objectInputStream.close();
                        tc.objectOutputStream.close();
                        tc.socket.close();
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception z)
            {
                display("Exception closing the server and clients: " + z);
            }
        }
        catch (IOException x)
        {
            String msg = simpleDateFormat.format(new Date()) + " Exception on new ServerSocket: " + x + "\n";
            display(msg);
        }
    }

    /**
     * @fn stop()
     * \brief   : stop the server
     */
    public void stop()
    {
        isRunning = false;
        try
        {
            new Socket("localhost", server_port);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * \fn display
     * @param msg .: message to display
     * \brief .....: message to display with time
     */
    public void display(String msg)
    {
        String time = simpleDateFormat.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    /**
     * @fn broadcast
     * @param message .: message to display
     * @return ........: return true if true if the message is broadcasted
     *                   option : @ send message to specific user
     *                   option : # send message to a topic
     *                   option : "" send broadcasted message
     * \brief .........: depending of the option, send the type of message
     */
    public synchronized boolean broadcast(String message, ClientHandler clientHandler) {
        boolean isPrivate = false;
        boolean isTopic = false;
        String time = simpleDateFormat.format(new Date());

        /**
         * 2. check if message is private i.e. client to client message
         * If the first character is @, then its private
         * If the first character is #, then send to topic
         **/
        String[] forWho = message.split(" ",3);

        if(forWho[1].charAt(0)=='@')
            isPrivate = true;


        if(forWho[1].charAt(0)=='#')
            isTopic = true;

        // Private message
        if(isPrivate==true)
        {
            if(!privateMessage(forWho, message, time, clientHandler))
            {
                return false;
            }
        }
        // Create Topic and send message into the topic
        else if(isTopic==true)
        {
            topicMessage(forWho, message, time, clientHandler);
        }
        // Broadcast message
        else
        {
            broadcastMessage(message, time);
        }
        return true;
    }

    /**
     * @fn privateMessage()
     * @param forWho ........: Array of string to check if the first character is '@' or '#' else its a message
     * @param message .......: message to send
     * @param time ..........: get the current time
     * @return ..............: If the username of client is found, return true
     * \brief ...............: send a message to a specific user
     */
    public boolean privateMessage(String[] forWho, String message, String time, ClientHandler clientHandler)
    {
        /**
         * 1. Get username without @
         * forWho[0] = current username
         * forWho[1] = @username_to_join
         * forWho[2] = message
         */
        String usernameClient = forWho[1].substring(1, forWho[1].length());
        message = "[PRIVATE] " + time + " " + forWho[0] + " " + forWho[2] + "\n";

        boolean found=false;
        for(int i = 0; i < arrayListClientHandler.size(); i++)
        {
            clientHandler = arrayListClientHandler.get(i);
            String ifUserExist= clientHandler.getUsername();
            if(ifUserExist.equals(usernameClient))
            {
                clientHandler.writeMsg(message);
                found=true;
                break;
            }
        }
        if(found!=true)
        {
            return false;
        }
        return true;
    }

    /**
     * @fn topicMessage()
     * @param forWho ........: Array of string to check if the first character is '@' or '#' else its a message
     * @param message .......: message to send
     * @param clientHandler .: Update the attribute topic of the current Thread clientHandler
     * \brief ...............:
     */
    public void topicMessage(String[] forWho, String message, String time, ClientHandler clientHandler)
    {
        boolean isFound = false;
        String name_topic = forWho[1].substring(1, forWho[1].length());
        for(String topic: arrayListNameTopic)
        {
            if(topic.equals(name_topic))
            {
                isFound = true;
            }
        }
        if(isFound == false)
        {
            arrayListNameTopic.add(name_topic);
        }

        clientHandler.setTopic_name(name_topic);

        for(int i = 0; i < arrayListClientHandler.size(); i++)
        {
            ClientHandler clientHandlerList = arrayListClientHandler.get(i);
            String topic_client = clientHandlerList.topic_name;
            if(topic_client.equals(name_topic))
            {
                String message_time = "[" +name_topic + "] " + time + " " + message + "\n";
                clientHandlerList.writeMsg(message_time);
            }
        }
    }

    /**
     * @fn broadcastMessage()
     * @param message ........: Message to broadcast
     * @param time ...........: Add the current time to the message
     * \brief ................: Send a broadcast message to all active clients
     */
    public void broadcastMessage(String message, String time)
    {
        String message_broadcast = "[BROADCAST] " + time + " " + message + "\n";
        System.out.print(message_broadcast);
        for(int i = 0; i < arrayListClientHandler.size(); i++)
        {
            ClientHandler clientHandler = arrayListClientHandler.get(i);
            if(!clientHandler.writeMsg(message_broadcast))
            {
                arrayListClientHandler.remove(i);
                display("Disconnected Client " + clientHandler.username + " removed from list.");
            }
        }
    }


    /**
     * @fn remove()
     * @param id ...: id of client
     * \brief ......: remove the id of the client from server
     */
    public synchronized void remove(int id, ClientHandler clientHandler) {
        String disconnectedClient = "";
        // scan the array list until we found the Id
        for(int i = 0; i < arrayListClientHandler.size(); ++i) {
            ClientHandler clientHandlerList = arrayListClientHandler.get(i);
            // if found remove it
            if(clientHandlerList.id == id) {
                disconnectedClient = clientHandlerList.getUsername();
                arrayListClientHandler.remove(i);
                break;
            }
        }
        broadcast("*** " + disconnectedClient + " has left the chat room. ***", clientHandler);
    }


    /*----------------------------------------------------------------------
                                  MAIN PROGRAM
    -----------------------------------------------------------------------*/
    /**
     * @fn Main()
     * @param args .............: This parameter is not used
     * @throws IOException .....: Signals that an I/O exception has occurred
     * \brief ..................: Open a socket to listen on server_port. If a client is connected, the server
     *                            create a thread for the following client
     */
    public static void main(String args[]) throws IOException {
        Server server = new Server();
        server.start();
    }
}