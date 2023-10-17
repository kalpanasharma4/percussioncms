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

(function($) {
$.perc_template_metadata_dialog = function(templateId) {
   var dialog;
   var doctypeHTML5;
   var doctypeXHTML;
   var doctypeCustom;
   var doctypeSelected;
   
   dialog = $("<div id='perc_template_metadata_dialog'><div class='perc-metadata-setcion-wrapper'>" +
               "<div class = 'fieldGroup perc-first-section'><div class = 'perc-section-header'><div class = 'perc-section-label' groupname = 'perc-section-doctype-container'>" +
               "<span class = 'perc-min-max perc-items-minimizer'></span>DocType/HTML Tags</div></div>" +
               "<div id = 'perc-section-doctype-container'>" +
               "<textarea id='perc_template_metadata_custom_doctype' class='percCustomDoctype tempmeta perc-readOnlyText' readonly = 'readonly' rows='10' cols='200'></textarea> <br />" +
               "<input style = 'width:auto' type='radio' name='doctype' value='html5' /> HTML5 (recommended)<br />" +
               "<input style = 'width:auto' type='radio' name='doctype' value= 'xhtml' /> XHTML Transitional<br />" +
               "<input style = 'width:auto' type='radio' name='doctype' value= 'custom' /> Custom<br /></div></div>" +

               "<div class = 'fieldGroup'><div class = 'perc-section-header'><div class = 'perc-section-label' groupname = 'perc-section-add-code-container'>" +
               "<span class = 'perc-min-max perc-items-maximizer'></span>Additional Code</div></div>" +
               "<div id = 'perc-section-add-code-container' groupname= 'perc-section-add-code-container' type='sys_normal' style='display: none'>" +
               "<label accesskey='' for='perc_template_metadata_additional_head_content'>Additional head content:</label> <br/> " +
               "<textarea id='perc_template_metadata_additional_head_content' name='perc_template_metadata_additional_head_content' wrap='soft' class='datadisplay' aarenderer='MODAL' requirescleanup='no' codeeditor='yes' codeeditor_mode='html'></textarea>" +
               "<script>"+
               "var editor12 = CodeMirror.fromTextArea(document.getElementById('perc_template_metadata_additional_head_content'), {"+
               "mode: 'htmlmixed',"+
               "htmlMode: true,"+
               "showCursorWhenSelecting: true,"+
               "theme: 'default',"+
               "lineNumbers: true,"+
               "height: 'auto',"+
               "lineWrapping: false,"+
               "indentUnit: 2,"+
               "tabSize: 4,"+
               "indentWithTabs: true,"+
               "matchBrackets: true,"+
               "saveCursorPosition: true,"+
               "styleActiveLine: true,"+
               "spellcheck: true,"+
               "autocorrect: true,"+
               "autofocus: true,"+
               "gutter: true,"+
               "screenReaderLabel: 'HTML Source Code Editor'});"+
               "setTimeout( editor12.refresh, 0 );"+
               "</script><br />"+
               "<label accesskey='' for='perc_template_metadata_code_insert_after_body_start'>Code insert after body start:</label> <br/> " +
               "<textarea id='perc_template_metadata_code_insert_after_body_start' name='perc_template_metadata_code_insert_after_body_start' wrap='soft' class='datadisplay' aarenderer='MODAL' requirescleanup='no' codeeditor='yes' codeeditor_mode='html'></textarea> <br />" +
               "<script>"+
               "var editor13 = CodeMirror.fromTextArea(document.getElementById('perc_template_metadata_code_insert_after_body_start'), {"+
               "mode: 'xml',"+
               "htmlMode: true,"+
               "showCursorWhenSelecting: true,"+
               "theme: 'default',"+
               "lineNumbers: true,"+
               "height: 'auto',"+
               "lineWrapping: false,"+
               "indentUnit: 2,"+
               "tabSize: 2,"+
               "indentWithTabs: true,"+
               "matchBrackets: true,"+
               "saveCursorPosition: true,"+
               "styleActiveLine: true,"+
               "spellcheck: true,"+
               "autocorrect: true,"+
               "autofocus: true,"+
               "screenReaderLabel: 'HTML Source Code Editor'});"+
               "editor13.refresh();"+
               "</script><br />"+
               "<label accesskey='' for='perc_template_metadata_code_insert_before_body_close'>Code insert before body close:</label> <br/> " +
               "<textarea id='perc_template_metadata_code_insert_before_body_close' name='perc_template_metadata_code_insert_before_body_close' wrap='soft' class='datadisplay' aarenderer='MODAL' requirescleanup='no' codeeditor='yes' codeeditor_mode='html'></textarea> " +
               "<script>"+
               "var editor14 = CodeMirror.fromTextArea(document.getElementById('perc_template_metadata_code_insert_before_body_close'), {"+
               "mode: 'xml',"+
               "htmlMode: true,"+
               "showCursorWhenSelecting: true,"+
               "theme: 'default',"+
               "lineNumbers: true,"+
               "height: 'auto',"+
               "lineWrapping: false,"+
               "indentUnit: 2,"+
               "tabSize: 2,"+
               "indentWithTabs: true,"+
               "matchBrackets: true,"+
               "saveCursorPosition: true,"+
               "styleActiveLine: true,"+
               "spellcheck: true,"+
               "autocorrect: true,"+
               "autofocus: true,"+
               "screenReaderLabel: 'HTML Source Code Editor'});"+
               "editor14.refresh();"+
               "</script><br />"+
               "</div></div>"+
               getSecueSectionFieldGroups() +
               "</div></div>").perc_dialog( {
         title: I18N.message("perc.ui.template.metadata.dialog@Template Metadata"),
         modal: true,
         width: 800,
	 height: 'auto',
	 //autoOpen:false,
	 open: function(){ setMetaContent(templateId); },
	 percButtons:	{
	 	"Save":	{
			click: function()	{return true;},
			id: "perc_template_metadata-save-button"
		},
	 	"Cancel":	{
			click: function()	{return true;},
			id: "perc_template_metadata-cancel-button"
		}
	  },
	id:"perc-template-metadata-dialog"
	     
	     } );
         
      function setMetaContent(templateId)
      {
		 $.PercSiteTemplatesController(false).loadTemplateMetadata(templateId, function(metadataObj){         
			var hc = metadataObj.HtmlMetadata.additionalHeadContent?metadataObj.HtmlMetadata.additionalHeadContent:"";
			var bb = metadataObj.HtmlMetadata.beforeBodyCloseContent?metadataObj.HtmlMetadata.beforeBodyCloseContent:"";
			var ab = metadataObj.HtmlMetadata.afterBodyStartContent?metadataObj.HtmlMetadata.afterBodyStartContent:"";
            var pr = metadataObj.HtmlMetadata.protectedRegion?metadataObj.HtmlMetadata.protectedRegion:"";
            var prt = metadataObj.HtmlMetadata.protectedRegionText?metadataObj.HtmlMetadata.protectedRegionText:"";
            var getDocValue = metadataObj.HtmlMetadata.docType.options;
            doctypeSelected = metadataObj.HtmlMetadata.docType.selected;
            
            for(i=0; i< getDocValue.length; i++) {
                if(getDocValue[i].option == "xhtml")
                {
                    doctypeXHTML = getDocValue[i].value;
                }
                else if(getDocValue[i].option == "html5")
                {
                    doctypeHTML5 = getDocValue[i].value;
                }
                else if (getDocValue[i].option == "custom")
                {
                    doctypeCustom = getDocValue[i].value;
                }
            }
            
			$("#perc_template_metadata_additional_head_content").val(hc);
			$("#perc_template_metadata_code_insert_after_body_start").val(ab);
			$("#perc_template_metadata_code_insert_before_body_close").val(bb);
            $("#perc_template_metadata_protected_region").val(pr);
            $("#perc_template_metadata_custom_doctype").val(doctypeHTML5);
            
            if (prt == "")
            {
                //TODO: I18N below?
            	prt = "Please sign in";
            }
            $("#perc_template_metadata_protected_region_text").val(prt);
            $("input[value='" +doctypeSelected + "']").trigger("click");
		});
		setButtonHandlers(templateId);
      }
	  
      function saveMetadata(templateId)
      {
	    //var additionalHeadContent = document.getElementById("perc_template_metadata_additional_head_content");
		//var afterBodyStartContent = document.getElementById("perc_template_metadata_code_insert_after_body_start");
		//var beforeBodyCloseContent = document.getElementById("perc_template_metadata_code_insert_before_body_close");
		
		//var additionalHeadContent = $("#perc_template_metadata_additional_head_content");
		//var afterBodyStartContent = $("#perc_template_metadata_code_insert_after_body_start");
		//var beforeBodyCloseContent = $("#perc_template_metadata_code_insert_before_body_close");
        
         var metadataObj = {"HtmlMetadata" :{  
                                            "additionalHeadContent": $("#perc_template_metadata_additional_head_content").val(), 
                                            "afterBodyStartContent": $("#perc_template_metadata_code_insert_after_body_start").val(),
                                            "beforeBodyCloseContent": $("#perc_template_metadata_code_insert_before_body_close").val(),
                                            "id" : templateId,
                                            "protectedRegion" : $("#perc_template_metadata_protected_region").val(),
                                            "protectedRegionText":$("#perc_template_metadata_protected_region_text").val(),
                                            "docType":{
                                                        "selected":$('input[name="doctype"]:checked').val(),    
                                                        "options":[
                                                                    {"option":"xhtml","value":"PERC_XHTML"},
                                                                    {"option":"html5","value":"PERC_HTML5"},
                                                                    {"option":"custom","value":$("#perc_template_metadata_custom_doctype").val()}
                                                            ]                                    
                                             }
                                          }                       
                        };
                      
        $.PercSiteTemplatesController(false).saveTemplateMetadata( metadataObj, function() {});            
        dialog.remove();
      }

      function setButtonHandlers(templateId)
      {
		$('#perc_template_metadata-save-button').on("click",function()	{saveMetadata(templateId);});
		$('#perc_template_metadata-cancel-button').on("click",function() { dialog.remove(); });
      }
	  
	  function getSecueSectionFieldGroups(){
        var fieldGroups = '';

        fieldGroups = "<div class = 'fieldGroup perc-last-section'><div class = 'perc-section-header'><div class = 'perc-section-label' groupname = 'perc-section-protected-container'>" +
            "<span class = 'perc-min-max perc-items-maximizer'></span>Protected Regions</div></div>" +
            "<div id = 'perc-section-protected-container' style='display: none'>" +
            "<label for='perc_template_metadata_protected_region'>Protected region name:</label><br/>" +
            "<input id='perc_template_metadata_protected_region' name='perc_template_metadata_protected_region' maxlength='200' type=''/><br/>" +
            "<label for='perc_template_metadata_protected_region_text'>Protected region default text:</label><br/>" +
            "<input id='perc_template_metadata_protected_region_text' name='perc_template_metadata_protected_region_text' maxlength='200' type='text' />" +
            "</div></div>";
        return fieldGroups;
     }

      
        // Bind collapsible event
        dialog.find(".perc-section-label").off("click").on("click",function() {
            var self = $(this);
            self.find(".perc-min-max")
                .toggleClass('perc-items-minimizer')
                .toggleClass('perc-items-maximizer');
            dialog.find('#' + self.attr('groupName')).toggle();
        });

        //Bind the click on radio button
        dialog.find("input[name='doctype']").on("click", function() {
            var textarea = $("#perc_template_metadata_custom_doctype");
            if($(this).attr('value') === "html5") {
                textarea.val(doctypeHTML5).attr('readonly', 'readonly').addClass('perc-readOnlyText');
            }
            else if($(this).attr('value') === "xhtml") {
                textarea.val(doctypeXHTML).attr('readonly', 'readonly').addClass('perc-readOnlyText');
            }
            else if($(this).attr('value') === "custom") {
                textarea.removeAttr('readonly').removeClass('perc-readOnlyText');
                if(doctypeCustom !== "" && doctypeCustom != null) {
                    textarea.val(doctypeCustom);
                }
            }
        });       
   };   

 })(jQuery);
