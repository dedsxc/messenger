package Chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client{

    /*----------------------------------------------------------------------
                               VARIABLES CLIENT
    -----------------------------------------------------------------------*/
    private static String server_ip = "127.0.0.1";
    private static int server_port = 65001;
    protected String username;
    protected String password;
    protected ObjectInputStream objectInputStream;
    protected ObjectOutputStream objectOutputStream;
    protected Socket socketClient;

    /*----------------------------------------------------------------------
                        CLASS THREAD FOREACH CLIENT
    -----------------------------------------------------------------------*/
    public class ClientThread extends Thread {
        /**
         * @fn run()
         * \brief .......: Thread who only listen message from server
         */
        public void run()
        {
            boolean serverRunning = true;
            while(serverRunning)
            {
                try
                {
                    String msg = (String) objectInputStream.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException i)
                {
                    System.out.println("*** Server has closed the connection: " + i + " ***");
                    try{
                        disconnect();
                    }catch (IOException e){

                    }
                    serverRunning = false;
                }
                catch(ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    /*----------------------------------------------------------------------
                                  FUNCTIONS
    -----------------------------------------------------------------------*/

    /**
     * @fn Client()
     * brief .......: Constructor by default
     */
    Client(){

    }

    /**
     * @fn Client()
     * @param username ...: initialize username of the client
     * @param password ...: initialize password of the client
     * \brief ............: Constructor with parameters
     */
    Client(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    /**
     * @fn start()
     * @return ....: if all start correctly, return true
     * \brief .....: Start the client
     *               1. Connect to the server
     *               2. Initialize I/O Stream
     *               3. Authentification to the server (username / password)
     */
    public boolean start()
    {
        // 1. Try to connect to the server
        try
        {
            socketClient = new Socket(server_ip, server_port);
        }catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        System.out.println("Connection accepted : " + socketClient.getInetAddress() + ":" + socketClient.getPort());
        System.out.println("-------- AUTHENTIFICATION ---------");

        // 2. Create ObjectStream
        try
        {
            objectInputStream = new ObjectInputStream(socketClient.getInputStream());
            objectOutputStream = new ObjectOutputStream(socketClient.getOutputStream());
        }catch (IOException e)
        {
            System.out.println("Exception creating new I/O stream: " +e);
            return false;
        }

        // 3. Authentification
        Scanner scan = new Scanner(System.in);
        System.out.println("Do you have an account ? (y/n)");
        Scanner sc = new Scanner(System.in);
        String sc_response = sc.nextLine();

        if(sc_response.equals("n"))
        {
            // 1. Create account to new Client
            System.out.print("Enter new username: ");
            Scanner sc1 = new Scanner(System.in);
            username = sc1.nextLine();

            System.out.print("Enter new password: ");
            Scanner sc2 = new Scanner(System.in);
            password = sc2.nextLine();
        }
        else if(sc_response.equals("y"))
        {
            System.out.print("Enter username: ");
            Scanner sc3 = new Scanner(System.in);
            username = sc3.nextLine();

            System.out.print("Enter password: ");
            Scanner sc4 = new Scanner(System.in);
            password = sc4.nextLine();
        }

        // 3. send username to server
        try
        {
            objectOutputStream.writeObject(sc_response);
            objectOutputStream.writeObject(username);
            objectOutputStream.writeObject(password);
        }catch (IOException e)
        {
            System.out.println("Error sending username to server:" + e);
            try{
                disconnect();
            }catch (IOException i){
                i.printStackTrace();
            }
            return false;
        }

        // 4. Check if the account is authentified or not
        try
        {
            String msg_server = (String) objectInputStream.readObject();
            if(msg_server.equals("true"))
            {
                welcome_message();
                // 4.2 Create client for the client
                Client client = new Client(username, password);
                ClientThread t = new ClientThread();
                t.start();
            }
            else if(msg_server.equals("false"))
            {
                System.out.println("Wrong password or login :( bye");
                return false;
            }
        }catch(IOException e)
        {
            e.printStackTrace();
        }catch (ClassNotFoundException c)
        {
            c.printStackTrace();
        }
        // 5. if all success
        return true;
    }


    /**
     * @fn welcome_message()
     * \brief ...............: Welcome message printed when the client is authentified
     */
    public void welcome_message()
    {
        System.out.println("-------- YOUR ACCOUNT ---------");
        System.out.println("username > " +username);
        System.out.println("password > " +password);
        System.out.println("-------------------------------");
        System.out.println("Instructions:");
        System.out.println("1. Type your message to send broadcast to all active clients");
        System.out.println("2. Type '@username' without quotes to send private message");
        System.out.println("3. Type 'WHOISIN' without quotes to list of active clients");
        System.out.println("4. Type '#topic' without quotes to join a topic");
        System.out.println("5. Type 'TOPIC' without quotes to list active topics");
        System.out.println("6. Type 'EXIT' without quotes to logout from server");
        System.out.println("-------------------------------");
    }

    /**
     * @fn send_message()
     * @param message ....: Message to send to server
     * \brief ............: Send message to the server. If something goes wrong, close I/O streams, and close socket
     */
    public void send_message(Message message) throws IOException
    {
        objectOutputStream.writeObject(message);
    }

    /**
     * @fn disconnect()
     * \brief ..........: Disconnect to the server
     *                    1. close socket
     */
    public void disconnect() throws IOException{
        if(socketClient != null)
            socketClient.close();
    }

    /**
     * @fn command()
     * @param sc ......: Message typed by the user
     * @param client ..: use functions of the class Client
     * \brief .........: Interpretation of command type by user
     */
    public static void command(Scanner sc, Client client){
        while(true)
        {
            try {
                System.out.print("> ");
                String msg = sc.nextLine();
                if (msg.equals("EXIT")) {
                    client.send_message(new Message(2, ""));
                    break;
                } else if (msg.equals("WHOISIN")) {
                    client.send_message(new Message(0, ""));
                } else if (msg.equals("TOPIC")) {
                    client.send_message(new Message(3, ""));
                } else {
                    client.send_message(new Message(1, msg));
                }
            } catch (IOException e) {
                System.out.println("Exception writing to server: " + e);
                try{
                    client.disconnect();
                }catch (IOException i){
                    i.printStackTrace();
                }
                break;
            }
        }
    }
    /*----------------------------------------------------------------------
                                MAIN PROGRAM
    -----------------------------------------------------------------------*/
    /**
     * @fn Main()
     * @param args ..............: arguments not used
     * @throws IOException ......: Signals that an I/O exception has occurred
     * \brief ...................: Open a socket to connect on server_port. Once client is connected, the client
     *                             is redirected into a thread from the server
     */
    public static void main(String args[]) throws IOException{
        Scanner sc = new Scanner(System.in);

        Client client = new Client();

        if(client.start() == false)
        {
            sc.close();
            return;
        }

        command(sc, client);
        sc.close();
    }

}
