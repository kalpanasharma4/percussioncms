

<!-- Template for widget fields -->
<script type="text/template" id="perc-widget-fields-collection-template">
    <div class="perc-widget-field-wrapper">
        <div class="perc-widget-field-entry" title="<@- name @>"><@- name @></div>
        <div class="perc-widget-field-entry" title="<@- label @>"><@- label @></div>
        <div class="perc-widget-field-entry"><@- type.toLowerCase().replace("_", " ") @></div>
        <div class="perc-widget-field-entry"><div class="perc-widget-field-actions" for="<@- name @>" ><span role="button" tabindex="128" title='<i18n:message key="perc.ui.widget.builder@Edit"/>' class="perc-widget-field-action-edit" style="cursor:pointer"><i18n:message key="perc.ui.widget.builder@Edit"/></span> | <span role="button" tabindex="129" title='<i18n:message key="perc.ui.widget.builder@Delete"/>' class="perc-widget-field-action-delete" style="cursor:pointer"><i18n:message key="perc.ui.widget.builder@Delete"/></span></div>
        </div>
</script>

<!-- Template for General tab of widget builder -->
<script type="text/template" id="perc-widget-general-tab-template">
    <csrf:form name="perc-widget-general-tab-form" action="templates.jsp" method="post">
        <div type="sys_normal">
            <label accesskey="" for="widgetname" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Name"/>:</label><br/>
            <input aria-required="true" tabindex="130" title='<i18n:message key="perc.ui.widget.builder@Name"/>' id="widgetname" <@- widgetname != ''?'readonly=readonly':'' @> <@- widgetname != ''?'class=perc-disabled-input':'class="datadisplay"' @> type="text" name="widgetname" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- widgetname @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="description"><i18n:message key="perc.ui.widget.builder@Description"/>:</label><br/>
            <textarea tabindex="131" title='<i18n:message key="perc.ui.widget.builder@Description"/>' id="description" name="description" wrap="soft" class="datadisplay" rows="4" cols="80" maxlength="1024"><@- description @></textarea>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="prefix" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Prefix"/>:</label><br/>
            <input aria-required="true" tabindex="132" title='<i18n:message key="perc.ui.widget.builder@Prefix"/>' type="text" id="prefix" <@- prefix != ''?'readonly=readonly':'' @> <@- prefix != ''?'class=perc-disabled-input':'class="datadisplay"' @> type="text" name="prefix" class="datadisplay" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- prefix @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="author" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Author"/>:</label><br/>
            <input aria-required="true" tabindex="133" title='<i18n:message key="perc.ui.widget.builder@Author"/>' type="text" id="author" name="author" class="datadisplay" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- author @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="publisherUrl" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Publisher URL"/>:</label><br/>
            <input aria-required="true" tabindex="134" title='<i18n:message key="perc.ui.widget.builder@Publisher URL"/>' type="text" id="publisherUrl" name="publisherUrl" class="datadisplay" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- publisherUrl @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="version" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Version"/>:</label><br/>
            <input aria-required="true" tabindex="135" title='<i18n:message key="perc.ui.widget.builder@Version"/>' type="text" id="version" name="version" class="datadisplay" size="50" maxlength="50" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- version @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="widgetTrayCustomizedIconPath" ><i18n:message key="perc.ui.widget.builder@Widget Tray Icon Path"/>:</label><br/>
            <input aria-required="false" tabindex="136" title='<i18n:message key="perc.ui.widget.builder@Widget Tray Icon Path"/>' type="text" id="widgetTrayCustomizedIconPath" name="widgetTrayCustomizedIconPath" class="datadisplay" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- widgetTrayCustomizedIconPath @>"/>
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="toolTipMessage" ><i18n:message key="perc.ui.widget.builder@ToolTip Message"/>:</label><br/>
            <input tabindex="137" title='<i18n:message key="perc.ui.widget.builder@ToolTip Message"/>' type="text" id="toolTipMessage" name="toolTipMessage" class="datadisplay" size="50" maxlength="100" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- toolTipMessage @>"/>
            <br/>
        </div>
        <div type="sys_normal">
		    <input tabindex="138" title='<i18n:message key="perc.ui.widget.builder@Is Responsive"/>'  type="checkbox" id="isResponsive" name="responsive" class="datadisplay" style="height: 15px; padding-top: 3px; padding-bottom: 5px; vertical-align:middle" <@ print(responsive==true?checked='checked':'') @>/><label accesskey="" for="isResponsive"><i18n:message key="perc.ui.widget.builder@Is Responsive"/></label>
            <br/>
        </div>
    </csrf:form>
</script>

