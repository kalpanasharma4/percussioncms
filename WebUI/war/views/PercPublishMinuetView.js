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

var regionsList;
var publishingServerList;
var isEC2Instance;
$(document).ready(function() {

    siteView = 'card';
	PAGE_SIZE=5;
    availableRegions = {};
    availablePublishingServer={};
    siteListObject = {};
    serverListObject = {};
    selectedSiteData = {};
    selectedServerData = {};
    selectedServerId = null;
    initialLoad = true;
    checkEC2Instance();
    getAllRegions();
    getAllSites();
    getAllPublishingServer("Production");
});

function checkEC2Instance(){

    var checkCallback = function(status, result)
    {
        if (status == "error")
        {
            isEC2Instance = false;

        }
        else
        {
            isEC2Instance = result.data;

        }
    };
    $.PercPublisherService(false).isEC2InstanceCheck(checkCallback);

}


function resetPublishObjects() {
    siteListObject = {};
    serverListObject = {};
    selectedSiteData = {};
    clearSelectedServer();
}

function clearSelectedServer() {
    selectedServerData = {};
    selectedServerId = null;
}

function getAllSites() {

    // Start by retrieving a list of sites
    siteListDeferred = $.Deferred();
    $.PercSiteService.getSites(getSitesCallback);

    siteListDeferred.done(function(siteList) {
        siteListObject = siteList;
        updateSites();
    });

}

function getAllRegions() {
    if(regionsList != null){
        return;
    }

    // Start by retrieving a list of regions
    regionListDeferred = $.Deferred();
    $.PercPublisherService(false).getAvailableRegions(getRegionsCallback);

    regionListDeferred.done(function(rgnLst) {
        regionsList = rgnLst;
    });

}

function getAllPublishingServer(serverType,serverObj) {


    // Start by retrieving a list of regions
    publishingServerListDeferred = $.Deferred();

    if(serverType == undefined){
        serverType='PRODUCTION';
    }
    $.PercPublisherService(false).getAvailablePublishingServer(getPublishServerCallback,serverType);

    publishingServerListDeferred.done(function(pslLst) {
        publishingServerList = pslLst;
        $("#publishServer").empty();
        for(var i in publishingServerList) {
            $('#publishServer').append(new Option(publishingServerList[i],publishingServerList[i]));
        }
        var selectedserver;
        if(publishingServerList.length > 1){
            selectedserver = publishingServerList[0];
        }

        if(typeof serverObj !== 'undefined' && typeof serverObj.serverInfo !== 'undefined'){
            var selectedserverProp = getArrayProperty(serverObj.serverInfo.properties, "key", "publishServer");
            //Selecting Second Record as default because first one is us gov.
            if( typeof selectedserverProp !== 'undefined' && selectedserverProp !== null && typeof selectedserverProp.value !== 'undefined'){
                selectedserver = selectedserverProp.value;
            }
        }
        var isExist = publishingServerList.includes(selectedserver);
        if(isExist){
            $('#publishServer').val(selectedserver);
        }else{
            $('#publishServer').val("NONE");
        }
    });
}

function getDefaultFolder() {
    defaultFolderDeferred = $.Deferred();

    /*  Server type cannot be changed after initial setup, so first we check
    *   to find out if server data is defined. If not, we get the server type
    *   from the dropdown
    */
    if(!selectedServerData.serverInfo) {
        serverType = $('#percServerType').val();
    }
    else {
        serverType = selectedServerData.serverInfo.serverType;
    }
    $.PercPublisherService(false).getLocalFolderPath(selectedSiteData.siteId, serverType, getDefaultFolderCallback);
    defaultFolderDeferred.done(function(defaultFolder) {
        $('input[name=defaultServer]').val(defaultFolder);
        $('#ownServer').fadeOut('fast', function() {
            $('#defaultServer').fadeIn('fast');
        });
    });

}

function getSiteDetails() {
    serverListDeferred = $.Deferred();
    $.PercPublisherService(false).getServersList(selectedSiteData.siteId, getServersListCallback);

    serverListDeferred.done(function(serverList) {
        serverListObject = serverList;
        updateSiteDetails();

        // Initialize Status
        $.PercPublishStatusMinuetView(false).renderPublishStatusSection(selectedSiteData.siteId);
        $('#percPublishStatusListTarget').animateCss('fadeIn');

        // Initialize logs
        $.PercPublishLogsMinuetView(false).renderSiteLogsSection(selectedSiteData.siteId);
        $('#percServerLogListTarget').animateCss('fadeIn');
    });

}

