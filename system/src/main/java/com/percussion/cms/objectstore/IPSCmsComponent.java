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
package com.percussion.cms.objectstore;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This interface must be implemented by all objects used in the CMS layer
 * that need to be serialized to/from xml. These objects are not necessarily
 * persisted to the database, except as part of some other component.
 * Components that are persisted must implement the {@link IPSDbComponent}
 * interface.
 */
public interface IPSCmsComponent extends Cloneable
{
   /**
    * This method is called to create an XML element node with the
    * appropriate format for this object. An element node may contain a
    * hierarchical structure, including child objects. The element node can
    * also be a child of another element node.
    * <p>All derived classes implementing this method must document their
    * dtd in the javadoc for this method.
    * <p>If a base class implements this method, the derived class should,
    * but doesn't have to, call the base class to perform the serialization.
    * If the base class doesn't implement this method, the derived class is
    * responsible for storing the base class state. Whatever technique is used,
    * it should be symmetrical w/ the <code>fromXml</code> method, meaning
    * both methods must call the base class or not.
    * <p>The name of the element returned should be the same name returned
    * by the <code>getNodeName</code> method.
    *
    * @param doc - the document from which the element node will be created.
    *    Never <code>null</code>.
    *
    * @return - the newly created XML element node.  Never <code>null</code>.
    */
   public Element toXml(Document doc);


   /**
    * The name of the element returned by the <code>toXml</code> and
    * expected by the <code>fromXml</code> methods. Typically, this should
    * be of the form: if the base class name is 'PSFoo', then this method
    * should return 'PSXFoo'.
    *
    * @return A name valid for an xml element name. Never empty or
    *    <code>null</code>.
    */
   public String getNodeName();


   /**
    * This method is called to recreate a previously persisted instance. The
    * supplied xml node was typically generated by a previous call to the
    * {@link #toXml(Document) toXml} method. Say you have 2 instances of
    * this class, A and B. After calling B.fromXml(A.toXml), it is guaranteed
    * that A.equals(B) will return <code>true</code>.
    * <p>If a base class implements this method, the derived class should,
    * but doesn't have to, call the base class to perform the de-serialization.
    * If the base class doesn't implement this method, the derived class is
    * responsible for restoring the base class (in this case the base class
    * must have methods allowing the derived class to properly restore the
    * state). Whatever technique is used, it should be symmetrical w/ the
    * <code>toXml</code> method, meaning both methods must call the base
    * class or not.
    * <p>The name of the element supplied should be the same name returned
    * by the <code>getNodeName</code> method.
    * <p>Every class implementing this interface must also create a single
    * arg ctor that takes an Element. The implementation of this ctor should
    * call super(elem) followed by fromXml(elem).
    *
    * @param src The node to extract the data for this instance. Never
    *    <code>null</code>. See {@link #toXml(Document) toXml} for the
    *    required format of the supplied element.
    *
    * @throws PSUnknownNodeTypeException If the name of the supplied element
    *    is incorrect or any of its attributes or children are required but
    *    missing or incorrect.
    */
   public void fromXml(Element src)
      throws PSUnknownNodeTypeException;


   /**
    * Creates a new instance of this object, deep copying all member variables
    * unless those members are physically or logically immutable, in which
    * case they are not cloned. The javadoc for such classes must indicate
    * this fact.
    *
    * @return a deep-copy clone of this instance, never <code>null</code>.
    */
   public Object clone();


   /**
    * Indicates whether some other object is "equal to" this one.
    * All implementing classes must override this method as the default
    * behavior is incorrect for deep clones. If the implementation does not
    * follow the contract documented for this method in
    * {@link Object#equals(Object) Object}, then it must clearly state how it
    * deviates from that contract. The following algorithm should be used:
    * <ol>
    *    <li>Call the base class (unless Object) and if <code>true</code>
    *       is returned, continue.</li>
    *    <li>Compare all members of current class.</li>
    *    <li>If all members are equal, return <code>true</code>.</li>
    * </ol>
    *
    * @param obj the reference object with which to compare. May be
    *    <code>null</code>;
    *
    * @return <code>true</code> if this object is logically equivalent to the
    *    supplied obj; <code>false</code> otherwise. If <code>null</code>
    *    or not an instance of this class, <code>false</code> is returned.
    */
   public boolean equals(Object obj);


   /**
    * Must be overridden to fulfill contract of this method as described in
    * Object. The following method should be used to calculate the hash code.
    * <ol>
    *    <li>Get the hashcode from the base class (unless the base class is
    *       Object).</li>
    *    <li>Add this hash code to the sum of all hash codes obtained from
    *       all non-native types.</li>
    *    <li>Add to this result the hash code obtained by creating a
    *       concatenated string of all native types and taking the hash code
    *       of that.</li>
    * </ol>
    *
    * @return A hash code value for this object that will work correctly for
    *    maps.
    */
   public int hashCode();
}
