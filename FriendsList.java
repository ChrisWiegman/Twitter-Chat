/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Friends list class
 */


package TwitterChat;

import java.awt.Font;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import twitter4j.DirectMessage;
import twitter4j.User;

public class FriendsList implements Runnable {

    //Interface Items
    private static JFrame friendsList;
    private JMenuBar menubar;
    private ImageIcon icon;
    private JMenu file;
    private JMenuItem exitItem;
    private JMenu help;
    private JMenuItem helpItem;
    private JScrollPane fScroller;
    private JEditorPane fList;
    private List<User> friends;
    private String html = "";
    private List<DirectMessage> messages;
    private Date lastMessage;
    private boolean loaded;
    private final static java.net.URL IMAGE_URL = TwitterChat.class.getResource("icon.png");;
    private final Font DESKTOP_FONT = UIManager.getFont("Label.font");
    private final String BODY_CSS = "body { font-family: " + DESKTOP_FONT.getFamily() + "; "
                + "font-size: " + DESKTOP_FONT.getSize() + "pt; }";

    /**
     * Constructor initializes friends list
     */
    public FriendsList() {

        loaded = false; //it's currently not loaded

        lastMessage = new Date(Calendar.getInstance().getTimeInMillis());

        //create friends list
        friendsList = new JFrame();

        //menubar for exit and information
        menubar = new JMenuBar();
        icon = new ImageIcon(getClass().getResource("exit.png"));

        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);