function updateSites() {
    // Creates state and lists available sites
    processTemplate(siteListObject, 'templatePublishingCards', 'perc-publish-body-target', bindSitesEvents);
    // Restore previously saved view (grid vs list) or initiate default
    $('.perc-site-view-toggle-button > input[value=' + siteView + ']').trigger('click', function() {
        $('#perc-publish-body-target').animateCss('fadeIn');
    });

}


function updateSiteDetails() {
    //  Creates state
    processTemplate(serverListObject, 'templateSiteDetailsShell', 'perc-publish-body-target');
    // Lists available servers
    processTemplate(serverListObject, 'templatePublishServerList', 'publishServerListTarget');
    $('#publishServerListTarget').animateCss('fadeIn');
    // Updates site name and logo
    processTemplate(selectedSiteData, 'templateSiteTitleLogo', 'siteTitleLogoTarget');

    bindSiteDetailsEvents();

}

/* Start callback methods section */

function getDefaultFolderCallback(status, result) {
    defaultFolderDeferred.resolve(result[0]);
}

function getPrivateKeysCallback(status, result) {
    privateKeyListDeferred.resolve(result);
}

function getSitesCallback(status, result) {
    siteListDeferred.resolve(result);
}

function getRegionsCallback(status,result){
    regionListDeferred.resolve(JSON.parse(result[0]));
}

function getServersListCallback(status, result) {
    // The result needs to be parsed into JSON format first
    serverListDeferred.resolve(JSON.parse(result[0]));
}

function getServerPropertiesCallback(status, result) {
    serverPropertiesDeferred.resolve(result[0]);
}

function confirmationDialogCallback(eventObject) {
    response = $(eventObject).data('perc-confirmation-result');
    dialogConfirmationResponseDeferred.resolve(response);
}
function getPublishServerCallback(status,result){
    publishingServerListDeferred.resolve(JSON.parse(result[0]));


}

function processDeleteServerCallback(status, result) {
    stopProcessRunningAlert();
    $.when(processRunningAnimationDeferred).then(function() {
        response = {};
        response.result = {};
        response.source = I18N.message("perc.ui.publish.title@Delete Server");
        if(result[1] == 'success') {
            response.result.status = selectedServerData.serverInfo.serverName + ' ' + I18N.message("perc.ui.publish.title@Successfully Removed");
            deleteServerResponseDeferred.resolve(response);
        }
        else {
            response.result.warning = result;
            deleteServerResponseDeferred.resolve(response);
        }
    });
}

function publishCallback(status, result) {
    stopProcessRunningAlert();
    $.when(processRunningAnimationDeferred).then(function() {
        // If incremental publish window is open, close it
        if($('#percCloseIncrementalPublishPreviewOverlay')) {
            $('#percCloseIncrementalPublishPreviewOverlay').trigger('click');
        }
        parsedResponse = requestResultsParser(I18N.message("perc.ui.publish.title@Publish Request"), status, result);
        processAlert(response);
    });
}

function updateServerPropertiesCallback(status, result) {
    stopProcessRunningAlert();
    $.when(processRunningAnimationDeferred).then(function() {
        response = {};
        response.result = {};
        response.source = I18N.message("perc.ui.publish.title@Server Configuration");
        if(result[1] == 'success') {
            response.result.warning = false;
            response.result.status = result[0].serverInfo.serverName + ' ' + I18N.message("perc.ui.publish.title@Updated Successfully");
            $('#percServerPropertiesModal').modal('toggle');
            processAlert(response);
            clearSelectedServer();
            refreshServerList();
        }
        else{
            responseText = JSON.parse(result.request.responseText);
            response.result.warning = findVal(responseText, 'defaultMessage');
            processAlert(response);
        }
    });
}

/* End callback methods section */


/* Start bind events section */

function bindSitesEvents() {
    $('.perc-site-select').on('click keypress', function(event) {
        if(event.type === 'click' || event.which === 13) {
            percSiteSelect(this);
        }
    });

    $('.perc-site-view-toggle-button').on("click", function(evt) {
        toggleSiteView(this);
    });
	$('.perc-site-view-toggle-button').on("keydown", function(event) {
		if(event.code == "Enter" || event.code == "Space"){
				document.activeElement.click();
		}
    });

    $('.perc-site-filter-field').on('keyup', function() {
        filterString = $(this).val().toLowerCase();
        filterSitesByString(filterString);
    });
}

