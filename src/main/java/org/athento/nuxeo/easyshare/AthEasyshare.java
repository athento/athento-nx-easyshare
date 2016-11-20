package org.athento.nuxeo.easyshare;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Easyshare base on Nuxeo DM Easyshare.
 */
@Path("/easyshare")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "EasyShare")
public class AthEasyshare extends ModuleRoot {

    /** Log. */
    protected final Log log = LogFactory.getLog(AthEasyshare.class);

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("{folderId}")
    @GET
    public Object getFolderListing(@PathParam("folderId")
                                   String folderId) {
        return new AthEasyshareUnrestrictedRunner() {
            @Override
            public Object run(CoreSession session, IdRef docRef)
                    throws ClientException {
                if (session.exists(docRef)) {
                    DocumentModel docFolder = session.getDocument(docRef);

                    Date today = new Date();
                    if (today.after(docFolder.getProperty("dc:expired").getValue(
                            Date.class))) {
                        return getView("denied");
                    }

                    if (!docFolder.getType().equals("EasyShareFolder")) {
                        return Response.serverError().status(
                                Response.Status.NOT_FOUND).build();
                    }

                    DocumentModelList docList = session.getChildren(docRef);

                    // Audit Log
                    OperationContext ctx = new OperationContext(session);
                    ctx.setInput(docFolder);

                    // Audit.Log operation parameter setting
                    try {
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("event", "Access");
                        params.put("category", "Document");
                        params.put("comment",
                                "IP: " + request.getRemoteAddr());
                        AutomationService service = Framework.getLocalService(AutomationService.class);
                        service.run(ctx, "Audit.Log", params);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        return getView("denied");
                    }

                    return getView("folderList").arg("docFolder", docFolder).arg(
                            "docList", docList);
                } else {

                    return getView("denied");
                }
            }
        }.runUnrestricted(folderId);

    }

    /**
     * Get file name.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    public String getIconPath(DocumentModel doc) throws ClientException {
        return (String) new AthEasyshareUnrestrictedRunner() {
            @Override
            public Object run(CoreSession session, IdRef docRef)
                    throws ClientException {
                log.info("Go with " + docRef);
                DocumentModel doc = session.getDocument(docRef);
                BlobProperty blob = (BlobProperty) doc.getProperty("file:content");
                String iconPath = "/icons/file.png";
                if (blob != null) {
                    try {
                        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
                        MimetypeEntry mimeEntry = mimetypeRegistry.getMimetypeEntryByMimeType(((StorageBlob) blob.getValue()).getMimeType());
                        if (mimeEntry != null) {
                            if (mimeEntry.getIconPath() != null) {
                                iconPath = "/icons/" + mimeEntry.getIconPath();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Mimetype error for easyshare", e);
                    }
                }
                return iconPath;
            }
        }.runUnrestricted(doc.getId());
    }

    /**
     * Get file name.
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    public String getFileName(DocumentModel doc) throws ClientException {
        Blob blob = ((BlobHolder)doc.getAdapter(BlobHolder.class)).getBlob();
        return blob.getFilename();
    }

    @GET
    @Path("{folderId}/{fileId}/{fileName}")
    public Response getFileStream(@PathParam("fileId")
                                  String fileId) throws ClientException {

        return (Response) new AthEasyshareUnrestrictedRunner() {
            @Override
            public Object run(CoreSession session, IdRef docRef)
                    throws ClientException {
                if (session.exists(docRef)) {
                    try {
                        DocumentModel doc = session.getDocument(docRef);

                        Blob blob = doc.getAdapter(BlobHolder.class).getBlob();
                        DocumentModel docFolder = session.getDocument(doc.getParentRef());

                        // Audit Log
                        OperationContext ctx = new OperationContext(session);
                        ctx.setInput(doc);

                        Date today = new Date();
                        if (today.after(docFolder.getProperty("dc:expired").getValue(
                                Date.class))) {
                            return Response.serverError().status(
                                    Response.Status.NOT_FOUND).build();

                        }


                        // Audit.Log operation parameter setting
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("event", "Download");
                        params.put("category", "Document");
                        params.put("comment",
                                "IP: " + request.getRemoteAddr());
                        AutomationService service = Framework.getLocalService(AutomationService.class);
                        service.run(ctx, "Audit.Log", params);

                        if (doc.isProxy()) {
                            DocumentModel liveDoc = session.getSourceDocument(docRef);
                            ctx.setInput(liveDoc);
                            service.run(ctx, "Audit.Log", params);
                        }

                        //Email notification
                        String email = docFolder.getProperty("eshare:contactEmail").getValue(String.class);
                        String fileName = blob.getFilename();
                        String shareName = docFolder.getName();
                        try{
                            log.debug("Easyshare: starting email");
                            EmailHelper emailer = new EmailHelper();
                            Map<String, Object> mailProps = new Hashtable<String, Object>();
                            mailProps.put("mail.from", "system@nuxeo.com");
                            mailProps.put("mail.to", email);
                            mailProps.put("subject", "EasyShare Download Notification");
                            mailProps.put("body", "File "+fileName+" from "+shareName+" downloaded by "+request.getRemoteAddr());
                            mailProps.put("template", "easyShareEmail");
                            emailer.sendmail(mailProps);
                            log.debug("Easyshare: completed email");
                        } catch (Exception ex) {
                            log.error("Cannot send easyShare notification email", ex);
                        }


                        return Response.ok(blob.getStream(),
                                blob.getMimeType()).build();

                    } catch (Exception ex) {
                        log.error("error ", ex);
                        return Response.serverError().status(
                                Response.Status.NOT_FOUND).build();
                    }

                } else {
                    return Response.serverError().status(
                            Response.Status.NOT_FOUND).build();
                }
            }
        }.runUnrestricted(fileId);

    }

}
