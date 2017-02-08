package org.athento.nuxeo.easyshare.ejb;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * EasyShare action bean.
 */
@Name("easyShareAction")
@Scope(ScopeType.CONVERSATION)
public class EasyShareActionBean implements EasyShareAction, Serializable {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(EasyShareActionBean.class);

    /** Email address. */
    private String emailAddress;

    /** Current document. */
    @In(required = true)
    protected DocumentModel currentDocument;

    /** Faces messages. */
    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    /** i18n bundles. */
    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    /**
     * Send the public URL by email.
     */
    @Override
    public void sendPublicURL() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Sending EasyShare public URL to " + emailAddress);
        }
        EmailHelper emailHelper = new EmailHelper();
        // Prepare parameters
        Map<String, Object> params = new HashMap<>();
        String fromEmail = (String) currentDocument.getPropertyValue("eshare:contactEmail");
        if (fromEmail == null) {
            fromEmail = Framework.getProperty("mail.from", "noreply@athento.com");
        }
        // Get current mail.from
        String currentMailFrom = Framework.getProperty("email.from");
        Framework.getProperties().put("email.from", fromEmail);
        params.put("baseURL", Framework.getProperty("nuxeo.url"));
        params.put("docId", currentDocument.getId());
        params.put("docTitle", currentDocument.getTitle());
        params.put("mail.to", emailAddress);
        params.put("subject", "EasyShare folder shared");
        params.put("template", "sendEasyShareURL");
        try {
            emailHelper.sendmail(params);
            facesMessages.add(
                    INFO,
                    resourcesAccessor.getMessages().get(
                            "label.sendemail.sent"));
        } catch (Exception e) {
            e.printStackTrace();
            facesMessages.add(
                    INFO,
                    resourcesAccessor.getMessages().get(
                            "label.sendemail.notsent"));
        } finally {
            if (currentMailFrom != null) {
                Framework.getProperties().put("email.from", currentMailFrom);
            }
        }
    }

    /**
     * Get email address.
     *
     * @return
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Set email address.
     *
     * @param emailAddress
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