function bindSiteDetailsEvents() {
    $('.perc-publish-button').on("click", function(evt) {
        processPublish(this);
    });

    $('#percRefreshServerList').on("click", function(evt) {
        refreshServerList();
    });

    $('#percBackToSites').on("click", function(evt) {
        backToSites();
    });

    $('#percAddServer').on("click", function(evt) {
        addPublishingServer();
    });

    $('.perc-edit-server-properties').on("click", function(evt) {
        editServerProperties(this);
    });

}

function bindServerPropertiesEvents() {

    $('.perc-clear-server').on("click", function(evt) {
        clearSelectedServer();
    });

    $('#percUpdateServerProperties').on("click", function(evt) {
        processServerPropertiesForm(this);
    });

    $('#percDeleteServer').on("click", function(evt) {
        deleteServerRequest();
    });

    $('#percServerType').on("change", function(evt) {

        $('#defaultServerFlag').trigger('click');
        if($('#percServerType').val() === 'PRODUCTION') {
            $('#percPublishNowFlag').prop('disabled', false);
        }
        else{
            $('#percPublishNowFlag').prop('disabled', true);
        }
        getPublishingServerBasedOnServerType();
    });

    // Toggle server location input fields
    $('#defaultServerFlag').on("click", function(evt) {

        $('#ownServer').fadeOut('fast', function() {
            getDefaultFolder();
            $('#defaultServer').fadeIn('fast');
        });
        $("#percPublishSecureSiteConfigOnExactPath").closest("div").show();

    });

    $('#ownServerFlag').on("click", function(evt) {
        $('#defaultServer').fadeOut('fast', function() {
            $('#ownServer').fadeIn('fast');
        });
        $("#percPublishSecureSiteConfigOnExactPath").closest("div").show();
    });

    $('#useAssumeRole').on("change", function(evt) {
        if($('#useAssumeRole')[0].checked) {
            $('#ARNRole').prop('disabled', false);
            if(isEC2Instance != null && JSON.parse(isEC2Instance) === true){
                $("#s3accessSecurityKey").hide('fast');
            }else{
                $("#s3accessSecurityKey").show('fast');
            }
        }
        else{
            $('#ARNRole').prop('disabled', true);
            if(isEC2Instance != null && JSON.parse(isEC2Instance) === true){
                $("#s3accessSecurityKey").hide('fast');
            }else{
                $("#s3accessSecurityKey").show('fast');
            }
        }
    });

    $('.perc-driver-group').on("change", function(evt) {
        updateDriverPropertiesUi();
    });

    $('#publishType').on("change", function(evt) {
        updateDriverPropertiesUi();
    });

    // SFTP property bindings
    $('#fileDriver').on("change", function(evt) {
        if(evt.currentTarget.options.item(evt.currentTarget.options.selectedIndex).text === 'SFTP') {
            // Set the SFTP fields.
            //if neither password or key is checked, trigger the key as it is preferred.
            if (!$('#SFTPprivateKeyFlag').is(':checked') && !$('#SFTPpasswordFlag').is(':checked')) {
                $('#perc-sftp-password').prop("disabled", true);
                $('#privateKeyList').prop("disabled", false);
                $("#SFTPprivateKeyFlag").prop("checked",true);
            } else if ($('#SFTPprivateKeyFlag').is(':checked')) {
                $('#perc-sftp-password').prop("disabled", true);
                $('#privateKeyList').prop("disabled", false);
            } else if ($('#SFTPpasswordFlag').is(':checked')) {
                $('#perc-sftp-password').prop("disabled", false);
                $('#privateKeyList').prop("disabled", true);
            }

            $('#SFTPprivateKeyFlag').on("change", function (evt) {
                if ($('#SFTPprivateKeyFlag').is(':checked')) {
                    $('#perc-sftp-password').prop("disabled", true);
                    $('#privateKeyList').prop("disabled", false);
                }

            });

            $('#SFTPpasswordFlag').on("change", function (evt) {
                if ($('#SFTPpasswordFlag').is(':checked')) {
                    $('#perc-sftp-password').prop("disabled", false);
                    $('#privateKeyList').prop("disabled", true);
                }

            });
        }
    });

    // Always trigger driver change and secureFTP flag change on initial load
    $('.perc-driver-group').trigger('change');
    $('#secureFTP').trigger('change');
    //  If we are on initial server setup, trigger change on server type
    $('#percServerType').trigger('change');
    $('#useAssumeRole').trigger('change');

}

/* End bind events section */

