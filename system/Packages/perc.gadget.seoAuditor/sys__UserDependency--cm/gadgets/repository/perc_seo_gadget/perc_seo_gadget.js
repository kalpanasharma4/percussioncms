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

(function($)
{
    // number of rows per page
    var itemsPerPage = 5;
    var TABLE_STATUS_FOOTER_PADDING_TOP = 5;

    // grab necessary Perc APIs
    var PercPageService = percJQuery.PercPageService;
    var PercServiceUtils = percJQuery.PercServiceUtils;
    var perc_utils = percJQuery.perc_utils;
    var perc_paths = percJQuery.perc_paths;
    var severityText = ["Good", "Moderate", "Medium", "High", "Severe"];
    var issueMessages = {
        "DEFAULT_TITLE": I18N.message("perc.ui.gadgets.seo@Default Title"),
        "MISSING_DESCRIPTION": I18N.message("perc.ui.gadgets.seo@Missing Description"),
        "MISSING_KEYWORD_DESCRIPTION": I18N.message("perc.ui.gadgets.seo@Missing Keyword Description"),
        "MISSING_KEYWORD_LINK": I18N.message("perc.ui.gadgets.seo@Missing Keyword Link"),
        "MISSING_KEYWORD_TITLE": I18N.message("perc.ui.gadgets.seo@Missing Keyword Title"),
        "TITLE_TOO_LONG": I18N.message("perc.ui.gadgets.seo@Title Too Long"),
        "TEST": "Very Long Test message",
        "DESCRIPTION_TOO_LONG": I18N.message("perc.ui.gadgets.seo@Description Too Long")
    };

    var isLargeColumn = true; // if gadget is on the right side (large column)
    var statusTable = undefined;
    var tableDiv;

    // API for this library
    $.fn.PercSeoGadget = function(site, workflow, state, keyword, severity, rows)
    {
        // setup custom sorting function for datatable plugins
        declareCustomSortingFunctions();

        // never show a scrollbar in the gadget
        $("body").css("overflow", "hidden");
        // resize gadget to fit the rows
        itemsPerPage = rows;

        tableDiv = $(this);

        if (site == null) site = "";
        loadGadget(site, workflow, state, keyword, severity);
    };

    /**
     * Calculate the severity integer that will be used as an index of the array "severityText" to
     * get the severity title and image representations that goes in the severity column.
     * @param Number severity Numerical representation of the severity
     */
    function getSeverityInt(severity)
    {
        // We will use the same algorithim as seen in "PSSEOStatistics.java", in the
        // method "getSeverityLevel"
        if (severity == 100)
        {
            return 4;
        }
        else if (severity >= 75)
        {
            return 3;
        }
        else if (severity >= 50)
        {
            return 2;
        }
        else if (severity >= 25)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Retrieves pages from a site that are in a particular status and then renders them as a table
     * @param site (string) site pages are in. Empty string means all sites.
     * @param workflow (string) workflow of pages.
     * @param status (string) status of pages. Empty string means any status.
     */
    function loadGadget(site, workflow, status, keyword, severity)
    {
        // get the data and then pass it to createStatusTable to create the table
        PercPageService.getNonSEOPages(
            perc_paths.SITES_ROOT + '/' + site,
            workflow,
            status,
            severity,
            keyword,
            function(status, data)
            {
                if (status == PercServiceUtils.STATUS_SUCCESS)
                {
                    createStatusTable(data);
                }
                else
                {
                    displayErrorMessage(data);
                }
            }
        );
    }

    function createStatusTable(data)
    {
        // iterate over the data
        var pageStats = data.SEOStatistics;
        var itemCount = pageStats.length;
        var percRows = [];

        for (i = 0; i < pageStats.length; i++)
        {
            // get each page's stats
            var stats = pageStats[i];

            var pageSummary = stats.pageSummary;
            var linkTitle = htmlEntities(pageSummary.linkTitle);
            var summary = stats.summary;
            if (summary) summary = $(summary).text();
            else summary = "&nbsp;";

            var title = htmlEntities(pageSummary.title);
            title = [
                {
                    content: title,
                    title: title
                }
            ]; //"<div class='data-cell'><div class='top-line perc-ellipsis' title='"+ title +"' >"+title+"</div></div>";

            var firstColumn = [
                {
                    content: linkTitle,
                    title: stats.path
                }, {
                    content: summary,
                    title: summary
                }
            ]; //"<div class='data-cell'><div class='perc-datatable-firstrow perc-ellipsis' title='"+linkTitle+"'>"+linkTitle+"</div><div class='perc-datatable-secondrow perc-ellipsis' title='"+ stats.path +"'>"+stats.path+"</div></div>";
            var issueCodes = stats.issues;
            var issuesMessage = "";
            var issuesMessageAll = "";
            var issuesMessageTitle = "";
            var issuesMessagePopup = "";
            var issueText = "";
            // if it's just one code then JSON is a string but we always want an array
            if (typeof(issueCodes) != "object")
            {
                issueCodes = [issueCodes];
            }

            var issues = [];
            for (let issueCode of issueCodes)
            {

                if (typeof(issueCode) === 'undefined') break;
                issueText = issueMessages[issueCode];
                if (issues.length < 2) issues.push(
                    {
                        content: issueText,
                        title: issueText
                    });
                issuesMessageTitle += issueText + "; ";
            }

            // Append the empty character to fix empty cells issue with IE and FF
            if (issues.length == 0)
            {
                issues.push('&nbsp;');
            }

            issuesMessageTitle = issuesMessageTitle.substring(0, issuesMessageTitle.length - 2);

            var severity = Math.round(6 * stats.severity / 100);
            var sevText = severityText[getSeverityInt(stats.severity)];
            var imageName = "perc-meter-" + sevText.toLowerCase() + ".png";

            if ($.browser.mozilla) issuesMessageTitle = sevText + "; " + issuesMessageTitle;
            else issuesMessageTitle = sevText + "&#xD;" + issuesMessageTitle;
            var toolTip = issuesMessageTitle;

            imageName = "<img src='/cm/gadgets/repository/common/images/" + imageName + "' data-severity='" + stats.severity + "' alt='" + toolTip + "'>";

            var callbackInfo = {
                pageId: pageSummary.id + "",
                pagePath: stats.path + ""
            };
            var percRow = {
                rowContent: [firstColumn, title, issues, imageName],
                rowData: {
                    pageId: pageSummary.id,
                    pagePath: stats.path
                }
            };
            percRows.push(percRow);
        }

        var aoColumns = [
            {
                sType: "string"
            }, {
                sType: "string"
            }, {
                sType: "string"
            }, {
                sType: "html-img"
            }
        ];

        isLargeColumn = gadgets.window.getDashboardColumn() == 1;
        var percVisibleColumns = null;
        if (!isLargeColumn) percVisibleColumns = [0, 3];

        var headers = [I18N.message("perc.ui.publish.title@Title"), I18N.message("perc.ui.seo.gadget@Title Tag"), I18N.message("perc.ui.seo.gadget@Issues"), I18N.message("perc.ui.seo.gadget@Severity")];

        var columnWidths = ["*", "103", "140", "70"];

        var tableConfig = {
            percColumnWidths: columnWidths,
            percData: percRows,
            percHeaders: headers,
            iDisplayLength: itemsPerPage,
            aoColumns: aoColumns,
            percVisibleColumns: percVisibleColumns
        };
        tableDiv.PercPageDataTable(tableConfig);

        miniMsg.dismissMessage(loadingMsg);
    }

    function displayErrorMessage(message)
    {
        tableDiv.append("<div class='perc-gadget-errormessage'>" + message + "</div>");
        miniMsg.dismissMessage(loadingMsg);
    }

    function declareCustomSortingFunctions()
    {

        //CMS-8495 : Sorting the severity column issue on seo gadget.
        $.fn.dataTableExt.afnSortData['perc-type-html-img'] = function(oSettings, iColumn)
        {
            var aData = [];

            this.api().column( iColumn, {order:'index'} ).nodes().map( function ( td, iColumn ) {
                var img = $($(td)[0]).find('img');
                aData.push($(img[0]).attr('data-severity'));
            } );

            return aData;
        };

        $.fn.dataTableExt.oSort['html-img-asc'] = function(x, y)
        {
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        };
        $.fn.dataTableExt.oSort['html-img-desc'] = function(x, y)
        {
            return ((x < y) ? 1 : ((x > y) ? -1 : 0));
        };
    }

})(jQuery);
