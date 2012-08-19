/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Execution class
 */

package TwitterChat;

public class TwitterChat {

    //Program variables.
    private static final String APPLICATION_TITLE = "DC Twitter Chat";
    private static final String APPLICATION_VERSION = "1.0";

    /**
     * Execute the application. No arguments are necessary
     * @param args
     * @throws Exception 
     */
    public static void main(String args[]) throws Exception {

        //Start the controller
        Controller twitterController = new Controller();
        Thread controller = new Thread(twitterController);
        controller.start();

    }

    /**
     * Return application name
     * @return 
     */
    public static String getTitle() {
        return APPLICATION_TITLE;
    }

    /**
     * Return application version
     * @return 
     */
    public static String getVersion() {
        return APPLICATION_VERSION;
    }
}