function backToSites() {
    $('#perc-publish-body-target').animateCss('fadeOut faster', function() {
        $('#perc-publish-body-target').empty();
        refreshSiteList();
    });
}

function updateDriverPropertiesUi() {
    selectedType = $('#publishType').val();

    if(selectedType == 'File') {
        $('#databaseDriver').hide();
        $('#fileDriver').show();
        selectedDriver = $('#fileDriver').val();

    }

    if(selectedType == 'Database') {
        $('#databaseDriver').show();
        $('#fileDriver').hide();
        selectedDriver = $('#databaseDriver').val();
    }

    $('#percServerPropertiesFormTarget > form > section').each(function() {
        currentSection = this.id;
        desiredSection = 'percServerProperties' + selectedType + selectedDriver + 'Target';

        // Always show database common properties if type is database, hide if type is file
        if(selectedType == 'Database' && currentSection == 'percServerPropertiesDatabaseCommonTarget') {
            $(this).show('fast');
            return true;
        }
        else if(selectedType == 'File' && currentSection == 'percServerPropertiesDatabaseCommonTarget') {
            $(this).hide('fast');
            return true;
        }

        // Always show common properties and optional properties
        if(currentSection == 'percServerPropertiesCommonTarget' || currentSection == 'percServerPropertiesOptionalTarget') {
            $(this).show('fast');
            return true;
        }

        if(selectedType == 'Database' && (selectedDriver == 'MSSQL' || selectedDriver == 'MYSQL')) {
            $('#perc-database-name-container').show('fast');
        }
        else {
            $('#perc-database-name-container').hide('fast');
        }

        // Only show the local driver properties if the type is file and we don't have Amazon S3 selected
        if(selectedType == 'File' && currentSection == 'percServerPropertiesFileLocalTarget' && selectedDriver != 'AMAZONS3') {
            $(this).show('fast');
            return true;
        }
        else if(selectedType == 'File' && currentSection == 'percServerPropertiesFileLocalTarget' && selectedDriver == 'AMAZONS3') {
            $(this).hide('fast');
            $('#useAssumeRole').trigger('change');

            return true;
        }

        // Hide section if the section does not contain the type name
        if(!(currentSection.indexOf(selectedType) >= 0)) {
            $(this).hide('fast');
            return true;
        }

        if(currentSection == desiredSection) {
            $(this).show('fast');
        }

        else {
            $(this).hide('fast');
        }

    });

    /*  This logic determines when the default server should be auto selected
    *   If the local driver is chosen from the dropdown, look at the ownServerFlag
    *   to determine if that was the previously saved option. If not, we know the default
    *   server field is empty and a click event should be triggered
    */
    var ownServerFlag = $('#ownServerFlag');
    if (selectedDriver === 'Local' && !(ownServerFlag.is(':checked'))) {
        triggerEvent('defaultServerFlag', 'click');
    }
    if (selectedDriver === 'FTP' && !(ownServerFlag.is(':checked'))){
        triggerEvent('defaultServerFlag', 'click');
    }
    if (selectedDriver === 'FTPS' && !(ownServerFlag.is(':checked'))) {
        triggerEvent('defaultServerFlag', 'click');
    }
    if (selectedDriver === 'SFTP' && !(ownServerFlag.is(':checked'))) {
        triggerEvent('defaultServerFlag', 'click');
    }
}

function refreshServerList() {
    hideSection('#publishServerListTarget', 'fadeOut');
    hideSection('#percPublishStatusListTarget', 'fadeOut');
    hideSection('#percServerLogListTarget', 'fadeOut');
    setTimeout(getSiteDetails, 500);
}

function refreshSiteList() {
    // All selected and original objects need to be cleared first
    resetPublishObjects();
    getAllSites();
}

function percSiteSelect(eventObj) {
    selectedSiteId = $(eventObj).data('perc-site-id');
    selectedSiteData = getArrayProperty(siteListObject.SiteSummary, 'siteId', selectedSiteId);
    selectedSitePath = selectedSiteData.folderPath.substring(1, selectedSiteData.folderPath.length) + '/';
    updateNavLocation(selectedSiteData.name, selectedSitePath);
    $('#perc-publish-body-target').animateCss('fadeOut faster', function() {
        $('#perc-publish-body-target').empty();
        getSiteDetails();
    });
}

function filterSitesByString(siteFilterString) {
    $("#sitesListView tr").filter(function() {
        $(this).toggle($(this).text().toLowerCase().indexOf(siteFilterString) > -1);
    });
    $("#sitesCardView .perc-site-card").filter(function() {
        $(this).parent('.perc-site-card-container').toggle($(this).text().toLowerCase().indexOf(siteFilterString) > -1);
    });
}

