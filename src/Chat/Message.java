package Chat;

import java.io.*;

public class Message implements Serializable {

    /**
     *  The different types of message sent by the Client
     *  WHOISIN to receive the list of the users connected
     *  MESSAGE an ordinary text message
     *  EXIT to disconnect from the Server
     *  TOPIC to receive the list of topic
     **/
    static final int WHOISIN = 0, MESSAGE = 1, EXIT = 2, TOPIC = 3;
    private int type;
    private String message;

    /*
    # \fn Message()
    # \brief...........: Constructor of the class
    */
    Message(int type, String message) {
        this.type = type;
        this.message = message;
    }

    public int getType()
    {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
