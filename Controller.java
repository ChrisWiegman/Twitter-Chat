/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Controller class for handling communications, etc
 */


package TwitterChat;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class Controller implements Runnable {

    //constants provided by Twitter
    private final String TWITTER_CONSUMER_KEY = "Consumer Key";
    private final String TWITTER_CONSUMER_SECRET = "Consumer Secret";
    //Access token for API calls
    private static AccessToken accessToken;
    //The authorization pin
    private static String authPin = null;
    //Twitter library for API calls
    private static Twitter twitter;
    //Pin interaction form
    private JFrame pinForm;
    //List of active chat windows
    private static List<String> openChats;
    private static List<DirectMessage> messages;

    /**
     * Constructor initializes twitter communications and lists for storing chats
     * and open windows
     */
    public Controller() {
        openChats = new ArrayList<String>(); //list of open chat windows
        twitter = new TwitterFactory().getInstance(); //connect back to twitter
        messages = new ArrayList<DirectMessage>(); //list of direct messages
    }

    /**
     * Authorize user with OAth and set up other variables
     */
    public void run() {

        //authorize with Twitter
        authorize();

        //create and start the friends list
        FriendsList fl = new FriendsList();
        Thread friendsList = new Thread(fl);
        friendsList.start();

        //wait until friendslist is loaded before continuing
        while (fl.getLoaded() == false) {
            try {
                Thread.sleep(500); //sleep for half second then try again.
            } catch (InterruptedException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //start polling for new direct messages
        Thread getDms = new Thread(new GetDms());
        getDms.start();
        
        //Start the system tray icon
        Thread stray = new Thread(new Stray()); 
        stray.start();
    }

    /**
     * Authorizes Twitter user returns true if successful
     * @return boolean
     */
    private boolean authorize() {

        //Request token to be sent to Twitter
        RequestToken requestToken;

        try {

            //set Twitter variables
            twitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);

            //get request token and set accesstoken to null
            requestToken = twitter.getOAuthRequestToken();
            accessToken = null;

            //get the url to call
            String authURL = requestToken.getAuthorizationURL(); //get the URL
            try {
                new OpenURL(authURL); //open the URL in a browser
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Dialog",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            //while we don't have a valid access token, keep trying to get one
            while (accessToken == null) {

                try {

                    if (authPin != null) { //if we have a pin try to get an access token
                        accessToken = twitter.getOAuthAccessToken(requestToken, authPin);
                        twitter.setOAuthAccessToken(accessToken);

                        return true;

                    } else { //no pin = get one from the user
                        if (pinForm == null) {
                            getPin(authURL);
                        } else { //if they've closed the pin form reopen it
                            pinForm.setVisible(true);
                        }
                    }

                } catch (TwitterException te) {
                    if (401 == te.getStatusCode()) { //twitter is down
                        JOptionPane.showMessageDialog(new JFrame(), "Unable to get the access token. Twitter is down", "Dialog",
                                JOptionPane.ERROR_MESSAGE);
                        pinForm = null;
                        authPin = null;
                    } else {
                        JOptionPane.showMessageDialog(new JFrame(), te.getErrorMessage(), "Dialog",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        } catch (TwitterException ex) { //twitter is down
            JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Dialog",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return false;
    }

    /**
     * Get the OAuth pin from the user
     */
    private void getPin(final String authURL) {

        //Form components
        JEditorPane authText;
        JButton submitButton;

        //Create a JFrame
        pinForm = new JFrame();
        pinForm.setSize(400, 260);
        pinForm.setLayout(new BorderLayout(5, 10));

        //Use HTML to tell the user what the form is all about
        String aText = "<center>"
                + "<h1>You must authenticate with Twitter</h1>"
                + "<p>Copy the PIN given to you from the Twitter Website and enter it below.</p>"
                + "<p>If the Twitter page doesn't open automatically <a href=\"" + authURL + "\">follow this link</a>."
                + "</center>";
        authText = new JEditorPane(new HTMLEditorKit().getContentType(), aText);
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; "
                + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) authText.getDocument()).getStyleSheet().addRule(bodyRule);
        authText.addHyperlinkListener(
                new HyperlinkListener() {
                    // if user clicked hyperlink, go to specified page

                    public void hyperlinkUpdate(HyperlinkEvent event) {
                        if (event.getEventType()
                                == HyperlinkEvent.EventType.ACTIVATED) {
                            try {
                                new OpenURL(authURL);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Error",
                                        JOptionPane.ERROR_MESSAGE);
                                System.exit(1);
                            }
                        }
                    } // end method hyperlinkUpdate
                } // end inner class
                );
        authText.setContentType("text/html");
        authText.setEditable(false);
        authText.setText(aText);

        //Set up the input field
        final JTextField pinEntry = new JTextField();
        pinEntry.addActionListener(
                new ActionListener() {

                    //close the pin window and set the pin
                    public void actionPerformed(ActionEvent event) {
                        setPin(pinEntry.getText());
                        pinForm.setVisible(false);

                    }
                });

        //Set up a submit button
        submitButton = new JButton();
        submitButton.setText("Authenticate");
        submitButton.addActionListener(new ActionListener() {

            //close the pin window and set the pin
            public void actionPerformed(ActionEvent event) {
                setPin(pinEntry.getText());
                pinForm.setVisible(false);
            }
        });

        //add components to the form and set visible
        pinForm.add(authText, BorderLayout.NORTH);
        pinForm.add(pinEntry, BorderLayout.CENTER);
        pinForm.add(submitButton, BorderLayout.SOUTH);
        pinForm.setVisible(true);

    }

    /**
     * Sets the pin number attained from the pin form
     * @param pin 
     */
    private void setPin(String pin) {
        authPin = pin;
    }

    /**
     * Determine if a user is logged in
     * @return boolean
     */
    private boolean hasAccess() {
        if (accessToken == null) { //if we have an access token we are logged in
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns list of user objects that can be direct messaged too
     * @return List<User>
     */
    public static List<User> getFriendsList() {
        try {
            //who we're followed by
            long[] followers = twitter.getFollowersIDs(-1).getIDs();

            //who we're following
            long[] friends = twitter.getFriendsIDs(-1).getIDs();

            //temp array of user ids to send to twitter
            long[] temp = new long[100];

            //list of users to return
            List<User> mutual = new ArrayList<User>();

            //keep track of temp array items
            int count = 0;

            for (int i = 0; i < friends.length; i++) {

                boolean found = false; //stop processing when we found a match

                for (int j = 0; j < followers.length && found == false; j++) {
                    if (friends[i] == followers[j]) {
                        found = true;
                        temp[count] = friends[i]; //add user to temp array 
                        count++;
                    }

                    //if array is full push to list and reset array and counter
                    if (count == 99) {
                        mutual.addAll(twitter.lookupUsers(temp));
                        count = 0;
                        Arrays.fill(temp, 0);
                    }
                }
            }

            //send any left in the array to the list
            mutual.addAll(twitter.lookupUsers(temp));

            //sort the list by twitter username
            Collections.sort(mutual, new UsernameComparator());

            return mutual;

        } catch (TwitterException ex) { //error with twitter
            JOptionPane.showMessageDialog(new JFrame(), ex.getErrorMessage(), "Dialog",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * subclass for sorting user list
     */
    private static class UsernameComparator implements Comparator {

        /**
         * Compares twitter username of 2 users
         * @param o1
         * @param o2
         * @return int
         */
        public int compare(Object o1, Object o2) {
            User u1 = (User) o1;
            User u2 = (User) o2;
            String s1 = u1.getScreenName().toLowerCase();
            String s2 = u2.getScreenName().toLowerCase();
            return s1.compareTo(s2);
        }
    }

    /**
     * Add chat window to openChats list
     * @param client 
     */
    public static synchronized void addClient(String client) {
        openChats.add(client);
    }

    /**
     * Remove chat window from list when closed
     * @param client 
     */
    public static synchronized void removeClient(String client) {
        openChats.remove(openChats.indexOf(client));
    }

    /**
     * Determine if a chat window has already been created for a given username
     * @param client
     * @return boolean
     */
    public static boolean checkClient(String client) {
        if (openChats.indexOf(client) == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Send direct message to given recipient
     * @param receiver
     * @param message 
     */
    public static void sendMessage(String receiver, String message) {
        try {
            twitter.sendDirectMessage(receiver, message);
        } catch (TwitterException ex) {
            JOptionPane.showMessageDialog(new JFrame(), ex.getErrorMessage(), "Dialog",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Set retrieved messages into list
     * @param messageList 
     */
    public static synchronized void setMessages(List<DirectMessage> messageList) {
        messages = messageList;
    }

    /**
     * Return list of direct messages sort true = descending, false = ascending
     * @param sort
     * @return List
     */
    public static synchronized List<DirectMessage> getMessages(boolean sort) {
        if (sort == false) {
            Collections.sort(messages, new MessageAscending());
        } else {
            Collections.sort(messages, new MessageDescending());
        }
        return messages;
    }

    /**
     * Return list of direct messages from a given username sort true = descending, false = ascending
     * @param sort
     * @param String
     * @return List
     */
    public static synchronized List<DirectMessage> getMessages(boolean sort, String client) {
        List<DirectMessage> clientMessages = new ArrayList<DirectMessage>();

        //parse through messages saving only those we need
        Iterator itr = messages.iterator();
        while (itr.hasNext()) {
            DirectMessage message = (DirectMessage) itr.next();
            if (message.getSenderScreenName().equals(client)) {
                clientMessages.add(message);
            }
        }
        if (sort == false) {
            Collections.sort(clientMessages, new MessageAscending());
        } else {
            Collections.sort(clientMessages, new MessageDescending());
        }
        return clientMessages;
    }

    /**
     * Compares dates to sort messages in ascending order
     */
    private static class MessageAscending implements Comparator {

        /**
         * Compares twitter username of 2 users
         * @param o1
         * @param o2
         * @return int
         */
        public int compare(Object o1, Object o2) {
            DirectMessage m1 = (DirectMessage) o1;
            DirectMessage m2 = (DirectMessage) o2;
            Date d1 = m1.getCreatedAt();
            Date d2 = m2.getCreatedAt();
            if (d1.after(d2)) {
                return 1;
            } else if (d1.equals(d2)) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    /**
     * Compares dates to sort messages in descending order
     */
    private static class MessageDescending implements Comparator {

        /**
         * Compares twitter username of 2 users
         * @param o1
         * @param o2
         * @return int
         */
        public int compare(Object o1, Object o2) {
            DirectMessage m1 = (DirectMessage) o1;
            DirectMessage m2 = (DirectMessage) o2;
            Date d1 = m1.getCreatedAt();
            Date d2 = m2.getCreatedAt();
            if (d1.after(d2)) {
                return -1;
            } else if (d1.equals(d2)) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * Returns raw directmessage list from Twitter for the getDMs class
     * @return 
     */
    public static List<DirectMessage> getRawMessages() {
        List<DirectMessage> rawMessages = new ArrayList<DirectMessage>();
        try {
            rawMessages = twitter.getDirectMessages();
        } catch (TwitterException ex) {
            JOptionPane.showMessageDialog(new JFrame(), ex.getErrorMessage(), "Dialog",
                    JOptionPane.ERROR_MESSAGE);
        }
        return rawMessages;
    }
}