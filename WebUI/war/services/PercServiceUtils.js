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

/**
 * Utility functions and constants for use with client services.
 */
(function ($) {

    /**
     * Constant indicating a successful request.
     */
    var STATUS_SUCCESS = "success";
    /**
     * Constant indicating an unsuccessful request.
     */
    var STATUS_ERROR = "error";
    /**
     * Constant indicating an aborted request.
     */
    var STATUS_ABORT = "abort";
    /**
     * Constant for a DELETE request.
     */
    var TYPE_DELETE = "DELETE";
    /**
     * Constant for a GET request.
     */
    var TYPE_GET = "GET";
    /**
     * Constant for a POST request.
     */
    var TYPE_POST = "POST";
    /**
     * Constant for a PUT request.
     */
    var TYPE_PUT = "PUT";

    var TYPE_OPTIONS = "OPTIONS";

    var TYPE_HEAD="HEAD";

    var CSRF_HEADER="X-CSRF-HEADER";

    var CSRF_METADATA_PATH="/perc-metadata-services/metadata/csrf";
    var CSRF_FORMS_PATH="/perc-form-processor/forms/csrf";
    var CSRF_POLLS_PATH="/perc-polls-services/polls/csrf";
    var CSRF_INTEGRATION_PATH="/perc-integrations/integrations/csrf";
    var CSRF_COMMENTS_PATH="/perc-comments-services/comment/csrf";
    var CSRF_MEMBERSHIP_PATH="/perc-membership-services/membership/csrf";
    var CSRF_FEEDS_PATH="/feeds/rss/csrf";

    $.ajaxSetup({
        timeout: 300000
    });



    function csrfGetURLFromServiceCall(url){
        if(typeof url === "undefined" || url == null)
            return null;
        //Create a new link with the url as its href:
        var ret;
        var a = $('<a>', {
            href: url
        });
        var path = url;
        if(path.indexOf("/perc-metadata-services/") !== -1)
            path = CSRF_METADATA_PATH;
        else if(path.indexOf("/perc-form-processor/") !== -1)
            path = CSRF_FORMS_PATH;
        else if(path.indexOf("/perc-polls-services") !== -1)
            path = CSRF_POLLS_PATH;
        else if(path.indexOf("/perc-comments-services/")!== -1)
            path = CSRF_COMMENTS_PATH;
        else if(path.indexOf("/perc-membership-services/") !== -1)
            path = CSRF_MEMBERSHIP_PATH;
        else if(path.indexOf("/feeds/") !== -1)
            path = CSRF_FEEDS_PATH;
        else
            path = null;

        if(path!= null){
            return  path;
        }else{
            return null;
        }

    }

    function csrfGetToken(url,callback){
        if(callback.callcount > 1){
            return;
        }
        var csrfToken;
        if(typeof url != "undefined" && url != null){
            if(url.indexOf("/csrf") === -1){
                url = csrfGetURLFromServiceCall(url);
            }
            if ('function' === typeof (jQuery.getDeliveryServiceBase)) {
                var servicebase = jQuery.getDeliveryServiceBase();
                if(servicebase !== null && typeof servicebase !== 'undefined') {
                    if(url.indexOf(servicebase) === -1 ){
                        url = joinURL(servicebase,url);
                    }
                }
            }
        }
        var init = {
            url:url,
            async: "false",
            method: TYPE_HEAD, // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            credentials: 'omit', // include, *same-origin, omit
            headers: {
                'Content-Type': 'application/json',
                "Accept": "text/plain"
            },
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'origin-when-cross-origin',
            success: function(data, textstatus,response){
                if (typeof response !== 'undefined' && response != null) {
                    var tokenHeader = response.getResponseHeader("X-CSRF-HEADER");
                    if (typeof tokenHeader === "undefined" || tokenHeader === null) {
                        if (typeof callback.callcount !== 'undefined' && callback.callcount === 1) {
                            callback.callcount = 2;
                        }else{
                            callback.callcount = 1;
                        }
                        csrfGetToken(url,callback);
                        return;
                    }else{
                        callback(response);
                    }
                }
            },
            error: function(request, textstatus, error){
                console.debug(textstatus + ":" + error);
            }
        };

        $.ajax( url, init);

    }

    function joinURL(firstPart, secondPart){
        if("undefined" !== typeof (firstPart) && firstPart !== ""){
            if(firstPart.lastIndexOf("/") === firstPart.length-1){
                firstPart = firstPart.substring(0,firstPart.length-1);
            }
        }else{
            firstPart="";
        }

        if("undefined" !== typeof (secondPart) &&  secondPart !== ""){
            if(secondPart.substr(0,1) === "/"){
                secondPart = secondPart.substring(1);
            }
        }else{
            secondPart="";
        }

        return firstPart+ "/" + secondPart;
    }

    function toJSON(payload){
        if("" === payload){
            payload ={};
        }
        if("undefined" !== typeof(payload)){
            if(Array.isArray(payload) || "string" === typeof(payload)){
                return JSON.parse(payload);
            }else{
                return payload;
            }
        }else{
            return payload;
        }

    }
    /**
     *  Makes a request to a specified url, returning status and results
     *  in the passed in callback function. Uses JSON as the content type
     *  for passed in data and results.
     *
     *  @param url {String} the url string where the request will be made. Cannot
     *  be <code>null</code> or empty.
     *  @param type {String} the type of request to be made, one of the following:
     *  (DELETE, GET, POST, PUT).
     *  @param sync {boolean} flag indicating that a syncronous call should be made
     *  if set to <code>true</code>. It is recommended to try to keep this set
     *  to <code>false</code> as synchronous calls will freeze the UI until complete.
     *  @param callback {function} the callback function that will return status and
     *  request results.
     *  <pre>
     *  The callback arguments will be:
     *     <table>
     *      <tr><td>Arg</td><td>Desc</td></tr>
     *      <tr><td>status</td><td>success or error</td></tr>
     *      <tr><td>result</td><td>if successful then a object with the following
     *      properties {data, textstatus} else if unsuccessful then
     *      {request, textstatus, error}</td></tr>
     *     </table>
     *  </pre>
     *  @param dataObject {Object} a javascript object that will be serialized into
     *  JSON for request. This is optional and is only needed if the request type
     *  is POST.
     *  @param abortCallback {function} the callback function to which return if error
     *  request status <= 0 (no status recieved from server)
     */
    function makeJsonRequest(url, type, sync, callback, dataObject, abortCallback, timeout) {
        var self = this;

        var ajaxTimeout = $.perc_utils.percParseInt(timeout);
        var args = {
            dataType: 'json',
            async: !sync,
            contentType: 'application/json',
            method: type,
            type: type,
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            url: url,
            headers: {"perc-version": getVersion()},
            success: function (data, textstatus) {
                var result = {
                    data: data,
                    textstatus: textstatus
                };
                callback(self.STATUS_SUCCESS, result);
            },
            error: function (request, textstatus, error) {
                var result;
                // look for status 204 which should not be an error
                if (request.status === 204 || request.status === 1223) {
                    result = {
                        data: {},
                        textstatus: request.statusText
                    };
                    callback(self.STATUS_SUCCESS, result);
                    return;
                }
                else if (request.status > 0) {
                     result = {
                        request: request,
                        textstatus: textstatus,
                        error: error
                    };
                    callback(self.STATUS_ERROR, result);
                }
                else if (typeof abortCallback === "function") {
                    abortCallback(self.STATUS_ABORT);
                }
            }
        };
        if (dataObject) {
            $.extend(args, {data: JSON.stringify(dataObject)});
        }
        if (ajaxTimeout) {
            $.extend(args, {timeout: ajaxTimeout});
        }
        makeAjaxRequest(args);
    }
    function makeAjaxRequest(args){
		setLastRequestTime();
        if(!csrfSafeMethod(args.method)) {
            var u = csrfGetURLFromServiceCall(args.url);
            if (u != null) {
                csrfGetToken(u, function (response) {
                    var tokenHeader;
                    if (typeof response !== 'undefined' && response != null)
                            tokenHeader = response.getResponseHeader(CSRF_HEADER);
                        if (typeof tokenHeader !== "undefined" && tokenHeader != null) {
                            var token = response.getResponseHeader("X-CSRF-TOKEN");
                             if (tokenHeader != null && token != null) {
                                args.headers[tokenHeader] = token;
                            }
                        }
                        $.ajax(args);
                     });
            } else {
                $.ajax(args);
            }
        }else{
            $.ajax(args);
        }
    }

    /**
     *  Makes generic request
     *  TODO: Write the other requests in terms of this one
     */
    function makeRequest(url, type, sync, callback, dataObject, contentType, dataType, noEscape, abortCallback) {
        var self = this;

        if (dataType === null || typeof dataType === "undefined") {
            dataType = "text";

        }

        var args = {
            dataType: dataType,
            accepts: {
                "*": "text/html",
                text: "application/json",
                html: "text/html",
                xml: "application/xml, text/xml",
                json: "application/json, text/javascript"
            },
            async: !sync,
            method:type,
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            contentType: contentType,
            type: type,
            url: (noEscape) ? url : escape(url),
            headers: {"perc-version": getVersion()},
            success: function (data, textstatus) {
                var result = {
                    data: data,
                    textstatus: textstatus
                };
                callback(self.STATUS_SUCCESS, result);
            },
            error: function (request, textstatus, error) {
                var result;

                // look for status 204 which should not be an error
                if (request.status === 204 || request.status === 1223) {
                     result = {
                        data: {},
                        textstatus: request.statusText
                    };
                    callback(self.STATUS_SUCCESS, result);
                    return;
                }
                else if (request.status > 0) {
                     result = {
                        request: request,
                        textstatus: textstatus,
                        error: error
                    };
                    callback(self.STATUS_ERROR, result);
                }
                else if (typeof abortCallback === "function") {
                    abortCallback(self.STATUS_ABORT);
                }
            }
        };
        if (dataObject) {
            $.extend(args, {data: JSON.stringify(dataObject)});
        }
        makeAjaxRequest(args);

    }

    /**
     * Make a cross domain JSON ajax request to the delivery services. The request content type will be
     * set to application/json and the accepted response content type can be
     * either application/json or text/plain.
     * libraries.
     * @param servicebase the services base url, may be <code>null</code> in which
     * case it will try to find servicebase by attempting to call jQuery.getDeliveryServiceBase.
     * If that function is not found then an error will be thrown.
     * @param url the relative url for the service call, cannot be null or empty.
     * @param type action type either TYPE_GET or TYPE_POST, no other actions supported.
     * Cannot be null or empty.
     * @param callback the function called when the request returns its response or error. Cannot
     * be null. The callback passes 2 arguments
     * <pre>
     *    0:  status = STATUS_SUCCESS or STATUS_ERROR
     *    1:  response =
     *       if status is STATUS_SUCCESS then response object contains the following properties:
     *          data = either json object or string depending on response content type
     *          status = status number (i.e. 200)
     *       if status is STATUS_ERROR then the response object contains the following properties:
     *          message = error message
     *          status = status number, may be undefined
     * </pre>
     * @param dataObject JSON payload object for request, may be null.
     */
    function makeXdmXmlRequest(servicebase, url, type, callback, dataObject) {
        var self = this;

        if (null === callback || 'undefined' === typeof (callback)) {
            alert("Callback cannot be null or undefined");
            return;
        }
        if (!(type === self.TYPE_GET || type === self.TYPE_POST)) {
            alert("Invalid type specified, must be GET or POST.");
            return;
        }
        if (null === servicebase || 'undefined' === typeof (servicebase)) {
            // Let's see if jQuery.getDeliveryServiceBase exists and 
            // if so use it to get the service base
            if ('function' === typeof (jQuery.getDeliveryServiceBase)) {
                servicebase = jQuery.getDeliveryServiceBase();
            }
            else {
                alert("Servicebase not defined, cannot make ajax call.");
                return;
            }
        }
         if(url.indexOf(servicebase) === -1 ){
            url = joinURL(servicebase,url);
        }
        var body;
        // Add payload object if it exists
        if (null != dataObject && '' !== dataObject && 'undefined' !== typeof (dataObject)) {
            body = JSON.stringify(dataObject);
        }
        var init = {
            url:url,
            async: true,
            dataType: "text",
            method: type, // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            credentials: 'omit', // include, *same-origin, omit
            contentType: "application/json",
            accept:"application/xml",
            data:body,
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            headers: {
                'Content-Type': 'application/json',
                "Accept": "application/xml",
                "perc-version": getVersion()
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'origin-when-cross-origin',
            success: function(data, textstatus){
                var resp = {
                    data: data,
                    status: textstatus
                };
                callback(self.STATUS_SUCCESS,resp);
            },
            error: function(request, textstatus, error){
                var resp = {
                    message: error,
                    status: textstatus
                };
                callback(self.STATUS_ERROR, resp);
            }
        };
        makeAjaxRequest(init);

    }




    /**
     * Make a cross domain JSON ajax request to the delivery sevices. The request content type will be
     * set to application/json and the accepted response content type can be
     * either application/json or text/plain.
     * libraries.
     * @param servicebase the services base url, may be <code>null</code> in which
     * case it will try to find servicebase by attempting to call jQuery.getDeliveryServiceBase.
     * If that function is not found then an error will be thrown.
     * @param url the relative url for the service call, cannot be null or empty.
     * @param type action type either TYPE_GET or TYPE_POST, no other actions supported.
     * Cannot be null or empty.
     * @param callback the function called when the request returns its response or error. Cannot
     * be null. The callback passes 2 arguments
     * <pre>
     *    0:  status = STATUS_SUCCESS or STATUS_ERROR
     *    1:  response =
     *       if status is STATUS_SUCCESS then response object contains the following properties:
     *          data = either json object or string depending on response content type
     *          status = status number (i.e. 200)
     *       if status is STATUS_ERROR then the response object contains the following properties:
     *          message = error message
     *          status = status number, may be undefined
     * </pre>
     * @param dataObject JSON payload object for request, may be null.
     */
     function makeXdmJsonRequest(servicebase, url, type, callback, dataObject) {
        var self = this;
        if(null === callback || 'undefined' === typeof (callback))
        {
            console.error("Callback cannot be null or undefined");
            return;
        }

        if(null === servicebase || 'undefined' === typeof (servicebase)){
            // Let's see if jQuery.getDeliveryServiceBase exists and
            // if so use it to get the service base
            if('function' === typeof (jQuery.getDeliveryServiceBase))
            {
                servicebase = jQuery.getDeliveryServiceBase();
            }
            else
            {
                console.error("Servicebase not defined, cannot make ajax call.");
                return;
            }
        }

        if(url.indexOf(servicebase) === -1 ){
            url = joinURL(servicebase,url);
        }

        var body;
        // Add payload object if it exists
        if (null !== dataObject && '' !== dataObject && 'undefined' !== typeof (dataObject)) {
            body = JSON.stringify(dataObject);
        }

        var header =   {
            'Content-Type': 'application/json',
            "Accept": "application/json,text/plain",
            "perc-version": getVersion()
        };
        var init = {
            url: url,
            dataType: "text",
            contentType: "application/json",
            type: type,
            method:type,
            async: true,
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            data:body,
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            credentials: 'omit', // include, *same-origin, omit
            headers : header,
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'origin-when-cross-origin',
            success: function(data, textstatus){
                var resp = {
                    data: data,
                    status: textstatus
                };
                callback(self.STATUS_SUCCESS,resp);
            },
            error: function(request, textstatus, error){
                var resp = {
                    message: error,
                    status: textstatus
                };
                callback(self.STATUS_ERROR, resp);
            }
        };
        makeAjaxRequest(init);
    }

    function csrfSafeMethod(method) {
         if(typeof method === 'undefined' || method === null){
             return true;
         }
        // these HTTP methods require CSRF protection
        var csrfSafe =  (['post','put','delete'].indexOf(method.toLowerCase()) !== -1);
         return  !csrfSafe;
    }

    function getVersion(){
         //Version can't be empty as it is illegal to have an empty header
        return typeof $.getCMSVersion === "function" ? $.getCMSVersion() : "8.0";
    }

    /**
     *  Makes a request to a specified url, returning status and results
     *  in the passed in callback function. Uses XML as the content type
     *  for passed in data and results.
     *
     *  @param url {String} the url string where the request will be made. Cannot
     *  be <code>null</code> or empty.
     *  @param type {String} the type of request to be made, one of the following:
     *  (DELETE, GET, POST, PUT).
     *  @param sync {boolean} flag indicating that a syncronous call should be made
     *  if set to <code>true</code>. It is recommended to try to keep this set
     *  to <code>false</code> as synchronous calls will freeze the UI until complete.
     *  @param callback {function} the callback function that will return status and
     *  request results.
     *  <pre>
     *  The callback arguments will be:
     *     <table>
     *      <tr><td>Arg</td><td>Desc</td></tr>
     *      <tr><td>status</td><td>success or error</td></tr>
     *      <tr><td>result</td><td>if successful then a object with the following
     *      properties {data, textstatus} else if unsuccessful then
     *      {request, textstatus, error}</td></tr>
     *     </table>
     *  </pre>
     *  @param dataString {String} Xml to be passed by the request.
     *  This is optional and is only needed if the request type is POST.
     *  @param abortCallback {function} the callback function to which return if error
     *  request status <= 0 (no status recieved from server)
     */
    function makeXmlRequest(url, type, sync, callback, dataString, abortCallback) {
        var self = this;
        var args = {
            dataType: 'xml',
            async: !sync,
            contentType: 'application/xml',
            method:type,
            type: type,
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            url: url,
            headers: {"perc-version": getVersion()},
            success: function (data, textstatus) {
                var result = {
                    data: data,
                    textstatus: textstatus
                };
                callback(self.STATUS_SUCCESS, result);
            },
            error: function (request, textstatus, error) {
                if (request.status > 0) {
                    var result = {
                        request: request,
                        textstatus: textstatus,
                        error: error
                    };
                    callback(self.STATUS_ERROR, result);
                }
                else if (typeof abortCallback === "function") {
                    abortCallback(self.STATUS_ABORT);
                }
            }
        };
        if (dataString) {
            $.extend(args, {data: dataString});
        }
        makeAjaxRequest(args);
    }

    /**
     *  Makes a request to a specified url, returning status and results
     *  in the passed in callback function. Uses HTTP DELETE method.
     *
     *  @param url {String} the url string where the request will be made. Cannot
     *  be <code>null</code> or empty.
     *  @param sync {boolean} flag indicating that a syncronous call should be made
     *  if set to <code>true</code>. It is recommended to try to keep this set
     *  to <code>false</code> as synchronous calls will freeze the UI until complete.
     *  @param callback {function} the callback function that will return status and
     *  request results.
     *  <pre>
     *  The callback arguments will be:
     *     <table>
     *      <tr><td>Arg</td><td>Desc</td></tr>
     *      <tr><td>status</td><td>success or error</td></tr>
     *      <tr><td>result</td><td>if successful then a object with the following
     *      properties {data, textstatus} else if unsuccessful then
     *      {textstatus, error}</td></tr>
     *     </table>
     *  </pre>
     *  @param dataString {String} Text data to be passed by the request.
     *  @param abortCallback {function} the callback function to which return if error
     *  request status <= 0 (no status recieved from server)
     */
    function makeDeleteRequest(url, sync, callback, dataString, abortCallback) {
        var self = this;

        var args = {
            dataType: 'text',
            async: !sync,
            contentType: 'application/xml',
            method: self.TYPE_DELETE,
            type: self.TYPE_DELETE,
            url: url,
            headers: {"perc-version": getVersion()},
            success: function (data, textstatus) {
                var result = {
                    data: data,
                    textstatus: textstatus
                };
                callback(self.STATUS_SUCCESS, result);
            },
            error: function (request, textstatus, error) {
                if (request.status > 0) {
                    var result = {
                        request: request,
                        textstatus: textstatus,
                        error: error
                    };
                    callback(self.STATUS_ERROR, result);
                }
                else if (typeof abortCallback === "function") {
                    abortCallback(self.STATUS_ABORT);
                }
            }
        };
        if (dataString) {
            $.extend(args, {data: dataString});
        }
        makeAjaxRequest(args);
    }


    /**
     * Extracts the default error message from the returned request object.
     * Currently this ONLY works for JSON requests.
     * TODO: Extract for XML.
     * TODO: Use TMX and I18N
     * @param request the request may contain the default error message.
     * @return the default error message. It may be blank if cannot extract
     * the default error message from the request.
     */
    function extractDefaultErrorMessage(request) {
        var buff = "";
        if (request === null || typeof(request) === 'undefined')
            return "";
        //Handle responses from cross domain requests
        if (typeof(request.responseText) === 'undefined' && typeof(request.message) !== 'undefined') {
            request.responseText = request.message;
        }
        if (request.responseText === null || request.responseText.length === 0) {
            if (request.statusText && request.statusText !== '') {
                return request.statusText;
            }
            return "";
        }
        var error = null;
        try {
            error = JSON.parse(request.responseText);
            var ua = navigator.userAgent.toLowerCase();
            var b = {
                ieQuirks: null,
                opera: /opera/.test(ua),
                msie: /msie/.test(ua) && !/opera/.test(ua)
            };
            //IE handles this differently from all other browsers, of course
            if (b.msie && !error && typeof(request.responseText) === 'string' && request.responseText.length > 0) {
                return request.responseText;
            }
        }
        catch (e) {
        }

        if (error !== null && error.ValidationErrors !== undefined) {
            var verrors = error.ValidationErrors;
            if (verrors.fieldErrors !== undefined ) {
                buff += objectErrorToString(verrors.fieldErrors);
            }
            else if (verrors.globalErrors !== undefined) {
                buff += objectErrorToString(verrors.globalErrors);
            }
            else if (verrors.globalError !== undefined) {
                buff += objectErrorToString(verrors.globalError);
            }
        }
        else if (error != null) {
            if (typeof(error.defaultMessage) != 'undefined' && error.Errors)
                error = error.Errors;

            var def = "";
            if (typeof(error.defaultMessage) != 'undefined') {
                def = error.defaultMessage;
            }
            else if (error.Errors && typeof(error.Errors.globalError.defaultMessage) != 'undefined') {
                def = error.Errors.globalError.defaultMessage;
            }
            else if (typeof(error.globalError.defaultMessage) != 'undefined') {
                def = error.globalError.defaultMessage;
            }
            else if (typeof(error.localizedMessage) != 'undefined') {
                def = error.localizedMessage;
            }
            else if (error.Errors && typeof(error.globalError.code) != 'undefined') {
                var prefix = request.status === 500 ? "Server Error: " : "";
                def = prefix + error.Errors.globalError.code;
            }
            buff += def;
        }

        //XML section. for the moment just parse validation errors
        var xmlResponse;
        if (typeof(request.responseText) != 'undefined' && request.responseText.indexOf('<?xml') !== -1) {
            xmlResponse = $(request.responseText);
        }
        else if (typeof(request) === 'string' && request.indexOf('<?xml') !== -1) {
            xmlResponse = $(request);
        }
        if (typeof(xmlResponse) != 'undefined' && xmlResponse.is('ValidationErrors')) {
            if (xmlResponse.find('globalErrors').find('defaultMessage').text() !== "") {
                buff += xmlResponse.find('globalErrors').find('defaultMessage').text();
            }
            else if (xmlResponse.find('globalError').find('defaultMessage').text() !== "") {
                buff += xmlResponse.find('globalError').find('defaultMessage').text() !== "";
            }
            else if (xmlResponse.next('defaultMessage')) {
                buff += xmlResponse.next('defaultMessage')[0].nextSibling.nodeValue;
            }
        }
        if (buff === "") {
            buff = request.responseText;
        }
        return buff;
    }


    /**
     * Converts object validation errors that are in the response
     * into a string.
     * See com.percussion.share.validation.PSErrors.PSObjectError
     * @param objectErrors maybe null.
     * @return a string
     */
    function objectErrorToString(objectErrors) {
        objectErrors = $.makeArray(objectErrors);
        var buf = "";
        for (var i = 0; i < objectErrors.length; i++) {
            var oe = objectErrors[i];
            if (oe.defaultMessage) {
                buf += replaceMessageTokens(oe.defaultMessage, oe.arguments);
            }
            else if (oe.code) {
                buf += oe.code;
            }
        }
        return buf;
    }

    /**
     * Akin to Java's MessageFormat where {i} is
     * replaced with an argument.
     */
    function replaceMessageTokens(msg, args) {
        args = $.makeArray(args);
        var token = "";
        for (var i = 0; i < args.length; i++) {
            token = new RegExp("\\{" + i + "\\}", "g");
            msg = msg.replace(token, args[i]);
        }
        return msg;
    }

    /**
     * Extracts the code of a field error validation from the specified request.
     * @param request the request may contain a field error validation.
     * @return the error code. It may be blank if the request does not contain
     * a field error validation.
     */
    function extractFieldErrorCode(request) {
        var error = JSON.parse(request.responseText);
        if (error.ValidationErrors === undefined)
            return "";

        var verrors = error.ValidationErrors;
        if (verrors.fieldErrors !== undefined)
            return verrors.fieldErrors.code;

        return "";

    }

    /**
     * Extracts the code of a global error validation from the specified request.
     * @param request the request may contain a global error validation.
     * @return string error code. It may be blank if the request does not contain
     * a global error validation.
     */
    function extractGlobalErrorCode(request) {
        var error = JSON.parse(request.responseText);
        if (error.ValidationErrors === undefined) {
            return "";
        }

        var verrors = error.ValidationErrors;
        if (verrors.globalError !== undefined) {
            return verrors.globalError.code;
        }

        return "";

    }

    /**
     * Helper method that converts the psmap entires in to an associated array.
     * @param {Object} mapEntries expected to be psmap entries.
     */
    function convertMapToArray(mapEntries) {
        var tempArray = [];
        if (Array.isArray(mapEntries.entry))
            tempArray = mapEntries.entry;
        else if (mapEntries.entry)
            tempArray.push(mapEntries.entry);
        var result = [];
        $(tempArray).each(function () {
            result[this.key] = this.value;
        });
        return result;
    }

	function setLastRequestTime(){
		localStorage.setItem("lastRequestTime", new Date().getTime());
	}

    $.PercServiceUtils = {
        STATUS_ERROR: STATUS_ERROR,
        STATUS_SUCCESS: STATUS_SUCCESS,
        STATUS_ABORT: STATUS_ABORT,
        TYPE_DELETE: TYPE_DELETE,
        TYPE_GET: TYPE_GET,
        TYPE_POST: TYPE_POST,
        TYPE_PUT: TYPE_PUT,
        csrfGetToken: csrfGetToken,
        makeJsonRequest: makeJsonRequest,
        makeAjaxRequest:makeAjaxRequest,
        makeXmlRequest: makeXmlRequest,
        makeRequest: makeRequest,
        makeXdmJsonRequest: makeXdmJsonRequest,
        makeDeleteRequest: makeDeleteRequest,
        extractDefaultErrorMessage: extractDefaultErrorMessage,
        extractFieldErrorCode: extractFieldErrorCode,
        extractGlobalErrorCode: extractGlobalErrorCode,
        makeXdmXmlRequest: makeXdmXmlRequest,
        convertMapToArray: convertMapToArray,
        joinURL: joinURL,
        toJSON: toJSON
    };

})(jQuery);
