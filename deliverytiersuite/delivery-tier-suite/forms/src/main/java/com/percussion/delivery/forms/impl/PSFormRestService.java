/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.forms.impl;


import com.percussion.delivery.forms.IPSFormRestService;
import com.percussion.delivery.forms.IPSFormService;
import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.data.PSFormSummaries;
import com.percussion.delivery.forms.data.PSFormSummary;
import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.delivery.utils.security.PSTlsSocketFactory;
import com.percussion.error.PSExceptionUtils;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.io.PathUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.InternalServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * REST/Webservice layer for form services.
 *
 * @author leonardohildt
 *
 */
@Path("/forms")
@Component
public class PSFormRestService extends PSAbstractRestService implements IPSFormRestService
{
    /**
     * The form service reference. Injected in the ctor. Never <code>null</code>
     * .
     */
    @Autowired
    private IPSFormService formService;
    private String enabledCiphers;

    /**
     * The form data joiner initialized by constructor. Used to merge forms data
     * and generate a CSV file. Never <code>null</code>.
     */
    private final PSFormDataJoiner formDataJoiner;

    /* Form field param constants */
    private static final String FORM_NAME_KEY = "perc_formName";

    private static final String FORM_ERROR_URL_KEY = "perc_errorUrl";

    private static final String FORM_HOST_REDIRECT = "perc_hostUrl";

    private static final String FORM_SUCCESS_URL_KEY = "perc_successUrl";

    private static final String FORM_URL_ENCRYPT_KEY = "perc_urlEncrypt";

    private static final String FORM_EMAIL_TO = "perc_emnt";

    private static final String EMAIL_FORM_TO = "perc_EmailFormTo";

    private static final String FORM_EMAIL_SUBJECT = "perc_emns";

    private  static final String FORM_PROCESSORURL = "perc_processorUrl";

    private  static final String FORM_PROCESSORTYPE = "perc_processorType";

    private static final String USER_AGENT = "Mozilla/5.0";

    /**
     * Logger for this class.
     */
    private  static final Logger log = LogManager.getLogger(PSFormRestService.class);


    public PSFormRestService(){ //NOOP
        formDataJoiner = new PSFormDataJoiner();
    }

    public PSFormRestService(IPSFormService service, String enabledCiphers)
    {
        formService = service;
        formDataJoiner = new PSFormDataJoiner();
        this.enabledCiphers = enabledCiphers;
    }