function toggleSiteView(eventObj) {
    siteView = $(eventObj).find('input').val();
    if(siteView == 'list') {
        $('#sitesCardView').fadeOut('fast', function() {
            $('#sitesListView').fadeIn('fast');
        });
    }
    if(siteView == 'card') {
        $('#sitesListView').fadeOut('fast', function() {
            $('#sitesCardView').fadeIn('fast');
        });
    }
}

function processDeleteServer() {
    startProcessRunningAlert();
    deleteServerResponseDeferred = $.Deferred();
    $.PercPublisherService(false).deleteSiteServer(selectedSiteData.siteId, selectedServerData.serverInfo.serverId, processDeleteServerCallback);
    deleteServerResponseDeferred.done(function(response) {
        deleteServerRequestDeferred.resolve(response);
    });
}

function processPublish(eventData) {
    publishType = $(eventData).data('perc-publish-type');
    siteName = selectedSiteData.name;
    serverName = $(eventData).closest('.perc-publish-button-group').data('perc-server-name');

    if (publishType == 'full') {
        startProcessRunningAlert();
        $.PercPublisherService(false).publishSite(siteName, serverName, publishCallback);
    }

    // If the selected publish type is incremental, we will serve up the preview dialog first
    if (publishType == 'incremental') {
        serverId = $(eventData).data('perc-server-id');
        processIncrementalPreview(serverId);
    }

}

function processIncrementalPreview(serverId) {
    serverPropertiesDeferred = $.Deferred();
    $.PercPublisherService(false).getServerProperties(selectedSiteData.siteId, serverId, function(status, result) {
        serverPropertiesDeferred.resolve(result[0]);
    });

    serverPropertiesDeferred.done(function(serverProperties) {
        publishRelatedItems = getArrayProperty(serverProperties.serverInfo.properties, 'key', 'publishRelatedItems').value;
        serverType = serverProperties.serverInfo.serverType;
        console.log(serverProperties);
		loadIncrementalPage(siteName,serverName,1);
    });

}

function loadIncrementalPage(siteName,serverName,startIndex){
		$.PercPublisherService(false).getIncrementalItems(siteName, serverName, startIndex, PAGE_SIZE, function(status, result) {
		status == 'error' || result.PagedItemList.childrenInPage.length == 0 ? incrementalPreviewObject = {} : incrementalPreviewObject = result;
		console.log(incrementalPreviewObject);

		processTemplate(incrementalPreviewObject, 'templateIncrementalPublishPreviewOverlay', 'percIncrementalPublishPreviewOverlayTarget');
		createPaginatedTable(incrementalPreviewObject,1);
		bindIncrementalPublishEvents();
		$('#percIncrementalPublishPreviewOverlayTarget').animateCss('fadeIn faster');
		$('#percIncrementalPublishPreviewOverlay').modal('_enforceFocus');

		if(publishRelatedItems === true && serverType.toLowerCase() == 'production' && result.PagedItemList.childrenInPage.length > 0) {
			processIncrementalRelatedItemsPreview();
			$('#percIncrementalRelatedItemsTarget').animateCss('fadeIn faster');
		}
	});

}

function refreshIncrementalPage(siteName,serverName,startIndex){
		$.PercPublisherService(false).getIncrementalItems(siteName, serverName, startIndex, PAGE_SIZE, function(status, result) {
		status == 'error' || result.PagedItemList.childrenInPage.length == 0 ? incrementalPreviewObject = {} : incrementalPreviewObject = result;
		console.log(incrementalPreviewObject);

		processTemplate(incrementalPreviewObject, 'templateIncrementalPublishPreviewOverlay', 'percIncrementalPublishPreviewOverlayTarget');
		createPaginatedTable(incrementalPreviewObject,startIndex);
		bindIncrementalPublishEvents();


		if(publishRelatedItems === true && serverType.toLowerCase() == 'production' && result.PagedItemList.childrenInPage.length > 0) {
			processIncrementalRelatedItemsPreview();
		}
	});

}

