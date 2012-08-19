/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Create and handle the system tray/notification icon
 */

package TwitterChat;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JOptionPane;

public class Stray implements Runnable {

    private TrayIcon trayIcon;
    private boolean vis = true; //helps determine default action
    private String TOOLTIP = TwitterChat.getTitle();
    private java.net.URL imageURL;
    private static PopupMenu menu;

    /**
     * Constructor sets up the tray icon
     *
     * @param gui
     */
    public Stray() {
        imageURL = TwitterChat.class.getResource("icon.png");
    }

    public void run() {
        if (SystemTray.isSupported()) { //only launch if supported

            MouseListener mouseListener = new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) { //show and hide the server window when left-click on the tray icon
                    if (vis == true) {
                        FriendsList.setVisible(false);
                        vis = false;
                    } else {
                        FriendsList.setVisible(true);
                        vis = true;
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) { //Not used
                }

                @Override
                public void mouseExited(MouseEvent e) {//Not used
                }

                @Override
                public void mousePressed(MouseEvent e) { //Not used
                }

                @Override
                public void mouseReleased(MouseEvent e) { //Not used
                }
            };

            SystemTray tray = SystemTray.getSystemTray();
            Image trayImage = Toolkit.getDefaultToolkit().getImage(imageURL);
            createMenu();
            trayIcon = new TrayIcon(trayImage, TOOLTIP, menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(mouseListener);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Error starting tray: " + e);
            }
        }
    }

    /**
     * creates the popup menu for the system tray
     */
    private void createMenu() {
        menu = new PopupMenu();
        MenuItem show = new MenuItem("Show Friendslist");
        MenuItem hide = new MenuItem("Hide Friendslist");
        MenuItem about = new MenuItem("About");
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(new exitListener());
        show.addActionListener(new showListener());
        hide.addActionListener(new hideListener());
        about.addActionListener(new aboutListener());
        menu.add(show);
        menu.add(hide);
        menu.addSeparator();
        menu.add(about);
        menu.add(exit);
    }

    /**
     * close the server from the system tray
     */
    class exitListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    /**
     * shows server window
     */
    class showListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vis == false) {
                FriendsList.setVisible(true);
                vis = true;
            }
        }
    }

    /**
     * Hides server window
     */
    class hideListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vis == true) {
                FriendsList.setVisible(false);
                vis = false;
            }
        }
    }

    /**
     * Shows about dialog
     */
    class aboutListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null,
                    TwitterChat.getTitle() +
                    "Version " + TwitterChat.getVersion() + "\n" +
                    "CS412/591 - Summer 2011", "About",
                    JOptionPane.PLAIN_MESSAGE);
        }
    }
}
