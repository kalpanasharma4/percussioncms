
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
 * The main javascript file for widget builder.
 * 
 */
var WidgetBuilderApp = {};

(function($)
{
    WidgetBuilderApp.dirtyController = $.PercDirtyController;
    WidgetBuilderApp.saveOnDirty = handleWidgetSave;
    WidgetBuilderApp.isValidationError = false;
    var currentWidgetView = null;
    var currentTabIndex = 0;
    //Underscore template settings are overridden here to make it work with templates from JSP as the default <% is processed by JSP.
    _.templateSettings = {
        interpolate: /<\@\=(.+?)\@\>/gim,
        evaluate: /<\@(.+?)\@\>/gim,
        escape: /<\@\-(.+?)\@\>/gim
    };
    WidgetBuilderApp.startsWithAlphaRegEx = new RegExp('^[a-zA-Z]');
    $(document).ready(function(){
        
        
        //Initializes widget tabs
        $("#perc-widget-def-tabs").tabs(
        {
            // Disable all Layout and Style tabs at load time
            disabled: [0,1, 2, 3],
            select: function(event, ui)
            {
                var isValid = validateTabData(false);
                if(isValid)
                    currentTabIndex = ui.index;
                return isValid;
            }
        });
		var tabindexCounter = 122;
			$("#perc-widget-def-tabs").find('li').each(function() {
				$(this).attr('aria-disabled',"true");
				$(this).attr('tabindex',tabindexCounter++);

			});

        addButtonClickHandlers();

        //Initialize grid view
        $("#perc-wb-defs-container").append(WidgetBuilderApp.WdgDefGrid.render().$el);
        WidgetBuilderApp.loadDefinitions();
        $.PercNavigationManager.addLocationChangeListener(function(url, id, notifyComplete, params){
            confirmIfDirty(function(){
                notifyComplete(id, true);
            });
        });

    });
    function confirmIfDirty(successCallback){
    	 
        var msg = "The widget editor contains unsaved changes. " +
            "Click \"Don't Save\" to proceed and discard changes, \"Save\" to proceed and keep changes, or \"Cancel\" to continue editing.";
        var options = {
            question: msg,
            dontSaveCallback:function(){
                WidgetBuilderApp.isValidationError = false;
                dirtyController.setDirty(false);
                successCallback();                
            }
        };
                
        dirtyController.confirmIfDirty(successCallback, function(){}, options);
    }
    /**
     * Adds the click handlers to the buttons.
     */
    function addButtonClickHandlers()
    {
            
        $("#perc-wb-button-new").off("click").on("click",function(){
            handleWidgetNew();
        });
        $("#perc-widget-save").on("click",function(){
            var saveCallback = function(status, result){
                if(status === $.PercServiceUtils.STATUS_ERROR){
                    var errorMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback(false, errorMsg);
                    return;
                }
                $.PercBlockUI();
                $.PercWidgetBuilderService.saveWidgetDef(prepareWidgetDef(), function(status, result){
                    $.unblockUI();
                    if(!status){
                        $.perc_utils.alert_dialog({"title":"Widget save error", "content":result});
                        return;
                    }
                    dirtyController.setDirty(false);
                    if(!currentWidgetView.model.get("widgetId")){
                        currentWidgetView.model.set({"widgetId":result.WidgetBuilderValidationResults.definitionId});
                    }

                    WidgetBuilderApp.loadDefinitions();

                });
            };
            handleWidgetSave(saveCallback);
        });
        $("#perc-widget-close").on("click",function(){
            handleWidgetClose();
        });
        
        WidgetBuilderApp.updateToolBarButtons = function(disableButtons){
            if(disableButtons){
                $("#perc-wb-button-delete").addClass("ui-disabled").removeClass("ui-enabled").off();
                $("#perc-wb-button-edit").addClass("ui-disabled").removeClass("ui-enabled").off();
                $("#perc-wb-button-deploy").addClass("ui-disabled").removeClass("ui-enabled").off();
            }
            else{
                $("#perc-wb-button-delete").removeClass("ui-disabled").addClass("ui-enabled").off("click").on("click",function(){
                    handleWidgetDelete();
                });
                $("#perc-wb-button-edit").removeClass("ui-disabled").addClass("ui-enabled").off("click").on("click",function(){
                    handleWidgetEdit();
                });
                $("#perc-wb-button-deploy").removeClass("ui-disabled").addClass("ui-enabled").off('click').on('click',function(){
                    handleWidgetDeploy();
                });
            }
        };
    }

    /**
     * Delete button handler
     */
    function handleWidgetDelete(){
        var settings = {
            id: 'perc-widget-delete-confirm',
            title: "Delete Widget Warning",
            question: "Are you sure you want to delete the selected widget?",
            success: deleteWidget,
            cancel:function(){return false;},
            type: "YES_NO",
            width: 700
        };
        $.perc_utils.confirm_dialog(settings);
    }
    
    /**
     * Makes a service call to delete the widget and shows error if not deleted.
     */
    function deleteWidget(){
        jQuery.PercWidgetBuilderService.deleteWidgetDef(WidgetBuilderApp.selectedModel, function(status, result){
            if(!status){
                $.perc_utils.alert_dialog({"title":"Widget delete error", "content":result});
                return;
            }
            WidgetBuilderApp.loadDefinitions();
            if(currentWidgetView != null && currentWidgetView.model.get("widgetId") === WidgetBuilderApp.selectedModel){
                dirtyController.setDirty(false);
                handleWidgetClose();
            }
            WidgetBuilderApp.updateToolBarButtons(true);
        });
    }

    /**
     * Edit widget button handler
     */
    function handleWidgetEdit(){
        confirmIfDirty(function(){
            if(WidgetBuilderApp.isValidationError)
            {
                WidgetBuilderApp.dirtyController.setDirty(true,"Widget",WidgetBuilderApp.saveOnDirty);
                return;
            }
            $.PercWidgetBuilderService.loadWidgetDefFull(WidgetBuilderApp.selectedModel, renderWidget);
        });
    }

    /**
     * New widget button handler
     */
    function handleWidgetNew(){
        confirmIfDirty(function(){
            if(WidgetBuilderApp.isValidationError)
            {
                WidgetBuilderApp.dirtyController.setDirty(true,"Widget",WidgetBuilderApp.saveOnDirty);
                return;
            }
            renderWidget(true, new WidgetBuilderApp.WidgetDefinitionModel(), true);
        });
    }

    /**
     * Makes a service call to create or update a widget
     */
    function renderWidget(status, result, isNew){
        //CMS-8047 : compatibility for jquery 1.9+
        $("#perc-widget-def-tabs").tabs('option', 'active', 0);
        $("#perc-widget-menu-buttons").show();
        if(!status){
            $.perc_utils.alert_dialog({"title":"Widget edit error", "content":result});
            return;
        }
        $("#perc-widget-def-tabs").tabs({disabled: []});
		var tabindexCounter = 122;
		$("#perc-widget-def-tabs").find('li').each(function() {
				$(this).attr('aria-disabled',"false");
				$(this).attr('tabindex',tabindexCounter++);
			});
        //CMS-8177 : "perc-widget-editing-container" class is used on all tab containers. Calling ".show()" function on the class caused all the tab content to show in first tab container element.
        $("#perc-widget-tab-general").show();
        var wdgModel = new WidgetBuilderApp.WidgetDefinitionModel();
        var wdgObject = isNew ? result : wdgModel.convertFromServerObject(result.WidgetBuilderDefinitionData);
        wdgModel.set(wdgObject);
        currentWidgetView = new WidgetBuilderApp.WidgetDefinitionGeneralView({model: wdgModel});
        $("#perc-widget-tab-general").empty().append(currentWidgetView.render().el);
        //Content tab
        var fieldsObj = isNew ? result.get("fieldsList").fields : result.WidgetBuilderDefinitionData.fieldsList.fields;
        WidgetBuilderApp.fieldsList = new WidgetBuilderApp.FieldCollection();
        WidgetBuilderApp.fieldsList.add(fieldsObj);
        WidgetBuilderApp.fieldsListView = new WidgetBuilderApp.FieldListView({model:WidgetBuilderApp.fieldsList});
        $('#perc-widget-fields-container').empty().html(WidgetBuilderApp.fieldsListView.render().el);
        //Display tab
        var widgetHtmlModel = new WidgetBuilderApp.WidgetHtmlModel();
        var widgetHtmlObj = isNew ? result.get("widgetHtml") : result.WidgetBuilderDefinitionData.widgetHtml;
        widgetHtmlModel.set({widgetHtml:widgetHtmlObj});
        WidgetBuilderApp.widgetHtmlView = new WidgetBuilderApp.WidgetHtmlView({model:widgetHtmlModel});
        $('#perc-widget-display-html-container').empty().html(WidgetBuilderApp.widgetHtmlView.render().el);
        //Resources tab
        var jsResObj = isNew ? result.get("jsFileList").resourceList : result.WidgetBuilderDefinitionData.jsFileList.resourceList;
        WidgetBuilderApp.jsResList = new WidgetBuilderApp.WidgetResourceCollection();
        var jsResModels = [];
        if (Array.isArray(jsResObj)) {
            $.each(jsResObj, function(){
                jsResModels.push({name:this});
            }, this);
        }
        else if(typeof jsResObj !== "undefined" && jsResObj.trim()!==""){
            jsResModels.push({name:jsResObj});
        }
        WidgetBuilderApp.jsResList.add(jsResModels);
        WidgetBuilderApp.jsResListView = new WidgetBuilderApp.ResourceListView({model:WidgetBuilderApp.jsResList});
        $('#perc-widget-js-resources-container').html(WidgetBuilderApp.jsResListView.render().el);

        var cssResObj = isNew ? result.get("cssFileList").resourceList : result.WidgetBuilderDefinitionData.cssFileList.resourceList;
        WidgetBuilderApp.cssResList = new WidgetBuilderApp.WidgetResourceCollection();
        var cssResModels = [];
        if (Array.isArray(cssResObj)) {
            $.each(cssResObj, function(){
                cssResModels.push({
                    name: this
                });
            }, this);
        }
        else if(typeof cssResObj !== 'undefined' && cssResObj.trim() !==""){
            cssResModels.push({name:cssResObj});
        }
        WidgetBuilderApp.cssResList.add(cssResModels);
        WidgetBuilderApp.cssResListView = new WidgetBuilderApp.ResourceListView({model:WidgetBuilderApp.cssResList});
        $('#perc-widget-css-resources-container').html(WidgetBuilderApp.cssResListView.render().el);
        
        //Add character input filters
        var generalForm = $("form[name=perc-widget-general-tab-form]");
        $.perc_filterField(generalForm.find("input[name=widgetname]"), $.perc_textFilters.ALPHA_NUMERIC);
        $.perc_filterField(generalForm.find("input[name=version]"), $.perc_textFilters.DIGITS_DOT);
		
    }
    
    /**
     * Widget deploy button handler
     */
    function handleWidgetDeploy(){
        var isDirty = WidgetBuilderApp.dirtyController.isDirty();
        confirmIfDirty(function(){
            if(WidgetBuilderApp.isValidationError)
            {
                WidgetBuilderApp.dirtyController.setDirty(true,"Widget",WidgetBuilderApp.saveOnDirty);
                return;
            }
            var settings = {
                id: 'perc-widget-deploy-confirm',
                title: "Deploy Widget Warning",
                question: "Are you sure you want to deploy the selected widget? Once a widget is deployed, it cannot be undeployed.",
                success: deployWidget,
                cancel:function(){
                    if(isDirty)
                        WidgetBuilderApp.dirtyController.setDirty(true,"Widget",WidgetBuilderApp.saveOnDirty);
                },
                type: "YES_NO",
                width: 700
            };
            $.perc_utils.confirm_dialog(settings);
        });
    }
    
    /**
     * Makes a service call to the server and displays the message to user.
     */
    function deployWidget(){
        $.PercBlockUI();
        $.PercWidgetBuilderService.deployWidget(WidgetBuilderApp.selectedModel, function(status, result){
            $.unblockUI();
            if(!status){
                $.perc_utils.alert_dialog({"id":"perc-widget-depoly-error","title":"Widget deploy error", "content":result});
                return;
            }
            $.perc_utils.alert_dialog({"id":"perc-widget-depoly-confirmation","title":"Widget deploy confirmation", "content":"Congratulations! selected widget has been deployed successfully."});
        });

    }

    /**
     * Widget save button handler.
     */
    function handleWidgetSave(callback){

        $.perc_utils.checkValidUserSession(
            function(data, textStatus){
                location.reload();
            },
            function(data, textStatus){
                var dataObj = prepareWidgetDef();
                if(!validateTabData(true))
                {
                    WidgetBuilderApp.isValidationError = false;
                }
                else
                {
                    WidgetBuilderApp.isValidationError = true;
                    if(callback)
                        callback();
                    return;
                }
                $.PercBlockUI();
                $.PercWidgetBuilderService.saveWidgetDef(dataObj, function(status, result){
                    $.unblockUI();
                    if(!status){
                        $.perc_utils.alert_dialog({"title":"Widget save error", "content":result});
                        return;
                    }
                    dirtyController.setDirty(false);
                    if(!currentWidgetView.model.get("widgetId")){
                        currentWidgetView.model.set({"widgetId":result.WidgetBuilderValidationResults.definitionId});
                    }

                    WidgetBuilderApp.loadDefinitions();
                    if(callback)
                        callback();
                });
            }
        );


    }

    function prepareWidgetDef(){
        var dataObj = currentWidgetView.model.convertToServerObject();
        //Add fields
        dataObj.WidgetBuilderDefinitionData.fieldsList = {};
        dataObj.WidgetBuilderDefinitionData.fieldsList.fields = WidgetBuilderApp.fieldsList.toJSON();
        dataObj.WidgetBuilderDefinitionData.widgetHtml = editor.getValue();
        //Add Resources
        dataObj.WidgetBuilderDefinitionData.jsFileList = {};
        dataObj.WidgetBuilderDefinitionData.jsFileList.resourceList = WidgetBuilderApp.jsResListView.toStringArray();
        dataObj.WidgetBuilderDefinitionData.cssFileList = {};
        dataObj.WidgetBuilderDefinitionData.cssFileList.resourceList = WidgetBuilderApp.cssResListView.toStringArray();
        return dataObj;       
    }
    
    function validateTabData(isOnSave){
        $(".perc_field_error").empty().hide();
        var isValid = false;
        jQuery.PercWidgetBuilderService.validate(prepareWidgetDef(),function(status, result){
            if(!status){
                $.perc_utils.alert_dialog({"title":"Widget validation error", "content":result});
            }
            else{
                var valRes = [];
                var errors = {};
                var temp = result.WidgetBuilderValidationResults.results;
                if(temp){
                    Array.isArray(temp)?valRes = temp:valRes.push(temp);
                    $(valRes).each(function(){
                        var cat = this.category;
                        if(!errors[cat])
                            errors[cat] = [];
                        errors[cat].push({name:this.name,message:this.message});
                    });
                    if(isOnSave){
                        for(var obj in errors){
                            showValidationErrors(obj,errors[obj]);
                        }
                        isValid = valRes.length === 0;
                    }
                    else{
                        var category = tabCategoryMap[currentTabIndex];
                        showValidationErrors(category, errors[category]);
                        isValid = !errors[category];
                    }
                }
                else{
                    isValid = true;
                }
            }
        });
        return isValid;
    }
    
    function showValidationErrors(tabCategory, errors){
        if(!errors)
            return;
        switch(tabCategory){
            case "GENERAL":
                currentWidgetView.showErrors(errors);
                break;
            default:
                var content = "The following field(s) have validation errors. <br/>";
                $(errors).each(function(){
                    content += this.name + " : " + this.message;
                });
                $.perc_utils.alert_dialog({"title":"Widget validation error(s)", "content":content});
        }
    }
    
    var tabCategoryMap = ["GENERAL","CONTENT","RESOURCES","DISPLAY"];
    /**
     * Widget close button handler
     */
    function handleWidgetClose(){
        confirmIfDirty(function(){
            if(WidgetBuilderApp.isValidationError)
            {
                WidgetBuilderApp.dirtyController.setDirty(true,"Widget",WidgetBuilderApp.saveOnDirty);
                return;
            }
            $(".perc-widget-editing-container").hide();
            $("#perc-widget-menu-buttons").hide();
            $("#perc-widget-def-tabs").tabs({disabled: [0,1,2, 3]});
			var tabindexCounter = 122;
			$("#perc-widget-def-tabs").find('li').each(function() {
				$(this).attr('aria-disabled',"true");
				$(this).attr('tabindex',tabindexCounter++);

			});
        });
    }
    //This method is just written for selenium webdriver
    WidgetBuilderApp.updateGeneralModel = function(){
        $(currentWidgetView.el).find("input, textarea").trigger("change");
    };

})(jQuery);