function createPaginatedTable(data,startIndex){

    if(data.PagedItemList && data.PagedItemList.childrenCount > 0){

        var totalItems = data.PagedItemList.childrenCount;
        var totalPages = Math.ceil(totalItems/PAGE_SIZE);
        var activePage = Math.ceil(startIndex/PAGE_SIZE);

		var pageSpan = $("<a class='paginate_button'></a>");
        var first = pageSpan.clone().text("First").addClass('paginate_button first');
		first[0].setAttribute("startIndex",1);
        var sind = ((activePage-2)*PAGE_SIZE + 1)<1?1:(activePage-2)*PAGE_SIZE + 1;
        var prev = pageSpan.clone().text("Prev").addClass('paginate_button previous');
		prev[0].setAttribute("startIndex",sind);
        sind = ((activePage*PAGE_SIZE) + 1)>totalPages*PAGE_SIZE?((activePage-1)*PAGE_SIZE) + 1:activePage*PAGE_SIZE + 1;
        var next = pageSpan.clone().text("Next").addClass('paginate_button next');
		next[0].setAttribute("startIndex",sind);
        var last = pageSpan.clone().text("Last").addClass('paginate_button last');
		last[0].setAttribute("startIndex",(totalPages-1)*PAGE_SIZE+1);

        var pages = [];
        pages.push(activePage);
        for(i=1;i<=4 && pages.length<5;i++){
            if(activePage-i > 0)
                pages.unshift(activePage-i);
            if(activePage+i <= totalPages)
                pages.push(activePage+i);
        }

        var pagedItems = $("<span/>");
        for(i=0; i<pages.length; i++){
            var clName = activePage === pages[i]?"paginate_button current":"paginate_button";
            var pageNum = pageSpan.clone().text(pages[i]).addClass(clName);
			pageNum[0].setAttribute("startIndex",((pages[i]-1)*PAGE_SIZE)+1);
            pagedItems.append(pageNum);
            if(i<pages.length-1){
                pagedItems.append(",");
            }
            pagedItems.append("&nbsp;");
        }

        var pagingBar = $("<div class='perc-wrapper'/>");
        pagingBar.append(first).append(prev).append(pagedItems).append(next).append(last);
        pagingBar.find(".paginate_button").on("click",function(){
			let startIndex = this.getAttribute("startIndex");
			refreshIncrementalPage(siteName,serverName,startIndex);
        });
        pagingBar.find(".paginate_button").each(function(){
           if($(this).data("startIndex") === (activePage-1)*PAGE_SIZE + 1){
               $(this).addClass("perc-disabled").off();
           }
        });

        $('#perc-paging-buttons').append(pagingBar);

    }

}

function processIncrementalRelatedItemsPreview() {
    $.PercPublisherService(false).getIncrementalRelatedItems(siteName, serverName, 1, 3, function(status, result) {
        status == 'error' || result.PagedItemList.childrenInPage.length == 0 ? incrementalRelatedPreviewObject = {} : incrementalRelatedPreviewObject = result.PagedItemList;
        console.log(incrementalRelatedPreviewObject);
        processTemplate(incrementalRelatedPreviewObject, 'templateIncrementalPublishRelatedItems', 'percIncrementalRelatedItemsTarget');
        bindIncrementalRelatedItemsEvents();
    });
}

function bindIncrementalPublishEvents() {
    $('#percCloseIncrementalPublishPreviewOverlay').on("click", function() {
        hideSection('#percIncrementalPublishPreviewOverlayTarget', 'fadeOut faster');
    });

    $('#percIncrementalPublishConfirm').on("click", function() {
        processIncrementalPublish();
    });
}

function bindIncrementalRelatedItemsEvents() {
    $('#percSelectAllRelatedItems').on('click', function() {
        $("#percIncrementalPublishRelatedItemsList input[type=checkbox]").prop('checked', $(this).prop('checked'));
    });
}

function processIncrementalPublish() {
    startProcessRunningAlert();
    // First, gather all selected related items for approval
    var itemsToApprove =[];
    $('#percIncrementalPublishRelatedItemsList tr input[type=checkbox]').each(function() {
        relatedItemData = $(this).closest('tr').data('perc-related-item');
        if(this.checked == true && relatedItemData) {
            itemsToApprove.push(relatedItemData.id);
        }
    });
    // Call Publish Incremental with list of related items selected for approval.
    $.PercPublisherService(false).publishIncrementalWithApproval(siteName, serverName,JSON.stringify(itemsToApprove),publishCallback);
}


function addPublishingServer() {

    // We need to create a default object
    newServerData = {};
    newServerData.action = 'create';
    // Default state for new servers
    newServerData.serverInfo = {
        "type": "File",
        "properties": [{
            "key": "defaultServerFlag",
            "value": "false"
        }, {
            "key": "ownServerFlag",
            "value": "false"
        }, {
            "key": "HTML",
            "value": "true"
        }, {
            "key": "XML",
            "value": "false"
        }, {
            "key": "driver",
            "value": "Local"
        }, {
            "key": "publishRelatedItems",
            "value": "false"
        }, {
            "key": "ignoreUnModifiedAssets",
            "value": "false"
        }]
    };

    assembleServerForms(newServerData);
}

