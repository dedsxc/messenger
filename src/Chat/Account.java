package Chat;

import java.io.*;

public class Account {
    private File accountFile = new File("./login.txt");

    public Account(){

    }

    /**
     * @fn createAccount()
     * @param username .....: Username of the client
     * @param password .....: Password of the client
     * @throws IOException .: Signals that an I/O exception of some sort has occurred.
     * \brief ..............: Create account <login>:<password> into the file accountFile
     */
    public void createAccount(String username, String password) throws IOException {
        FileWriter fileWriter = new FileWriter(accountFile, true);
        // 1. <login>:<password>
        fileWriter.write(username + ";" + password);
        fileWriter.write("\n");
        fileWriter.close();
    }

    /**
     * @fn authentification()
     * @param user_login ......: Username of the client to compare with the database "login.txt"
     * @param user_password ...: Password of the client to compare with the database "login.txt"
     * @return ................: If username and password match with the database, return true
     * @throws IOException ....: Signals that an I/O exception of some sort has occurred.
     */
    public boolean authentification(String user_login, String user_password) throws IOException{
        FileReader fileReader = new FileReader(accountFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        String[] parse;

        // 1. Read the file line per line.
        // 1.1 If log.file = user_log && pwd.file = user_pwd , return true
        while((line= bufferedReader.readLine()) != null){
            parse = line.split(";");
            if(parse[0].equals(user_login) && parse[1].equals(user_password)) {
                return true;
            }
        }
        return false;
    }
}
