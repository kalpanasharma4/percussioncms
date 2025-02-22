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

package com.percussion.soln.listbuilder;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An instance of a list builder usually created
 * from an content node.
 * 
 * @author adamgent
 *
 */
public class ListBuilderItem {

    private String dateRangeStart;
    private String dateRangeEnd;
    private String titleContains;
    private String contentType;
    private String slot;
    private String childSnippet;
    private String jcrQuery;
    private String folderPath;
    
    private Collection<String> folderPaths = new ArrayList<>();
    private Collection<String> contentTypes = new ArrayList<>();
    private Long count;

    
    
    public String getFolderPath() {
        return folderPath;
    }

    
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Collection<String> getContentTypes() {
        return contentTypes;
    }
    
    public void setContentTypes(Collection<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public Collection<String> getFolderPaths() {
        return folderPaths;
    }

    public void setFolderPaths(Collection<String> folderPaths) {
        this.folderPaths = folderPaths;
    }

    public String getDateRangeStart() {
        return dateRangeStart;
    }
    
    public void setDateRangeStart(String dateRangeStart) {
        this.dateRangeStart = dateRangeStart;
    }
    
    public String getDateRangeEnd() {
        return dateRangeEnd;
    }
    
    public void setDateRangeEnd(String dateRangeEnd) {
        this.dateRangeEnd = dateRangeEnd;
    }
    
    public String getTitleContains() {
        return titleContains;
    }
    
    public void setTitleContains(String titleContains) {
        this.titleContains = titleContains;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getSlot() {
        return slot;
    }
    
    public void setSlot(String slot) {
        this.slot = slot;
    }
    
    public String getChildSnippet() {
        return childSnippet;
    }
    
    public void setChildSnippet(String childSnippet) {
        this.childSnippet = childSnippet;
    }
    
    public String getJcrQuery() {
        return jcrQuery;
    }
    
    public void setJcrQuery(String jcrQuery) {
        this.jcrQuery = jcrQuery;
    }

    
    public Long getCount() {
        return count;
    }

    
    public void setCount(Long count) {
        this.count = count;
    }
    
}
