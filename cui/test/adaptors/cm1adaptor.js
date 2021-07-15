/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

// assumed dependencies:
//  - require.js

define
(
    [
       'jquery',
       'test/adaptors/cm1testdata'
    ],
    function ($, testData) {
        function openLibrary(){
            window.parent.location.search = "?view=home&initialScreen=library";
        }
        function openItem(path){
        }
        function previewItem(path){
            
        }
        function copyItem(path){
            
        }
        function deleteItem(path){
            
        }
        function bookMarkItem(path){
            
        }
        function recentList(type, site){
            var deferred = $.Deferred();
            deferred.resolve(testData.recentItems);
            return deferred.promise();
        }
        function myContent(){
            var deferred = $.Deferred();
            deferred.resolve(testData.myContentItems);
            return deferred.promise();
        }
        var api = {
            openLibrary : openLibrary,
            openItem : openItem,
            previewItem : previewItem,
            copyItem : copyItem,
            bookMarkItem : bookMarkItem,
            recentList : recentList,
            myContent : myContent
        };
        return api;
    }
);
