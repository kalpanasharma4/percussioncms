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
package com.percussion.delivery.forms.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Simple container. Its use is just to add a root element name for Jersey to
 * spit out when serializing to JSON.
 * 
 * @author leonardohildt
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder =
{"formsInfo"})
public class PSFormSummaries
{
    private List<PSFormSummary> formsInfo = new ArrayList<>();

    public List<PSFormSummary> getSummaries()
    {
        if (formsInfo == null)
            formsInfo = new ArrayList<>();
        return formsInfo;
    }

    public void setSummaries(List<PSFormSummary> formSummaries)
    {
        this.formsInfo = formSummaries;
    }
}
