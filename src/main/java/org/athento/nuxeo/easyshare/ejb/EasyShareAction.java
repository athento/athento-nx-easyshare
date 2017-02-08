package org.athento.nuxeo.easyshare.ejb;

import java.io.Serializable;

/**
 * Easyshare action.
 */
public interface EasyShareAction {

    /**
     * Send the public URL by email.
     */
    void sendPublicURL();

}
