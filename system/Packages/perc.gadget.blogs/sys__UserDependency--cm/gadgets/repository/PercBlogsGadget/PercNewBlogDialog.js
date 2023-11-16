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
 * @author Jose Annunziato
 */
(function($){
    percJQuery.PercNewBlogDialog = {
        moduleID : null,
        show : function(){
            percJQuery.PercWizard.show({
                width : 820,
                height: 580,
                steps : [
                    {title : I18N.message("perc.ui.blogs.Gadget@Define your blog"),      content : defineYourBlogWizardStep, cache : true, showRequiredFieldLabel : true},
                    {title : I18N.message("perc.ui.blogs.Gadget@Select your templates"), content : selectYourTemplatesStep,  cache : false}
                ],
                id : "perc-new-blog-dialog",
                style : "/cm/gadgets/repository/PercBlogsGadget/PercNewBlogDialog.css",
                beforeTransition : beforeTransition,
                afterTransition : afterTransition,
                stepTitleNavigation : false
            });
            
            // Redefining the event for input field of step 1. For some reason they lose the event
            var inputFields = percJQuery.PercWizard.dom.find("input");
            inputFields.on("keypress",function(){hideErrors(percJQuery.PercWizard.dom);});
        }
    };
    
    function getPageNameValue(wizardContentDom)
    {
        return wizardContentDom
            .find("input#perc-page-name").val().trim();
    }
    
    function getBlogLinkTextValue(wizardContentDom)
    {
        return wizardContentDom
            .find("input#perc-blog-text-link").val().trim();
    }
    
    function afterTransition(currentStep, nextStep, wizard){
        var wizardContentDom = wizard.dom;
        
        var pageName = wizardContentDom.find("#perc-page-name");
        var blogTextLink = wizardContentDom.find("#perc-blog-text-link");
        
        if(pageName.length>0 && blogTextLink.length>0){
            percJQuery.perc_textAutoFill(blogTextLink, pageName, percJQuery.perc_autoFillTextFilters.URL, false, 50);
            percJQuery.perc_filterField(pageName, percJQuery.perc_autoFillTextFilters.URL);
        }
    }

    function beforeTransition(currentStep, nextStep, wizard){
        var wizardContentDom = wizard.dom;
        
        hideErrors(wizardContentDom);
        
        var continueTransition = true;
        if(currentStep == 0 && currentStep == nextStep){
            continueTransition = true;
        }else if(currentStep == 0 && nextStep == 1){
            continueTransition = updateErrorMessages(wizardContentDom, currentStep);
        }else if(currentStep == 1 && nextStep == null){
            continueTransition = updateErrorMessages(wizardContentDom, currentStep);
            if(continueTransition){
                finish(wizard);
            }
        }
        return continueTransition;
    }
    
    function hideErrors(wizardContentDom){
        var errorMessages = wizardContentDom.find(".perc-error-message");
        errorMessages.hide();
    }

    function clearInputFields(wizardContentDom){
        wizardContentDom.find("input#perc-page-name").val("");
        wizardContentDom.find("input#perc-blog-text-link").val("");
    }
    
    function updateErrorMessages(wizardContentDom, currentStep){
        
        hideErrors(wizardContentDom);
        var pageNameError       = wizardContentDom.find(".perc-blog-page-name-error-message");
        var pageNameUniqueError = wizardContentDom.find(".perc-blog-page-name-unique-error-message");
        var blogLinkTextError   = wizardContentDom.find(".perc-blog-text-link-error-message");
        var blogLocationError   = wizardContentDom.find(".perc-blog-location-error-message");

        var blogIndexTemplateError = wizardContentDom.find(".perc-blog-index-page-template-error-message");
        var blogPostTemplateError  = wizardContentDom.find(".perc-blog-post-page-template-error-message");

        var pageName = getPageNameValue(wizardContentDom);
        var blogLinkText = getBlogLinkTextValue(wizardContentDom);

        if(pageName == "")
            pageNameError.show();
        if(blogLinkText == "")
            blogLinkTextError.show();

        if(siteName == null)
            blogLocationError.show();

        var blogIndexTemplateEmpty = wizardContentDom.find(".perc-blog-index-page-template-browser .perc-empty");
        var blogPostTemplateEmpty  = wizardContentDom.find(".perc-blog-post-page-template-browser .perc-empty");
        
        if(blogIndexTemplateEmpty.length > 0) {
            blogIndexTemplateError.show();
        }
        if(blogPostTemplateEmpty.length > 0) {
            blogPostTemplateError.show();
        }


        if(pageName == "" || blogLinkText == "" || siteName == null)
            return false;
            
        var blogExists = false;
        var hasWritePermission = true;
        
        var folderPath = "";
        var sectionPath =  "";
        if (selectedSectionPath == null)
        {
            folderPath = "/Sites/" + siteName;
            sectionPath = folderPath + "/" + pageName;
        }
        else
        {
            folderPath = "/Sites/" + selectedSectionPath;
            sectionPath = folderPath + "/" + pageName;
        }
        
        
        if(currentStep == 0){
            // validate workflow permission
	        percJQuery.PercUserService.getAccessLevel("percPage", -1, function(status, result){
	            if(status == percJQuery.PercServiceUtils.STATUS_ERROR)
	            {
	               percJQuery.perc_utils.alert_dialog({title: 'Warning', content: result});
	               hasWritePermission = false;
	               return;
	            }
	            else if(status == percJQuery.PercServiceUtils.STATUS_SUCCESS && (result == percJQuery.PercUserService.ACCESS_READ || result == percJQuery.PercUserService.ACCESS_NONE))
	            {
	               percJQuery.perc_utils.alert_dialog({title: 'Warning', content: I18N.message("perc.ui.blogs.Gadget@No Authorization")});
	               hasWritePermission = false;
	               return;
	            }
	        }, folderPath, true);

            if (!hasWritePermission)
                return false;
                	        
            // Validation of blog uniqueness
            var pathService = percJQuery.PercPathService;
            pathService.getLastExistingPath(sectionPath , function(status, result){
                blogExists = "/Sites/" + result == sectionPath ? true : false;
            }, true);
        }

        if(blogExists){
            pageNameUniqueError.show();
            return false;
        }

        if(currentStep == 1 && (blogIndexTemplateEmpty.length > 0 || blogPostTemplateEmpty.length > 0))
            return false;

        return true;
    }
    
    var step1Dom = null;
    function defineYourBlogWizardStep(wizardContentDom){

        if(step1Dom != null) {
            clearInputFields(step1Dom);
            return step1Dom;
        }
        
        tree = $("<div class='perc-select-blog-location-tree'>")
            .PercFinderTree({
                rootPath:$.PercFinderTreeConstants.ROOT_PATH_SITES,
                showFoldersOnly:true,
                height:"230px",
                width:"250px",
                classNames:{container:"perc-section-selector-container", selected:"perc-section-selected-item"},
                onClick:function(path){
                    if(path.accessLevel == "READ")
                    {
                       var msg = "You do not have write permissions to " + path.path;
                       percJQuery.perc_utils.alert_dialog({title: "Warning", content: msg});
                       return;
                    }
                    setSection(path);
                    hideErrors(step1Dom);
                }
            });
            
        var selectLocationDom = percJQuery("<div class='perc-select-blog-location'>")
            .append("<div class='perc-select-blog-location-label perc-label'>" + I18N.message("perc.ui.blogs.Gadget@Select a location") + "</div>")
            .append(tree)
            .append("<div class='perc-blog-location-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Blog Location Required")+"</div>");

        var selectTextLinkDom = percJQuery("<div class='perc-select-blog-text-link-ui'>")
            .append("<div class='perc-select-blog-text-link-label perc-label'>" + I18N.message("perc.ui.blogs.Gadget@Blog link text") + "</div>")
            .append("<div class='perc-select-blog-text-link-input perc-new-blog-input'><input type='text' aria-required='true' id='perc-blog-text-link'/>")
            .append("<div class='perc-blog-text-link-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Blog Link Text Required")+"</div>");
            
        var selectPageNameDom = percJQuery("<div class='perc-select-page-name-ui'>")
            .append("<div class='perc-select-page-name-label perc-label'>" + I18N.message("perc.ui.blogs.Gadget@Blog name") + "</div>")
            .append("<div class='perc-select-page-name-input perc-new-blog-input'><input type='text' aria-required='true' id='perc-page-name' maxlength='50'/>")
            .append("<div class='perc-blog-page-name-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Blog Name Required")+"</div>")
            .append("<div class='perc-blog-page-name-unique-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Unique Blog Name")+"</div>");
        
        var selectTitleNameAndLinkDom = percJQuery("<div class='perc-select-title-name-and-link'>")
            .append(selectTextLinkDom)
            .append(selectPageNameDom);
            
        
        var step1Dom = percJQuery("<div>")
            .append(selectLocationDom)
            .append(selectTitleNameAndLinkDom);
        
        var inputFields = step1Dom.find("input");
        inputFields.on("keypress",function(){hideErrors(step1Dom);});

        return step1Dom;
    }
    
    var siteName = null;
    var selectedSectionPath = null;
    /**
     * onClick callback from PercFinderTree. Sets the siteName (and section name)
     */
    function setSection(path){

        var folderPath = path.folderPath;

        // ignore if just Sites is selected
        if(folderPath == "//Sites") {
            siteName = null;
            selectedSectionPath = null;
            return;
        }
        
        // remove //Sites/
        folderPath = path.folderPath.replace("//Sites/","");
        
        siteName = folderPath.substring(0,folderPath.indexOf("/"));
        
        if(siteName == "") {
            siteName = folderPath;
            selectedSectionPath = null;
        } else {
            selectedSectionPath = folderPath;
        }
    }

    var step2Dom = null;
    function selectYourTemplatesStep(wizardContentDom){
        
        var clearBoth = $("<div style='clear:both'>&nbsp;</div>");
        var blogIndexPageTemplate = $("<div class='perc-blog-index-page-template-browser'>").PercScrollingTemplateBrowser({siteName:siteName,width:660, widgetDefId: "percBlogIndexPage"}); 
        var blogPostPageTemplate  = $("<div class='perc-blog-post-page-template-browser'>").PercScrollingTemplateBrowser({siteName:siteName,width:660, widgetDefId: "percBlogPost"});
        var blogIndexPageTemplateErrorMessage = $("<div class='perc-blog-index-page-template-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Blog Index Page Template Required")+"</div>");
        var blogPostPageTemplateErrorMessage  = $("<div class='perc-blog-post-page-template-error-message perc-error-message'>"+I18N.message("perc.ui.blogs.Gadget@Blog Post Page Template Required")+"</div>");
        step2Dom = $("<div class='perc-select-blog-templates'>")
            .append("<div class='perc-blog-index-page-template-label perc-label'>"+I18N.message("perc.ui.blogs.Gadget@Blog Index Page Template Select")+"</div>")
            .append(blogIndexPageTemplate)
            .append(blogIndexPageTemplateErrorMessage)
            .append("<div class='perc-blog-post-page-template-label perc-label'>"+I18N.message("perc.ui.blogs.Gadget@Blog Post Page Template Select")+"</div>")
            .append(blogPostPageTemplate)
            .append(blogPostPageTemplateErrorMessage)
            .append("<div style='margin-top:10px'><input id='perc_copy_templates' type='checkbox' style='width:15px;margin:5px 5px 5px 40px;top:0'/> <label for='perc_copy_templates'>"+I18N.message("perc.ui.blogs.Gadget@Make Copies")+"</label></div>");
            
        return step2Dom;
    }

    function step3(wizardContentDom){
        return "Step 3";
    }
    
    function finish(wizard)
    {
        var wizardContentDom = wizard.dom;

        var pageName = getPageNameValue(wizardContentDom);
        var blogLinkText = getBlogLinkTextValue(wizardContentDom);
        
        // Replace all spaces for blogName
        pageName = pageName.replace(/\s/g, "-");
        
        var templateSectionObject = wizardContentDom.find(".perc-blog-index-page-template-browser .perc-scrollable .item.perc-selected-item .item-id")[0];
        var templatePageObject = wizardContentDom.find(".perc-blog-post-page-template-browser .perc-scrollable .item.perc-selected-item .item-id")[0];

        var sectionPath =  "";
        if (selectedSectionPath == null)
        {
            sectionPath = "//Sites/" + siteName;
        }
        else
        {
            sectionPath = "//Sites/" + selectedSectionPath;
        }

        if (templateSectionObject != null && templatePageObject != null)
        {
            var templateSectionID = templateSectionObject.innerHTML;
            var templatePageID = templatePageObject.innerHTML;
            var copyTemplates = wizardContentDom.find("#perc_copy_templates").is(':checked');
            
            var sectionObj = {'CreateSiteSection' : {
               'pageName' : getPageNameWithExtension(siteName),
               'pageTitle' : blogLinkText,
               'templateId' : templateSectionID,
               'pageUrlIdentifier' : pageName,
               'pageLinkTitle' : blogLinkText,
               'folderPath': sectionPath,
               'sectionType':"blog",
               'blogPostTemplateId': templatePageID,
               'copyTemplates': copyTemplates} };
               
            percJQuery.PercBlockUI();
            percJQuery.Perc_SectionServiceClient.create(sectionObj, function(status, data){
                addNewSectionCallback(status, data, sectionObj);
            });
        }
    }
    
    function getPageNameWithExtension(site)
	{
		var pageName;
		percJQuery.PercSiteService.getSiteProperties(site, function(status, result) {
			if(status == percJQuery.PercServiceUtils.STATUS_SUCCESS) {
				var fileName = "index";
				var fileExt = result.SiteProperties.defaultFileExtention;
				if (fileExt && fileName.lastIndexOf(".") < 0) {
					if (fileName.length + fileExt.length < 255){ //consider a dot as one more char
						fileName += "." + fileExt;
					} else {
						fileName = fileName.substring(0, 254 - fileExt.length) + "." + fileExt; //consider a dot as one more char
					}
					pageName = fileName;
				}
			} else {
				percJQuery.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
			}
			
			return pageName;
		});
	}
    
    function addNewSectionCallback(status, data, sectionObj)
    {
        if(status === percJQuery.PercServiceUtils.STATUS_SUCCESS)
        {
            percJQuery.PercWizard.dialog.remove();
            percJQuery.PercWizard.dom.remove();
            percJQuery.unblockUI();
            var blogId = data.SiteSection.id;
            openPosts(blogId);
        }
        else
        {
            percJQuery.unblockUI();
            percJQuery.perc_utils.alert_dialog({title: 'Error', content: data});
        }
    }
    
    function openPosts(blogId)
    {
        PercMetadataService.save(
            "perc.user." + percJQuery.PercNavigationManager.getUserName() + ".dash.page." + "0" + ".mid." + percJQuery.PercNewBlogDialog.moduleID + "." + "selectedBlogID",
            blogId,
            function(){self.location.reload();}
        );
    }
})(jQuery);
