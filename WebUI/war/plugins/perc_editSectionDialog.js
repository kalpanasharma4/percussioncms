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
 * Edit Section Metadata
 */
(function($){

//Add custom validation method for the URL name.
//$.validator.addMethod( 'url_name', 
//	    function(x) { return x.match( /^[a-zA-Z0-9\-]*$/ ); }, 
//       I18N.message( "perc.ui.newpagedialog.error@Url name validation error" ));

    $.perc_editSectionDialog = function() {
        function openDialog(sectionId, callback) {
            //make an Ajax request to get the folder properties and call the createDiaog method by suppling that data.
            var url = $.perc_paths.SECTION_GET_PROPERTIES + "/" + sectionId;
            $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,createDialog);

            var taborder = 0;
            var v;
            var dialog;

            function createDialog(status,result)
            {
                var self = this;

                if(status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    $.perc_utils.alert_dialog({title: 'Error', content: defaultMsg});
                    callback(cancel);
                    return;
                }
                var sectionProps = result.data.SiteSectionProperties;
                dialog = $("<div>" +
                    "<form action='#' method='GET'> " +
                    "<div style='background: #E6E6E9; padding-top: 5px; padding-right: 10px; text-align:right;'><label>" +I18N.message("perc.ui.general@Denotes Required Field") + "</label></div>" +
                    "<div class='fieldGroup'>" +
                    "<div id='perc-section-general-container'>" +
                    "<label for='perc-section-name' class='perc-required-field'>" + I18N.message( "perc.ui.newSectionDialog.label@Section name" ) + ":</label> <br/> " +
                    "<input type='text' aria-required='true' class='required' tabindex='0' id='perc-section-name' name='section_name' maxlength='512'/> <br/>" +
                    "<label for='perc-section-url' class='perc-required-field'>" + I18N.message( "perc.ui.newSectionDialog.label@URL" ) + ":</label> <br/> " +
                    "<input type='text' class='required' aria-required='true' tabindex='0' id='perc-section-url' name='page_url' maxlength='100'/> <br/>" +
                    "<label for='perc-external-link-target'>" + I18N.message( "perc.ui.editSectionDialog.label@Target window" ) + ":</label> <br/>" +
                    "<select id='perc-section-target' name='perc-section-target'>" +
                    "<option value='_self' selected>" + I18N.message( "perc.ui.editSectionDialog.label@Same window" ) + "</option>" +
                    "<option value='_blank'>" + I18N.message( "perc.ui.editSectionDialog.label@New window" ) + "</option>" +
                    "<option value='_top'>" + I18N.message( "perc.ui.editSectionDialog.label@Top window" ) + "</option>" +
                    "<option value='_parent'>" + I18N.message( "perc.ui.editSectionDialog.label@Parent window" ) + "</option>" +
                    "</select> <br/>" +
                    "<label for='perc-section-navigation-cssclassnames'>" + I18N.message( "perc.ui.editSectionDialog.label@Navigation class names") + ":</label> <br/> " +
                    "<input type='text' tabindex='0' id='perc-section-navigation-cssclassnames' maxlength='255' name='perc-section-navigation-cssclassnames'/> <br/>" +
                    "</div>" +
                    "</div>" +

                    "<div class='fieldGroup'>" +
                    "<div id='perc-section-users-container'>" +
                    "<label for='perc-section-folder-permission'>" + I18N.message( "perc.ui.folderPropsDialog.label@Permission" ) + ":</label> <br/> " +
                    // refactored from radio buttons to drop downs
                    "<select name='perc-section-folder-permission' id='perc-section-folder-permission'>" +
                    "<option id='perc-folder-permission-option-read' value='" + $.PercFolderHelper().PERMISSION_READ  + "'>" + I18N.message( "perc.ui.folderPropsDialog.permissionValue@Read"  ) + "</option>" +
                    "<option id='perc-folder-permission-option-write' value='" + $.PercFolderHelper().PERMISSION_WRITE + "'>" + I18N.message( "perc.ui.folderPropsDialog.permissionValue@Write" ) + "</option>" +
                    "</select>" +
                    // render the list editor widget in the following div
                    "<div id='perc-section-permission-users'></div>" +
                    "</div>" +
                    "</div>" +

                    getSecureSectionFieldGroups() +
                    "<div class='ui-layout-south'>" +
                    "<div id='perc_buttons' style='z-index: 100;'></div>" +
                    "</div>" +
                    "</form> </div>").perc_dialog( {
                    resizable : false,
                    title: I18N.message( "perc.ui.editSectionDialog.title@Section Preferences" ),
                    modal: true,
                    dragStart:function() {
                        $("div.ac_results").hide();
                    },
                    closeOnEscape : false,
                    percButtons: {
                        "Save":	{
                            click: function() {
                                // get users from the list widget and build a comma separated string
                                var writePrincipals = "";
                                if(self.listEdit.isEnabled()) {
                                    var users = self.listEdit.getListItems();
                                    for(u=0; u<users.length; u++)
                                        writePrincipals += u===0?users[u]:","+users[u];
                                }
                                // add a hidden field to the form to pass the users
                                dialog.find('form').append("<input type='hidden' name='writePrincipals' value='"+writePrincipals+"'>");

                                // add a hidden field to the form to pass the requires login value (disabled object are not serialized)
                                var requiresLogin = $("#perc-requires-login").is(':checked');
                                dialog.find('form').append("<input type='hidden' name='requiresLogin' value='"+requiresLogin+"'>");

                                // Clear the white spaces from the class names field
                                var cssClassNamesField = dialog.find('#perc-section-navigation-cssclassnames');
                                cssClassNamesField.val(cssClassNamesField.val().replace(/ +/g, " "));

                                var sectionName = dialog.find('form').find('#perc-section-url');
                                var sectionNameValue = $.perc_textFilters.WINDOWS_FILE_NAME(sectionName.val().trim());
                                sectionName.val(sectionNameValue);

                                // submit the form
                                _submit();
                            },
                            id: "perc-edit-section-save"
                        },
                        "Cancel": {
                            click: function() {
                                _remove();
                            },
                            id: "perc-edit-section-cancel"
                        }
                    },
                    id: "perc-edit-section-dialog",
                    width: 520,
                    height: 618
                });

                // get the writePrincipals from the JSON object comming from the server
                // make sure it's an array even if it's a single object
                var writePrincipals = [];
                if(sectionProps.folderPermission.writePrincipals) {
                    writePrincipals = sectionProps.folderPermission.writePrincipals;
                    writePrincipals = Array.isArray(writePrincipals) ? writePrincipals : [writePrincipals];
                }

                $("#perc-section-name").val(sectionProps.title);
                $("#perc-section-url").val(sectionProps.folderName);
                $("#perc-section-target option[value='"+ sectionProps.target +"']").prop("selected",true);
                // Bind the filter to the class names field and retrieve its value
                $.perc_filterFieldText($('#perc-section-navigation-cssclassnames'), $.perc_autoFillTextFilters.IDNAMECDATAALPHA, ' ');
                $("#perc-section-navigation-cssclassnames").val(sectionProps.cssClassNames);

                if(sectionProps.folderPermission.accessLevel === $.PercFolderHelper().PERMISSION_READ)
                    $("#perc-folder-permission-option-read").prop("selected","true");
                else
                    $("#perc-folder-permission-option-write").prop("selected","true");

                _handleRequiresLogin(sectionProps);

                $("#perc-group-name-allowed").val(sectionProps.allowAccessTo);

                // build an array of users from the principals to populate the list editor widget
                var users = [];
                for(u=0; u<writePrincipals.length; u++)
                    users[u] = writePrincipals[u].name;

                $.PercUserService.getUsers(function(status, usersJson) {
                    if(status === $.PercUserService.STATUS_ERROR) {
                        $.PercUserView.alertDialog('Error while loading users', usersJson);
                        return;
                    }

                    // render the list editor widget in the div declared earlier in the dialog
                    self.listEdit = $.PercListEditorWidget({
                        "container" : "perc-section-permission-users",
                        "items"     : users,
                        "results"   : $.perc_utils.convertCXFArray(usersJson.UserList.users),
                        // element that will toggle enable/disable of this component
                        "toggler"   : $("#perc-section-folder-permission"),

                        // values of toggler that enable this component
                        "toggleron" : [$.PercFolderHelper().PERMISSION_READ],

                        // values of toggler that disable this component
                        "toggleroff": [$.PercFolderHelper().PERMISSION_WRITE, $.PercFolderHelper().PERMISSION_ADMIN],

                        "title1" : I18N.message( "perc.ui.folderPropsDialog.title@User Properties" )+":",
                        "title2" : I18N.message( "perc.ui.folderPropsDialog.permissionValue@Write" )

                        //Write (modify, add, remove and user content.)
                    });
                });

                v = dialog.find('form').validate({
                    errorClass: "perc-field-error",
                    validClass: "perc-field-success",
                    wrapper: "p",
                    validateHiddenFields: false,
                    debug: false,
                    submitHandler: function(form) {
                        dialog.remove();
                        callback("ok", $(form).serializeArray());
                    }
                });

                var section_name = $('#perc-section-name');
                var section_url = $('#perc-section-url');
                //var group_name_allowed = $('#perc-group-name-allowed');

                $.perc_textAutoFill(section_name, section_url, $.perc_autoFillTextFilters.URL);
                $.perc_filterField(section_name, $.perc_textFilters.NOBACKSLASH);
                $.perc_filterField(section_url, $.perc_textFilters.URL);

                _addFieldGroups();
                _manageGroupNameField();

            }

            function err( str ) {
                $('#perc-save-error').text( str ).effect('pulsate', {times: 1});
            }

            function _remove()	{
                dialog.remove();
                callback("cancel");
            }

            function _submit()	{
                dialog.find('form').trigger("submit");
            }

            // A private helper method to group the fields and create collapsible sections
            function _addFieldGroups() {
                var dialog = $('#perc-edit-section-dialog');
                var fieldGroups = [
                    { groupName : "perc-section-general-container", groupLabel : "Section"},
                    { groupName : "perc-section-users-container", groupLabel : "Users"},
                    { groupName : "perc-section-security-container", groupLabel : "Security"}
                ];
                $.each(fieldGroups, function(index) {
                    // Create HTML markup with the groupName minimizer/maximizer and
                    // insert it before the 1st field in each group
                    var minmaxClass = (index === 0) ? "perc-items-minimizer" : "perc-items-maximizer";
                    var groupHtml =
                        "<div class='perc-section-header'>" +
                        "<div class='perc-section-label' groupName='" + this.groupName + "'>" +
                        "<span tabindex='0' class='perc-min-max " + minmaxClass + "' ></span>" + this.groupLabel +
                        "</div>" +
                        "</div>";
                    dialog.find('#' + this.groupName).before(groupHtml);
                    // The first group will be the only one expanded (hide all others)
                    index !== 0 && dialog.find('#' + this.groupName).hide();
                });

                // Bind collapsible event
                dialog.find(".perc-section-label").off("click").on("click",function() {
                    var self = $(this);
                    self.find(".perc-min-max")
                        .toggleClass('perc-items-minimizer')
                        .toggleClass('perc-items-maximizer');
                    dialog.find('#' + self.attr('groupName')).toggle();
                });
				dialog.find(".perc-section-label").on("keydown",function() {
                   if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
					}
                });
            }

            function _manageGroupNameField(){
                var requiresLogin = $("#perc-requires-login");
                if (requiresLogin.is(':checked') && !requiresLogin.is(':disabled')){
                    $("#perc-group-name-allowed").prop('readonly',false).css('background-color', '#FFFFFF');
                }
                else{
                    $("#perc-group-name-allowed").prop('readonly', true).css('background-color', '#CCCCCC').val("");
                }
            }

            function _handleRequiresLogin(sectionProps){

                var requiresLogin = $("#perc-requires-login");
                if (sectionProps.requiresLogin)
                    requiresLogin.prop('checked', true);
                else
                    requiresLogin.prop('checked',false);

                requiresLogin.off();
                if (sectionProps.secureSite && !sectionProps.secureAncestor){
                    requiresLogin.prop('disabled',false).css('background-color', '#FFFFFF');
                    requiresLogin.off("click").on("click",function() {
                        _manageGroupNameField();
                    });
                }
                else{
                    requiresLogin.prop('disabled', true).css('background-color', '#CCCCCC');
                }
            }

            function getSecureSectionFieldGroups() {
                var fieldGroups = '';
                // if(!gIsSaaSEnvironment) {
                fieldGroups = "<div class='fieldGroup'>" +
                    '<div id="perc-section-security-container">' +
                    "<input type='checkbox' id='perc-requires-login' name='perc-requires-login' style='width:20px' tabindex='0'/>" +
                    "<label for='perc-requires-login'>" + I18N.message("perc.ui.editSectionDialog.label@Requires Log in") + "</label> <br/>" +
                    "<label for='perc-group-name-allowed' style='margin-left:5px'>" + I18N.message("perc.ui.editSectionDialog.label@Allow access to: (enter group names)") + "</label> <br/>" +
                    "<input type='text' id='perc-group-name-allowed' name='perc-group-name-allowed' >" +
                    "<p>" +I18N.message("perc.ui.editSectionDialog.label@Please use a comma to separate each group name") + "</p><br />" +
                    "</div>" +
                    "</div>";
                // }
                return fieldGroups;
            }


        }// End open dialog

        return {"open": openDialog};

    };

})(jQuery);
