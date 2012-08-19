/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Class to retrieve direct messages from Twitter
 */

package TwitterChat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.DirectMessage;

public class GetDms implements Runnable {
    
    private List<DirectMessage> messages;
    
    /**
     * Initialize direct message worker with controller and twitter wrapper
     * @param controller
     * @param twit 
     */
    public GetDms() {
        messages = new ArrayList<DirectMessage>(); //list to store retrieved messages
    }
    
    /**
     * Start 10 second loop when thread is initialized
     */
    public void run() {
        
        while (true) {
                messages = Controller.getRawMessages(); //retrieve messages from twitter
                Controller.setMessages(messages); //save messages to controller
                try {
                    Thread.sleep(10000); //sleep 10 seconds then try again
                } catch (InterruptedException ex) {
                    Logger.getLogger(GetDms.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
    }
    
   
}
