package org.athento.nuxeo.easyshare;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.runtime.api.Framework;

import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;

/**
 * AthEasyshare based on Nuxeo DM Easyshare.
 */
public abstract class AthEasyshareUnrestrictedRunner {

    protected final Log log = LogFactory.getLog(AthEasyshareUnrestrictedRunner.class);

    protected CoreSession session;

    public Object runUnrestricted(String docId) {
        try {
            IdRef docRef = new IdRef(docId);

            final LoginContext lc = Framework.login();

            CoreSession coreSession = null;
            RepositoryManager rm;
            try {
                rm = Framework.getLocalService(RepositoryManager.class);
                coreSession = rm.getDefaultRepository().open();

                // Run unrestricted operation
                return run(coreSession, docRef);

            } finally {
                final CoreSession session2close = coreSession;
                RequestContext.getActiveContext().addRequestCleanupHandler(
                        new RequestCleanupHandler() {

                            @Override
                            public void cleanup(HttpServletRequest req) {
                                try {
                                    Repository.close(session2close);
                                    lc.logout();
                                } catch (Exception e) {
                                    log.error(
                                            "Error during request context cleanup",
                                            e);
                                }
                            }
                        });

            }
        } catch (Exception ex) {
            log.error("Unable to render page", ex);
            return null;
        }

    }

    public abstract Object run(CoreSession coreSession, IdRef docId)
            throws ClientException;
}