function editServerProperties(eventData) {
    selectedServerData.action = 'update';
    selectedServerId = $(eventData).data('perc-server-id');
    serverPropertiesDeferred = $.Deferred();
    $.PercPublisherService(false).getServerProperties(selectedSiteData.siteId, selectedServerId, getServerPropertiesCallback);
    serverPropertiesDeferred.done(function(currentProperties) {
        selectedServerData.serverInfo = currentProperties.serverInfo;
        assembleServerForms(selectedServerData);
    });

}

function assembleServerForms(serverObj) {
    processTemplate(serverObj, 'templateServerPropertiesModal', 'percModalTarget');
    processTemplate(serverObj, 'templateServerPropertiesForm', 'percServerPropertiesFormTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesCommon', 'percServerPropertiesCommonTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesFileLocal', 'percServerPropertiesFileLocalTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesFileAMAZONS3', 'percServerPropertiesFileAMAZONS3Target');
    processTemplate(serverObj, 'templatePercServerPropertiesFileFTP', 'percServerPropertiesFileFTPTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesFileFTPS', 'percServerPropertiesFileFTPSTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesFileSFTP', 'percServerPropertiesFileSFTPTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesOptional', 'percServerPropertiesOptionalTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesDatabaseCommon', 'percServerPropertiesDatabaseCommonTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesDatabaseMSSQL', 'percServerPropertiesDatabaseMSSQLTarget');
    processTemplate(serverObj, 'templatePercServerPropertiesDatabaseOracle', 'percServerPropertiesDatabaseOracleTarget');

    privateKeyListDeferred = $.Deferred();
    $.PercUtilService.getPrivateKeys(getPrivateKeysCallback);
    privateKeyListDeferred.done(function(keyData) {
        processTemplate(keyData.data.PrivateKeys, 'templatePrivateKeyOptions', 'privateKeyList');
    });

    bindServerPropertiesEvents();
    $('#percServerPropertiesModal').modal('toggle');

    addRegionOptions(serverObj);
    addPublishingServerOptions(serverObj);
}

function addRegionOptions(serverObj){
    $("#region").empty();
    for (var i in regionsList) {
        $('#region').append(new Option(regionsList[i], regionsList[i]));
    }

    var selectedRegion = getArrayProperty(serverObj.serverInfo.properties, "key", "region");
    if(selectedRegion != null){
        selectedRegion = selectedRegion.value;
    }
    //Selecting Third Record as default because first two is us gov.
    if(selectedRegion == null && regionsList.length > 3){
        selectedRegion = regionsList[2];
    }


    $('#region').val(selectedRegion);
}

function addPublishingServerOptions(serverObj){
    getAllPublishingServer(serverObj.serverInfo.serverType,serverObj);

}

function getPublishingServerBasedOnServerType(){
    serverType=$('#percServerType').val();
    getAllPublishingServer(serverType);
}



function deleteServerRequest() {
    dialogConfirmationResponseDeferred = $.Deferred();
    dialogObject = createDialogObject();
    dialogObject.type = 'warning';
    dialogObject.title = I18N.message("perc.ui.publish.title@Delete Server Dialog Title");
    dialogObject.message = I18N.message("perc.ui.publish.title@Delete Server Dialog");

    processTemplate(dialogObject, 'templateFullScreenDialog', 'percDialogTarget');
    $('.perc-fullscreen-dialog').animateCss('fadeIn');
    $('.perc-fullscreen-dialog').modal('_enforceFocus');

    // Bind response click
    $('.perc-confirmation-button').on("click", function() {
        confirmationDialogCallback(this);
    });

    dialogConfirmationResponseDeferred.done(function(response) {
        if(response == 'confirm') {

            deleteServerRequestDeferred = $.Deferred();

            processDeleteServer();

            deleteServerRequestDeferred.done(function(response) {

                processAlert(response);

                // Only close the modal when we have a successful response
                if(!(response.result.warning)) {
                    $('#percServerPropertiesModal').modal('toggle');
                    clearSelectedServer();
                    refreshServerList();
                }
            });

        }

        $('#percServerPropertiesModal').modal('_enforceFocus');
        hideSection('#percDialogTarget', 'fadeOut');

    });

}