        exitItem = new JMenuItem("Exit", icon);
        exitItem.setMnemonic(KeyEvent.VK_C);
        exitItem.setToolTipText("Exit application");
        exitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });

        file.add(exitItem);

        help = new JMenu("Help");

        helpItem = new JMenuItem("About");
        helpItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                aboutBox();
            }
        });

        help.add(helpItem);

        menubar.add(file);
        menubar.add(help);

        //temporary text until friends are loaded
        String loadText = "<br /><br /><br /><br /><h1>Loading Friends List</h1>";

        fList = new JEditorPane(new HTMLEditorKit().getContentType(), loadText);
        ((HTMLDocument) fList.getDocument()).getStyleSheet().addRule(BODY_CSS); //use default system font
        fList.setEditable(false);
        fList.setContentType("text/html");
        fList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fList.setText(loadText);
        fList.addHyperlinkListener(
                new HyperlinkListener() {
                    // if user clicked hyperlink, go to specified page

                    public void hyperlinkUpdate(HyperlinkEvent event) {
                        if (event.getEventType()
                                == HyperlinkEvent.EventType.ACTIVATED) {
                            if (Controller.checkClient(event.getDescription()) == false) {
                                //start a new chat session and register with controller
                                Thread ct = new Thread(new ChatSession(event.getDescription()));
                                ct.start();
                                Controller.addClient(event.getDescription());
                            }
                        }
                    } // end method hyperlinkUpdate
                } // end inner class
                );

        fScroller = new JScrollPane(fList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        friendsList.add(fScroller);

        friendsList.setJMenuBar(menubar);
        friendsList.setTitle(TwitterChat.getTitle() + " Friends List");
        friendsList.setSize(300, 800);
        friendsList.setLocation(10, 10);
        
        //disable shutdown unless user specifically requests exit from menu or system tray
        friendsList.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowIconified(java.awt.event.WindowEvent evt) {
                formWindowIconified(evt);
            }
            public void windowDeiconified(java.awt.event.WindowEvent evt) {
                formWindowDeiconified(evt);
            }
        });
    }

    /**
     * Displays the about box when selected
     */
    private void aboutBox() {
        //create and populate a new JFrame
        final JFrame aboutBox = new JFrame();
        aboutBox.setTitle("About " + TwitterChat.getTitle());
        aboutBox.setSize(400, 300);
        aboutBox.setLocation(-20, 50);
        aboutBox.setLocationRelativeTo(friendsList);

        String aText = "<center>"
                + "<h1>" + TwitterChat.getTitle() + "</h1>\n"
                + "Version " + TwitterChat.getVersion() + "\n"
                + "<hr />"
                + "<p>By:<br />Daniel Houle<br />and<br />ChrisWiegman</p>"
                + "<hr />"
                + "<p>Southern Illinois University Carbondale<br />CS412/591 - Summer 2011</p>"
                + "</center>";
        JEditorPane aboutText = new JEditorPane(new HTMLEditorKit().getContentType(), aText);
        ((HTMLDocument) aboutText.getDocument()).getStyleSheet().addRule(BODY_CSS); //use default system font
        aboutText.setContentType("text/html");
        aboutText.setEditable(false);
        aboutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        aboutText.setText(aText);

        aboutBox.add(aboutText);

        aboutBox.setVisible(true);
    }

    public void run() {

        friendsList.setVisible(true); //show the list when a thread is created

        //Set mac stuff - buggy in Lion
        System.setProperty("apple.laf.useScreenMenuBar", "true"); //if mac use the screen menu bar

        friends = Controller.getFriendsList(); //get the friends list

        //parse user object in friends list
        Iterator itr = friends.iterator();
        while (itr.hasNext()) {
            User temp = (User) itr.next();
            html = html + "<a href=\"" + temp.getScreenName() + "\"><img align=\"left\" border=\"0\" width=\"30\" height=\"30\" src=\"" + temp.getProfileImageURL() + "\">&nbsp;<strong>" + temp.getScreenName() + "</strong> (" + temp.getName() + ")</a><hr />";
        }

        //update display
        fList.setText(html);

        //set loaded flag
        loaded = true;

        boolean newRun = false;

        //check for new messages and open new session if necessary
        while (true) {
            boolean newMessages = true;
            messages = Controller.getMessages(newRun);
            newRun = true;
            Iterator mitr = messages.iterator();
            while (mitr.hasNext() && newMessages == true) {

                DirectMessage message = (DirectMessage) mitr.next();

                if (lastMessage.before(message.getCreatedAt())) {
                    String sender = message.getSenderScreenName();
                    lastMessage = message.getCreatedAt();
                    if (Controller.checkClient(sender) == false) {
                        //start a new chat session and register with controller
                        Thread ct = new Thread(new ChatSession(sender));
                        ct.start();
                        Controller.addClient(sender);

                    }
                } else {
                    newMessages = false;
                }

            }
            try {
                Thread.sleep(10000); //sleep 10 seconds
            } catch (InterruptedException ex) {
                Logger.getLogger(FriendsList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * return true if friends list has been loaded, false if not
     * @return boolean
     */
    public boolean getLoaded() {
        return loaded;
    }

    /**
     * Adjust visibility of friends list window
     * @param vis 
     */
    public static void setVisible(boolean vis) {
        if (vis == true) {
            friendsList.setVisible(true);
        } else {
            friendsList.setVisible(false);
        }
    }
    
    /**
     * Disable standard close buttons ad only hide window
     * @param evt 
     */
    private void formWindowIconified(java.awt.event.WindowEvent evt) {                                     
        if (SystemTray.isSupported()) { //if system tray is supported then minimize to system tray
            friendsList.setVisible(false);
        }
    }                                    

    /**
     * Disable standard close buttons ad only hide window
     * @param evt 
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {                                   
        if (SystemTray.isSupported()) { //if system tray is supported then close to system tray
            friendsList.setVisible(false);
        } else {
            System.exit(0);
        }
    }                                  

    /**
     * Disable standard close buttons ad only hide window
     * @param evt 
     */
    private void formWindowDeiconified(java.awt.event.WindowEvent evt) {                                       
        if (SystemTray.isSupported()) { //if system tray is supported then minimize to system tray
            friendsList.setVisible(false);
        }
    }
}