    @HEAD
    @Path("/csrf")
    public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response)  {
        Cookie[] cookies = request.getCookies();
        if(cookies == null){
            return;
        }
        for(Cookie cookie: cookies){
            if("XSRF-TOKEN".equals(cookie.getName())){
                response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
                response.setHeader("X-CSRF-TOKEN", cookie.getValue());
            }
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.IPSFormRestService#delete(java.lang.String)
     */
    @Override
    @DELETE
    @Path("/form/cms/{formName}")
    public void delete(@PathParam("formName") String formName)
    {
        try
        {
            formService.deleteExportedForms(formName);
        }
        catch (Exception e)
        {
            log.error("Exception occurred while deleting form, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.IPSFormRestService#create(javax.ws.rs.core.MultivaluedMap, java.lang.String, javax.ws.rs.core.HttpHeaders, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @Path("/form/collect")
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED,MediaType.APPLICATION_JSON})
    public void create(@Context ContainerRequest containerRequest, @FormParam("action") String action,
                       @Context HttpHeaders header, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse resp) throws WebApplicationException, IOException
    {


        log.debug("Http Header in the service is : {}", header.getRequestHeaders());

        Map<String, String[]> formFields = new HashMap<>();
        Map<String, String[]> percFields = new HashMap<>();

        boolean encryptExist = false;
        boolean isSpamBot = false;
        boolean isFormEmail = false;
        String errorRedirect = null;
        String hostRedirect = "";
        String successRedirect = null;
        Form form2 = (Form) containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY);
        MultivaluedMap<String, String> params = form2.asMap();
        try
        {
            for (Entry<String, List<String>> param : params.entrySet())
            {

                String[] sl = param.getValue().toArray(new String[0]);
                String key = param.getKey();

                // this form was submitted by a spambot and has the hidden field populated
                if(key.equals("topyenoh") && param.getValue().toString().length() > 2) {
                    isSpamBot = true;
                    log.debug("headers getRequestHeaders: {}", header.getRequestHeaders());
                    continue;
                }
                else if (key.equals("topyenoh")) {
                    continue;
                }

                // the data drop down field (email form) is present on the forms widget
                if (key.equals("emailForm")) {
                    isFormEmail = true;
                }

                if (key.startsWith("perc_"))
                {
                    percFields.put(key, sl);
                }
                else if (key.startsWith("perc-")) {
                    percFields.put(key,sl);
                }
                else
                {
                    formFields.put(key, sl);
                }
            }

            String formName;

            String[] encryptUrl = percFields.get(FORM_URL_ENCRYPT_KEY);
            if (encryptUrl != null && encryptUrl[0] != null && !encryptUrl[0].trim().isEmpty()
                    && encryptUrl[0].equalsIgnoreCase("true"))
            {
                encryptExist = true;
            }

            String[] errorUrl = percFields.get(FORM_ERROR_URL_KEY);
            if (errorUrl != null && errorUrl[0] != null && !errorUrl[0].trim().isEmpty())
            {
                errorRedirect = errorUrl[0];
            }

            String[] hostUrl = percFields.get(FORM_HOST_REDIRECT);
            if (hostUrl != null && hostUrl[0] != null && !hostUrl[0].trim().isEmpty())
            {
                hostRedirect = hostUrl[0];
            }

            String[] successUrl = percFields.get(FORM_SUCCESS_URL_KEY);
            if (successUrl != null && successUrl[0] != null && !successUrl[0].trim().isEmpty())
            {
                successRedirect = successUrl[0];
            }

            String[] formNameValues = percFields.get(FORM_NAME_KEY);
            if (formNameValues == null || formNameValues[0] == null || formNameValues[0].trim().isEmpty())
            {
                log.error("Supplied form missing {} field.", FORM_NAME_KEY);
                WebApplicationException webEx = new WebApplicationException(new IllegalArgumentException(
                        "Supplied form missing " + FORM_NAME_KEY + " field."), Response.serverError().build());
                handleError(header, resp, webEx, hostRedirect, errorRedirect, encryptExist);
                return;
            }
            formName = formNameValues[0];

            String emailNotifTo = null;
            String[] emailNotifToVals = null;
            if(!isFormEmail) {
                emailNotifToVals = percFields.get(FORM_EMAIL_TO);
            }
            else if(isFormEmail) {
                emailNotifToVals = percFields.get(EMAIL_FORM_TO);
            }

            if (emailNotifToVals != null && emailNotifToVals[0] != null && !emailNotifToVals[0].trim().isEmpty())
            {
                try {
                    emailNotifTo = PSEncryptor.decryptString(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),emailNotifToVals[0]);
                }catch(PSEncryptionException | java.lang.IllegalArgumentException e){
                    emailNotifTo = PSLegacyEncrypter.getInstance(
                            PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)

                    ).decrypt(emailNotifToVals[0],
                            PSLegacyEncrypter.getInstance(
                                    PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                            ).DEFAULT_KEY(),null);
                }
            }

            String emailNotifSubject = null;
            String[] emailNotifSubjectVals = percFields.get(FORM_EMAIL_SUBJECT);

            if(!isFormEmail) {
                emailNotifSubjectVals = percFields.get(FORM_EMAIL_SUBJECT);
            }
            else if(isFormEmail) {
                emailNotifSubjectVals = formFields.get("email-subject");
            }

            if (emailNotifSubjectVals != null && emailNotifSubjectVals[0] != null && !emailNotifSubjectVals[0].trim().isEmpty() && !isFormEmail)
            {
                try {
                    emailNotifSubject = PSEncryptor.decryptString(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),emailNotifSubjectVals[0]);
                }catch(PSEncryptionException | java.lang.IllegalArgumentException e){
                    emailNotifSubject = PSLegacyEncrypter.getInstance(
                            PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                    ).decrypt(emailNotifSubjectVals[0],
                            PSLegacyEncrypter.getInstance(
                                    PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                            ).DEFAULT_KEY(),null);
                }
            } else if(isFormEmail && (emailNotifSubjectVals != null)) {
                    emailNotifSubject = emailNotifSubjectVals[0];

            }

            //If it is an email form and key fields are null log the errors
            if(isFormEmail){

                if(emailNotifSubject==null)
                    log.error("Form  is configured as an email form but is missing email-subject field {}",formName);
                if(emailNotifToVals==null)
                    log.error("Form {} is configured as an email form but is missing perc_EmailFormTo field to send email to.", formName);

                if(emailNotifSubject==null || emailNotifToVals==null){
                    log.error("Skipping form email for this form as it is not configured correctly.");
                    isFormEmail = false;
                }
            }

            //No point in validating captcha if we already know it is spam.
            if(formService.getRecaptchaService().isCaptchaOn() && !isSpamBot){
                String[] captchaResponse = formFields.get(PSRecaptchaService.RECAPTCHA_RESPONSE);
                boolean missingCaptcha=false;

                if(captchaResponse == null){
                    missingCaptcha = true;

                    if(formName==null || formName.isEmpty())
                        formName="<not set>";

                    log.error("recaptcha.on=true in configured properties, but form post is missing reCaptcha response. Post will be treated as a bot, verify that reCaptcha field is present on form: {}",
                            formName);
                }
                if(!missingCaptcha && StringUtils.isNotEmpty(captchaResponse[0])){
                    isSpamBot = !formService.getRecaptchaService().verify(captchaResponse[0]);
                    formFields.remove(PSRecaptchaService.RECAPTCHA_RESPONSE);
                }else{
                    isSpamBot=true;
                }
            }


            if(isSpamBot) {
                log.error("Blocking post from {}, was detected as a SPAM bot.  Consider blocking this IP in firewall rules.", httpServletRequest.getRemoteAddr());
                WebApplicationException webEx = new WebApplicationException(new IllegalArgumentException(
                        "Post detected as a Bot.  Form submission rejected."), Response.serverError().build());
                handleError(header, resp, webEx, hostRedirect, errorRedirect, encryptExist);
                return;
            }

            String processorType = "LocalServer";
            if(percFields.get(FORM_PROCESSORTYPE)!= null)
                processorType = percFields.get(FORM_PROCESSORTYPE)[0];

            if((processorType != null) && !processorType.equalsIgnoreCase("LocalServer"))  {
                //Post form to remote URL
                String processorUrl = null;
                if(percFields.get(FORM_PROCESSORURL)!=null){
                    processorUrl = percFields.get(FORM_PROCESSORURL)[0];
                    Security.addProvider(new BouncyCastleProvider());
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, new SecureRandom());

                    Protocol.registerProtocol("https",
                            new Protocol("https", new PSTlsSocketFactory(this.enabledCiphers), 443));

                    HttpClient client = new HttpClient( );
                    PostMethod post = new PostMethod(processorUrl);
                    //Loop through form parameters and set up for re-posting
                    params.keySet().forEach(key -> {
                        for (String value : params.get(key)) {
                            post.addParameter(key, value);
                        }
                    });

                    boolean success=false;

                    // execute method and handle any error responses.
                    client.executeMethod( post );
                    String body = post.getResponseBodyAsString( );

                    log.debug("Response Body: {}", body);


                    if(post.getStatusCode() >=200 &&  post.getStatusCode()  <=399){
                        success = true;
                    }else{
                        log.error("Post to remote form service: {} failed with error code: {} and a response body of: {}",processorUrl, post.getStatusCode(), body);
                        log.error("Redirecting to error page...");
                    }

                    post.releaseConnection();
                    if(success) {
                        handleRedirect(successRedirect, encryptExist, hostRedirect, resp);
                    }else{
                        handleError(header, resp, null, hostRedirect, errorRedirect, encryptExist);
                    }


                }else{
                    log.error("{} was not specified but Form is configured to POST to a remote service.", FORM_PROCESSORURL);
                    WebApplicationException webEx = new WebApplicationException(new IllegalArgumentException(
                            "Invalid form submitted"), Response.serverError().build());
                    handleError(header, resp, webEx, hostRedirect, errorRedirect, encryptExist);
                }

            }else{
                IPSFormData form = formService.createFormData(formName, formFields);

                if(isFormEmail) {
                    try{
                        sendFormDataEmail(emailNotifTo, emailNotifSubject, form);
                    }catch(Exception e){
                        log.error("Error sending form email for form: {}, Error: {}", formName,e.getMessage());
                        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                    }

                    handleRedirect(successRedirect, encryptExist, hostRedirect, resp);
                    return;
                }

                try {
                    formService.save(form);
                } catch (IllegalArgumentException e) {
                    log.error("Exception occurred while saving a form, Error: {}", PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));

                    WebApplicationException webEx = new WebApplicationException(new IllegalArgumentException(
                            "Invalid form submitted"), Response.serverError().build());
                    handleError(header, resp, webEx, hostRedirect, errorRedirect, encryptExist);
                    return;
                }

                if (emailNotifTo != null && emailNotifSubject != null){
                    try{
                        sendFormDataEmail(emailNotifTo, emailNotifSubject, form);
                    }catch(Exception e){
                        log.error("An error occurred sending the form notification email to: {} for form {}, Error: {}",emailNotifTo, formName,e.getMessage() );
                        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                    }
                }

                handleRedirect(successRedirect, encryptExist, hostRedirect, resp);
            }
        }
        catch (Exception ex)
        {
            log.error("Exception occurred during form creation, Error: {}", ex.getMessage());
            log.debug(ex.getMessage(), ex);
            WebApplicationException webEx = new WebApplicationException(ex, Response.serverError().build());
            handleError(header, resp, webEx, hostRedirect, errorRedirect, encryptExist);
        }

    }

    /**
     * @param successRedirect The success redirect page to decrypt
     * @param encryptExist if the success page is encrypted
     * @param hostRedirect the host url for the prefix of the redirect
     * @param resp the servlet response
     */
    private void handleRedirect(String successRedirect, boolean encryptExist, String hostRedirect, @Context HttpServletResponse resp) throws IOException {
        if (successRedirect != null)
        {
                if(encryptExist) {
                    try {
                        successRedirect = PSEncryptor.decryptString(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),successRedirect);
                    } catch (PSEncryptionException e) {
                        successRedirect = PSLegacyEncrypter.getInstance(
                                PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                        ).decrypt(
                                successRedirect, PSLegacyEncrypter.getInstance(
                                        PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                                ).DEFAULT_KEY(),null);
                    }
                }

            successRedirect = successRedirect.startsWith("/") ? successRedirect : "/" + successRedirect;

                //Check if the success redirect url is absolute or not - if absolute - just redirect there.
            if(successRedirect.startsWith("http://") || successRedirect.startsWith("https://")){
                resp.sendRedirect(successRedirect);
            }else {
                //If it is relative pre-pernd the host
                resp.sendRedirect(hostRedirect + successRedirect);
            }
        }
        else
        {
            String pathSuccess = hostRedirect + "/perc-form-processor";
            resp.sendRedirect(pathSuccess + "/success.html");
        }
    }


    /**
     * Sends an email with the supplied form data
     *
     * @param emailNotifTo The to addresses, comma-delimited, assumed not <code>null<code/> or empty.
     * @param emailNotifSubject The subject, assumed not <code>null<code/> or empty
     * @param form
     */
    private void sendFormDataEmail(String emailNotifTo, String emailNotifSubject, IPSFormData form)
    {
        try
        {
            formService.emailFormData(emailNotifTo, emailNotifSubject, form);
        }
        catch (Exception e)
        {
            log.error("Cannot email form data, unexpected error, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.IPSFormRestService#get(java.lang.String)
     */
    @Override
    @GET
    @Path("/form/cms/{formName}")
    @Produces(
            {MediaType.APPLICATION_JSON})
    public PSFormSummaries get(@PathParam("formName") String formName)
    {
        if (!formService.isValidFormName(formName)) {
            log.error("Invalid formName.");
            throw new WebApplicationException(new IllegalArgumentException(
                    "Invalid formName"), Response.serverError().build());
        }
        try
        {
            List<String> formNames = new ArrayList<>();
            if (formName != null)
            {
                formNames.add(formName);
            }
            if (StringUtils.isBlank(formName))
            {
                formNames.addAll(formService.findDistinctFormNames());
            }

            PSFormSummaries formResult = new PSFormSummaries();
            for (String name : formNames)
            {
                long total = formService.getTotalFormCount(name);
                long exported = formService.getExportedFormCount(name);

                PSFormSummary formSummary = new PSFormSummary();
                formSummary.setName(name);
                formSummary.setTotalForms(total);
                formSummary.setExportedforms(exported);
                formResult.getSummaries().add(formSummary);
            }
            return formResult;
        }
        catch (Exception e)
        {
            log.error("Exception occurred while getting form summaries, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.IPSFormRestService#get()
     */
    @Override
    @GET
    @Path("/form/cms/list")
    @Produces(
            {MediaType.APPLICATION_JSON})
    public PSFormSummaries get()
    {
        try
        {
            List<String> formNames = new ArrayList<>(formService.findDistinctFormNames());

            PSFormSummaries formResult = new PSFormSummaries();
            for (String name : formNames)
            {
                long total = formService.getTotalFormCount(name);
                long exported = formService.getExportedFormCount(name);

                PSFormSummary formSummary = new PSFormSummary();
                formSummary.setName(name);
                formSummary.setTotalForms(total);
                formSummary.setExportedforms(exported);
                formResult.getSummaries().add(formSummary);
            }
            return formResult;
        }
        catch (Exception e)
        {
            log.error("Exception occurred while getting all form summaries, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.IPSFormRestService#export(java.lang.String, java.lang.String)
     */
    @Override
    @GET
    @Path("/form/cms/{formName}/{csvFile}")
    @Produces(
            {"text/csv"})
    public Response export(@PathParam("formName") String formName, @PathParam("csvFile") String csvFile)
    {
        if (!formService.isValidFormName(formName)) {
            log.error("Invalid formName.");
            throw new WebApplicationException(new IllegalArgumentException(
                    "Invalid formName"), Response.serverError().build());
        }
        try
        {
            List<IPSFormData> forms;
            forms = formService.findFormsByName(formName);

            if(log.isDebugEnabled()){
                log.debug("Forms by name({}) : {}", formName, forms);
            }
            

            formService.markAsExported(forms);

            ResponseBuilder response = Response.ok(formDataJoiner.generateCsv(forms));
            response.header("Content-Disposition", "attachment; filename=" + csvFile);
            return response.build();
        }
        catch (Exception e)
        {
            log.error("Exception occurred while exporting the form, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /**
     * Handle the exception by trying to redirect to the error page if one
     * exists, else go to default error page, else throw a ServletException if
     * all else fails.
     *
     * @param header
     * @param ex
     * @param redirect
     * @param isEncrypted
     * @throws WebApplicationException
     * @throws IOException
     */
    private void handleError(HttpHeaders header, @Context HttpServletResponse resp, WebApplicationException ex,
                             String hostRedirect, String redirect, boolean isEncrypted) throws WebApplicationException, IOException
    {
        String urlErrorPage = "";
        if (redirect != null)
        {
            if(isEncrypted) {
                try {
                    urlErrorPage = PSEncryptor.decryptString(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),redirect);
                } catch (PSEncryptionException e) {
                    urlErrorPage = PSLegacyEncrypter.getInstance(
                            PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                    ).decrypt(redirect, PSLegacyEncrypter.getInstance(
                            PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                    ).DEFAULT_KEY(),null);
                }
            }

            urlErrorPage = urlErrorPage.startsWith("/") ? urlErrorPage : "/" + urlErrorPage;
            resp.sendRedirect(hostRedirect + urlErrorPage);
        }
        else
        {
            urlErrorPage = hostRedirect + "/perc-form-processor/error.html";
            resp.sendRedirect(urlErrorPage);
        }
    }

    @Override
    public String getVersion() {

        String version = super.getVersion();

        log.info("getVersion() from PSFormRestService ...{}", version);

        return version;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
        log.debug("Nothing to do in forms service for site: {}", prevSiteName);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}