function updateServerProperties(siteName, serverName, serverProperties) {
    startProcessRunningAlert();
    $.PercPublisherService(false).createUpdateSiteServer(siteName, serverName, serverProperties, updateServerPropertiesCallback);
}

function processServerPropertiesForm(eventData) {
    formProps = {};
    newServerProperties = {};

    // A lot of this section of logic comes from the original PublishVew.js

    serverForm = $('#percServerPropertiesForm');

    var newserverName = serverForm.find('input[percServerProp="serverName"]').val();
    var isDefault = serverForm.find('input[percServerProp="isDefault"]').is(':checked');
    var publishType = serverForm.find('select#publishType').val();
    var ignoreUnModifiedAssets = serverForm.find('input[percServerProp="ignoreUnModifiedAssets"]').is(':checked');
    var publishRelatedItems = serverForm.find('input[percServerProp="publishRelatedItems"]').is(':checked');
    var publishSecureSiteConfigOnExactPath = serverForm.find('input[percServerProp="publishSecureSiteConfigOnExactPath"]').is(':checked');
    var region = serverForm.find('select#region').val();

    if($('#percServerType').is(':visible')) {
        serverType = $('#percServerType').val();
    }
    else {
        serverType = selectedServerData.serverInfo.serverType;
    }

    if (publishType == 'Select') {
        publishType = '';
    }
    var driver;

    if(publishType == 'File') {
        driver = serverForm.find('select#fileDriver').val();
    }

    if(publishType == 'Database') {
        driver = serverForm.find('select#databaseDriver').val();
    }

    if (driver == 'Select') {
        driver = '';
    }
    if (driver == 'FTP' || driver == 'FTPS') {
        $('#SFTPprivateKeyFlag').prop('checked', false);
    }
    if(driver == 'SFTP'){
        $('#SFTPprivateKeyFlag').prop('checked', true);
    }

    //crawl through all properties and create an array of properties
    var serverProp = function() {

        if(selectedServerId == null) {
            serverName = newserverName;
        }
        else {
            serverName = selectedServerId;
        }

        var propFields = $("#percServerPropertiesForm").find('*[percServer' + selectedType + 'Prop]');
        var properties = [];
        $.each(propFields, function(index, value) {

            /*  Only parse the properties that are visible to prevent
            *   old entries for other configurations to be stored
            */
            if(!$(this).is(':visible')) {
                return true;
            }

            var inputField = $(value);
            var propName = inputField.attr('percServer' + selectedType + 'Prop');
            var propType = inputField.attr('type');
            var propVal;
            var ignoreProp = false;

            if (propType == 'radio') {
                propVal = inputField.prop('checked');
            } else if (propType == 'checkbox') {
                propVal = inputField.prop('checked');
            } else {
                if (propName === 'password') {
                    propVal = btoa(inputField.val());
                } else {
                    propVal = inputField.val();
                }
            }

            // Because of an inconsistency between how Local and FTP folder properties
            // are processed, the property name needs to be transformed to 'folder'
            // when the FTP driver is selected

            if (driver == 'FTP' || driver == 'SFTP' || driver == 'FTPS') {
                if (propName == 'defaultServerFlag') {
                    if (propVal == true) {
                        folder = $('#defaultServer').val();
                    }
                    if (propVal == false) {
                        folder = $('#ownServer').val();
                    }

                    var folderObjField = {
                        "key": 'folder',
                        "value": folder
                    };
                    properties.push(folderObjField);
                }
            }

            var propObjField = {
                "key": propName,
                "value": propVal
            };
            properties.push(propObjField);
        });
        // Add driver as property of server
        properties.push({
            "key": "driver",
            "value": driver
        });
        //push the ignore unmodified assets option
        properties.push({
            "key": "ignoreUnModifiedAssets",
            "value": ignoreUnModifiedAssets
        });
        //push the publish related items option
        properties.push({
            "key": "publishRelatedItems",
            "value": publishRelatedItems
        });

        properties.push({
            "key": "publishSecureSiteConfigOnExactPath",
            "value": publishSecureSiteConfigOnExactPath
        });
        return properties;
    };
    //Create the server Option Object
    var newServerPropObj = {
        serverInfo: {
            'isDefault': isDefault,
            'serverId': selectedServerId,
            'serverName': newserverName,
            'type': publishType,
            'isModified': '',
            'properties': serverProp(),
            'serverType': serverType
        }
    };

    updateServerProperties(selectedSiteData.siteId, serverName, newServerPropObj);
}
