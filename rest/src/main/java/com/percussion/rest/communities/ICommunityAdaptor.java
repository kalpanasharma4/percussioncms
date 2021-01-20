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
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.rest.communities;

import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.webservices.PSErrorResultsException;

import java.rmi.RemoteException;
import java.util.List;

public interface ICommunityAdaptor {

    public CommunityList createCommunities(List<String> names);
    public CommunityList findCommunities(String name);
    public CommunityList loadCommunities(GuidList ids, boolean lock, boolean overrideLock) throws PSErrorResultsException;
    public void saveCommunities(CommunityList communities, boolean release);
    public void deleteCommunities(GuidList ids, boolean ignoreDependencies);
    public CommunityVisibilityList getVisibilityByCommunity(
            GuidList ids, ObjectTypeEnum type) throws PSErrorResultsException, RemoteException;
    public void switchCommunity(String name);
}
