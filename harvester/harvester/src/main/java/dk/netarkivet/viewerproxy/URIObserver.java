package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Observable;
import java.util.Observer;

/**
 * Super class for all URIObservers - calls the URIObserver notify method on
 * all notifications of a URI and its response code.
 *
 */
public abstract class URIObserver implements Observer {
    /**
     * This notify method is called on every notification of URI and response
     * code.
     *
     * @param uri The uri notified about
     * @param responseCode The response code of this uri.
     */
    public abstract void notify(URI uri, int responseCode);

    /** Helper class to be able to notify about a pair of <uri,responsecode>. */
    static final class URIResponseCodePair {
        /** The uri. */
        private final URI uri;
        /** The response code.*/
        private final int responseCode;

        /** initialise values.
         *
         * @param uri The URI
         * @param code The code
         */
        public URIResponseCodePair(URI uri, int code) {
            this.uri = uri;
            this.responseCode = code;
        }
    }

    /** Will call the abstract notify method if arg is an URIResponseCodePair
     * value.
     *
     * @param o The observable which called this method. Ignored.
     * @param arg The argument. If Response instance, notify is called.
     * Otherwise ignored.
     */
    public final void update(Observable o, Object arg) {
        if (arg != null && arg instanceof URIResponseCodePair) {
            URIResponseCodePair URIResponseCodePair = (URIResponseCodePair) arg;
            notify(URIResponseCodePair.uri, URIResponseCodePair.responseCode);
        }
    }
}