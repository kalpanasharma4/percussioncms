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

package com.percussion.sitemanage.importer.utils;

public class PSFileDownloadJob
{
    String file;

    String url;

    Boolean createAsset;

    protected PSFileDownloadJob(String file, String url, Boolean createAsset)
    {
        this.file = file;
        this.url = url;
        this.createAsset = createAsset;
    }

    public String getFile()
    {
        return file;
    }

    public void setFile(String file)
    {
        this.file = file;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Boolean getCreateAsset()
    {
        return createAsset;
    }

    public void setCreateAsset(Boolean createAsset)
    {
        this.createAsset = createAsset;
    }
}
