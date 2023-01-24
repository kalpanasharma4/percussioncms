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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-661 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.04.26 at 02:27:24 PM ACT 
//


package com.percussion.delivery.data;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.percussion.delivery.data package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.percussion.delivery.data
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.CommentService.Comments }
     * 
     */
    public DeliveryServicesContent.CommentService.Comments createDeliveryServicesContentCommentServiceComments() {
        return new DeliveryServicesContent.CommentService.Comments();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.MembershipService }
     * 
     */
    public DeliveryServicesContent.MembershipService createDeliveryServicesContentMembershipService() {
        return new DeliveryServicesContent.MembershipService();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent }
     * 
     */
    public DeliveryServicesContent createDeliveryServicesContent() {
        return new DeliveryServicesContent();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.MembershipService.Memberships.Membership }
     * 
     */
    public DeliveryServicesContent.MembershipService.Memberships.Membership createDeliveryServicesContentMembershipServiceMembershipsMembership() {
        return new DeliveryServicesContent.MembershipService.Memberships.Membership();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.CommentService.Comments.Comment }
     * 
     */
    public DeliveryServicesContent.CommentService.Comments.Comment createDeliveryServicesContentCommentServiceCommentsComment() {
        return new DeliveryServicesContent.CommentService.Comments.Comment();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.CommentService }
     * 
     */
    public DeliveryServicesContent.CommentService createDeliveryServicesContentCommentService() {
        return new DeliveryServicesContent.CommentService();
    }

    /**
     * Create an instance of {@link DeliveryServicesContent.MembershipService.Memberships }
     * 
     */
    public DeliveryServicesContent.MembershipService.Memberships createDeliveryServicesContentMembershipServiceMemberships() {
        return new DeliveryServicesContent.MembershipService.Memberships();
    }

}