$(document).ready(function()
{
   $('.WBTabs').click(function(e)
   {
		var tabindexCounter = 122;
		$("#perc-widget-def-tabs").find('li').each(function() {
		$(this).attr('aria-disabled',"true");
		$(this).attr('tabindex',tabindexCounter++);
	});
   });

    $( "#perc-tab-widget-display" ).bind( "click", function() {
        editor.refresh();
    });

   $('.WBTabs').keydown(function(eventHandler){
    if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
		document.activeElement.click();
	}
   });

   if ($('#perc-add-js-resource-button').length){
	   $('#perc-add-js-resource-button').keydown(function(eventHandler){
		if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
			document.activeElement.click();
		}
	   });
   }

   if ($('#perc-add-css-resource-button').length){
	   $('#perc-add-css-resource-button').keydown(function(eventHandler){
		if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
			document.activeElement.click();
		}
	   });
   }

   if ($('#perc-display-html-auto-generate-button').length){
	   $('#perc-display-html-auto-generate-button').keydown(function(eventHandler){
		if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
			document.activeElement.click();
		}
	   });
   }
});

$(window).on('load', function() {
   if ($('#perc-wb-defs-container  .backgrid').length){
		var tbl= $("#perc-wb-defs-container  .backgrid");
		var tabCounter = 22;
		tbl.find('tr').each(function (i, el) {
			$(this).attr('tabindex', tabCounter++);
		});

		$("#perc-wb-defs-container  .backgrid thead tr th").each(function (i, el) {
			$(this).attr('scope', "col");
		});

		$("#perc-wb-defs-container  .backgrid tbody tr").each(function (i, el) {
			var myRow = $(this);
			myRow.find('td').each(function(j) {
				$(this).attr('scope', "row");
				return false;
			});
		});
   }
});

function setRowIndexOnContentData_1(){
	if ($('#perc-widget-fields-container').length){
		var tbl= $("#perc-widget-fields-container");
		var tabIndex=140;
		tbl.find('div').each(function (i, el) {
			var $this = $(this);
			var myClassName = this.className;
			if(myClassName == "perc-widget-field-wrapper"){
				this.setAttribute("tabindex", tabIndex++);

				$this.find('span').each(function (i, el) {
					this.setAttribute("tabindex", tabIndex++);
				});
			}
		});
   }
    $('.perc-add-field-button').keydown(function(eventHandler){
		if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
			document.activeElement.click();
		}
	   });


}

function setOnClickForDisplayAutoGenerate(){
	 $('#perc-display-html-auto-generate-button').keydown(function(eventHandler){
		if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
			document.activeElement.click();
		}
	   });
}





