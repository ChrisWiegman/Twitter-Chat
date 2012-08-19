/**
 * DC Twitter Chat
 * Dan Houle and Chris Wiegman
 * CS412 - Summer 2011
 * 
 * Class to handle URL's in the user's default browser
 */

package TwitterChat;

import java.net.URI;
import java.awt.Desktop;

public class OpenURL {

    /**
     * Constructor opens URL in default browser
     * @param URL 
     */
    public OpenURL(String URL) throws Exception {

        //if we're not on a supported OS just quit
        if (!Desktop.isDesktopSupported()) {
            throw new Exception("Unsupported Desktop. Can't open authentication site.");
        }

        //if thre is no url to open just quit
        if (URL.length() == 0) {
            throw new Exception("Invalid URL received");
        }

        //create a desktop opject
        Desktop desktop = Desktop.getDesktop();

        //another check for a supported os (not sure why this is needed)
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            throw new Exception("Unsupported Desktop. Can't open authentication site.");
        }

        //try to open the URL or throw an error message
        URI uri = new URI(URL);
        desktop.browse(uri);

    }
}
