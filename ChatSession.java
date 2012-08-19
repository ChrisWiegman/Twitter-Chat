/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Chat session class
 */


package TwitterChat;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import twitter4j.DirectMessage;

public class ChatSession extends JFrame implements Runnable {

    private JTextField enterField; // for entering messages
    private JTextArea displayArea; // for displaying messages
    private String partner; //name of the person you're talking to
    private Date startDate; //used as a timestamp for displaying messages
    private final int MAX_MSG_SIZE = 140; //max size of a single message
    private final int PREFIX_SIZE = 6; //number of in the prefix for multiple messages

    // set up GUI
    public ChatSession(final String name) {
        super(name);
        //assign the global variable
        partner = name;
        //removes this thread when the window is closed
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                Controller.removeClient(name);
            }
        });

        enterField = new JTextField("Type message here");
        enterField.addActionListener(
                //this action listener does several things
                //1) clears the JTextField after hitting ENTER
                //2) determines size of message
                //  a) if message is <= MAX_MSG_SIZE, nothing special happens, the message is just displayed
                //  b) if it's larger than MAX_MSG_SIZE, it's splitup & has a prefix put in front to show the order
                //3) the global timestamp is updated after the message is sent
                new ActionListener() {
                    
                    public void actionPerformed(ActionEvent event) {
                        try {
                            // get message from textfield 
                            String message = event.getActionCommand();
                            //the message can be sent in 1 time
                            if(message.length() <= MAX_MSG_SIZE){                            
                                displayArea.append("\nMe>>> " + message + "\n");
                                Controller.sendMessage(partner, message);
                                displayArea.setCaretPosition( displayArea.getText().length());
                            }
                            //message needs to be broken into smaller parts
                            else{
                                //determine number of initial "chunks"
                                int tCt = message.length()/MAX_MSG_SIZE;
                                //determine the number of "chunks" with the prefixes added
                                int msgCt = (message.length() + (tCt * 6)) / MAX_MSG_SIZE;
                                //determine if there is any overlap. if there is, increase the # of "chunks"
                                int rest = msgCt % MAX_MSG_SIZE;
                                if(rest != 0) msgCt++;
                                
                                //break up message & send the individual "chunks"
                                for(int i = 0; i < msgCt; i++){
                                    String chunk = "";
                                    //if it's not the last chunk of message
                                    if(i < msgCt - 1)        
                                        chunk = message.substring(i * (MAX_MSG_SIZE - PREFIX_SIZE), (i * (MAX_MSG_SIZE - PREFIX_SIZE)) + (MAX_MSG_SIZE - PREFIX_SIZE));
                                    //it is the last chunk of message
                                    else if(i == msgCt - 1)
                                        chunk = message.substring(i * (MAX_MSG_SIZE - PREFIX_SIZE), message.length());
                                    //create & append the prefix
                                    String pre = "(" + (i + 1) + "/" + msgCt + ") ";
                                    String msgShown = pre + chunk;
                                    //send & display the chunks
                                    displayArea.append("\nMe>>> " + msgShown + "\n");
                                    Controller.sendMessage(partner, msgShown);
                                    displayArea.setCaretPosition( displayArea.getText().length());
                                }
                            }
                            //update the timestamp & reset the textArea
                            startDate = new Date();
                            enterField.setText("");
                        } // end try
                        catch (Exception ioException) {
                            displayMessage(ioException + "\n");
                            ioException.printStackTrace();
                        } // end catch
                    } // end actionPerformed
                } // end inner class
                ); // end call to addActionListener

        add(enterField, BorderLayout.NORTH);

        displayArea = new JTextArea();
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        setSize(400, 300); // set window size
        setVisible(true); // show window
    } // end Client constructor

    public void run() {
        //start the timestamp start waitForMessages()
        startDate = new Date();
        waitForMessages();        
    }

    // wait for messages to arrive from Controller, display message contents
    //upon initial start, only display messages that are <= 3 hours old
    public void waitForMessages() {
        //current messages pulled from the Controller
        List<DirectMessage> message = null;
        //prior messages to check for updates
        List<DirectMessage> oldMessage = new ArrayList<DirectMessage>();
        //just a flag to determine initial startup
        boolean start = true;
        while (true) {
            // receive message and display contents if new
            try  {
                //get the messages from the person you're chatting with
                message = Controller.getMessages(false, partner);
                //if there really are messages
                if(message != null){
                    //if the messages are new
                    if(!message.equals(oldMessage)){
                        // display a single message
                        if(message.size() == 1){
                            //client has just started
                            if(start){
                                if((startDate.getTime() - (message.get(0).getCreatedAt()).getTime()) <= 10800000){
                                    displayMessage("\n" + partner + ">>> " + message.get(0).getText() + "\n"); 
                                }
                            }
                            else{
                                if((message.get(0).getCreatedAt()).after(startDate)){
                                    displayMessage("\n" + partner + ">>> " + message.get(0).getText() + "\n"); 
                                }
                            }
                        }
                        //there are multiple messages        
                        else if(message.size() > 1){
                            int numMsg = message.size();
                            //client has just started
                            if(start){
                                for(int i = 0; i < numMsg; i++){
                                    if((startDate.getTime() - (message.get(i).getCreatedAt()).getTime()) <= 10800000){
                                        displayMessage("\n" + partner + ">>> " + message.get(i).getText() + "\n");
                                    }
                                }
                            }
                            else{
                                for(int i = 0; i < numMsg; i++){
                                    if((message.get(i).getCreatedAt()).after(startDate)){
                                        displayMessage("\n" + partner + ">>> " + message.get(i).getText() + "\n"); 
                                    }
                                }
                            }
                        }
                        //clear oldMessage
                        if(oldMessage != null)
                            oldMessage.clear();
                        //make message into oldMessage
                        for(int i = 0; i < message.size(); i++){
                            oldMessage.add(message.get(i));
                        }
                        //clear message to make room for more
                        message.clear(); 
                        //turn the flag "off"
                        start = false;
                        //update the timestamp
                        startDate = new Date();
                        //if the chat window isn't "active" window, it'll be brought to the front
                        //which means the icon will start flashing
                        if(!isFocused()){
                            toFront();
                        }
                    }
                }
            } // end try
            catch (Exception exception) {
                displayMessage(exception + "\n");
                exception.printStackTrace();
            } // end catch
            try {
                Thread.sleep(500); //sleep .5 second then try again
            } catch (InterruptedException ex) {
                
            }
            
        } // end while
    } // end method waitForPackets

    // manipulates displayArea in the event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // updates displayArea
                    {
                        displayArea.append(messageToDisplay);
                    } // end method run
                } // end inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage
}  // end class Client