<!-- Template for field editor dialog of widget builder -->
<script type="text/template" id="perc-widget-field-editor-template">
    <csrf:form name="perc-widget-field-editor-form" action="templates.jsp" method="post">
        <div type="sys_normal">
            <label accesskey="" for="name" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Name"/>:</label><br/>
            <input aria-required="true" id="name" <@- name != ''?'readonly=readonly':'' @> <@- name != ''?'class=perc-disabled-input':'class="datadisplay"' @> type="text" name="name" size="50" maxlength="50" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- name @>">
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="label" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Label"/>:</label><br/>
            <input aria-required="true" id = "label" type="text" name="label" class="datadisplay" size="50" maxlength="50" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- label @>">
            <br/>
        </div>
        <div type="sys_normal">
            <label accesskey="" for="type" class="perc-required-field"><i18n:message key="perc.ui.widget.builder@Type"/>:</label><br/>
            <@ if(name == '') { @>
            <select aria-required="true" id="type" class="datadisplay" name="type">
                <option <@- type == 'TEXT'?'selected="selected"':''@> value="TEXT"><i18n:message key="perc.ui.widget.builder@Text"/></option>
                <option <@- type == 'RICH_TEXT'?'selected="selected"':''@> value="RICH_TEXT"><i18n:message key="perc.ui.widget.builder@Rich Text"/></option>
                <option <@- type == 'DATE'?'selected="selected"':''@> value="DATE"><i18n:message key="perc.ui.widget.builder@Date"/></option>
                <option <@- type == 'TEXT_AREA'?'selected="selected"':''@> value="TEXT_AREA"><i18n:message key="perc.ui.widget.builder@Textarea"/></option>
                <option <@- type == 'FILE'?'selected="selected"':''@> value="FILE"><i18n:message key="perc.ui.widget.builder@File"/></option>
                <option <@- type == 'IMAGE'?'selected="selected"':''@> value="IMAGE"><i18n:message key="perc.ui.widget.builder@Image"/></option>
                <option <@- type == 'PAGE'?'selected="selected"':''@> value="PAGE"><i18n:message key="perc.ui.widget.builder@Page"/></option>
            </select>
            <@ } else {@>
            <!-- top input field contains the actual value, the bottom is for display purposes only to handle localization -->
            <input value="<@ if(type == 'TEXT'){ @>text<@ }else if(type == 'RICH_TEXT'){ @>rich text<@ }else if(type == 'DATE'){ @>date<@ }else if(type == 'TEXT_AREA'){ @>textarea<@ }else if(type == 'FILE'){ @>file<@ }else if(type == 'IMAGE'){ @>image<@ }else if(type == 'PAGE'){ @>page<@ } @>" readonly="readonly" class="perc-disabled-input" type="hidden" name="type" size="50" maxlength="255" style="height: 15px; padding-top: 3px; padding-bottom: 5px;">
            <input value="<@ if(type == 'TEXT'){ @><i18n:message key="perc.ui.widget.builder@Text"/><@ }else if(type == 'RICH_TEXT'){ @><i18n:message key="perc.ui.widget.builder@Rich Text"/><@ }else if(type == 'DATE'){ @><i18n:message key="perc.ui.widget.builder@Date"/><@ }else if(type == 'TEXT_AREA'){ @><i18n:message key="perc.ui.widget.builder@Textarea"/><@ }else if(type == 'FILE'){ @><i18n:message key="perc.ui.widget.builder@File"/><@ }else if(type == 'IMAGE'){ @><i18n:message key="perc.ui.widget.builder@Image"/><@ }else if(type == 'PAGE'){ @><i18n:message key="perc.ui.widget.builder@Page"/><@ } @>" readonly="readonly" class="perc-disabled-input" type="text" name="type-display" size="50" maxlength="255" style="height: 15px; padding-top: 3px; padding-bottom: 5px;">
            <@ } @>
            <br/>
        </div>
    </csrf:form>
</script>

<!-- Template for display tab of widget builder -->
<script type="text/template" id="perc-widget-display-editor-template">
<csrf:form name="perc-widget-display-tab-form" action="templates.jsp" method="post">
    <div type="sys_normal">
	    <textarea id="widgetHtml" title='<i18n:message key = "perc.ui.publishing.history@DisplayHTML"/>' name="widgetHtml" wrap="soft" class="datadisplay" rows="25" cols="110"><@- widgetHtml @></textarea>
        <br/>
        <script>
            var editor = CodeMirror.fromTextArea(document.getElementById('widgetHtml'), {
                mode: 'htmlmixed',
                theme: 'default',
                lineNumbers: true,
                lineWrapping: true,
                indentUnit: 2,
                tabSize: 2,
                indentWithTabs: true,
                matchBrackets: true,
                saveCursorPosition: true,
                styleActiveLine: true,
                spellcheck: true,
                autocorrect: true,
                autofocus: true,
                screenReaderLabel: 'Widget Source Code Editor'});
    </script>
    </div>
</csrf:form>
</script>

<!-- Template for display tab of widget builder -->
<script type="text/template" id="perc-widget-resource-item-editor-template">
    <csrf:form action="templates.jsp" method="post">
        <div type="sys_normal" class="perc-widget-resource">
            <input type="text" class="datadisplay perc-resource-entry-field" size="50" maxlength="255" style="height: 15px; padding-top: 3px; padding-bottom: 5px;" value="<@- name @>"><span class="perc-resource-delete perc-font-icon resource-tab-button-background icon-remove fas fa-times" title='<i18n:message key="perc.ui.widget.builder@Remove Resource"/>'></span>
            <br/>
        </div>
    </csrf:form>
</script>
