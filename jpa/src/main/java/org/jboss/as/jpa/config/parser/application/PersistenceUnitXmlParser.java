/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.config.parser.application;

import org.jboss.as.jpa.config.PersistenceMetadata;
import org.jboss.metadata.parser.util.MetaDataElementParser;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Parse a persistence.xml into a list of persistence unit definitions.
 *
 * NOTE:  that the PU classloaders are not set and need to be set by the caller or something else.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitXmlParser extends MetaDataElementParser {

   public static List<PersistenceMetadata> parse(final XMLStreamReader reader) throws XMLStreamException {

      reader.require(START_DOCUMENT, null, null);  // check for a bogus document and throw error

      // Read until the first start element
      Version version = null;
      while (reader.hasNext() && reader.next() != START_ELEMENT) {
         if (reader.getEventType() == DTD) {
            final String dtdLocation = readDTDLocation(reader);
            if (dtdLocation != null) {
               version = Version.forLocation(dtdLocation);
            }
         }
      }
      final String schemaLocation = readSchemaLocation(reader);
      if (schemaLocation != null) {
         version = Version.forLocation(schemaLocation);
      }
      if (version == null || Version.UNKNOWN.equals(version)) {
         // Look at the version attribute
         String versionString = null;
         final int count = reader.getAttributeCount();
         for (int i = 0; i < count; i++) {
            if (reader.getAttributeNamespace(i) != null) {
               continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            if (attribute == Attribute.VERSION) {
               versionString = reader.getAttributeValue(i);
            }
         }
         if ("1.0".equals(versionString)) {
            version = Version.JPA_1_0;
         } else if ("1".equals(versionString)) {
            version = Version.JPA_1_0;
         } else if ("2.0".equals(versionString)) {
            version = Version.JPA_2_0;
         } else if ("2".equals(versionString)) {
            version = Version.JPA_2_0;
         }
      }

      final int count = reader.getAttributeCount();
      for (int i = 0; i < count; i++) {
         final String value = reader.getAttributeValue(i);
         if (reader.getAttributeNamespace(i) != null) {
            continue;
         }
         final Element attribute = Element.forName(reader.getAttributeLocalName(i));
         switch (attribute) {
            case VERSION:
               System.out.println("version = " + value);
               break;
            default:
               throw unexpectedAttribute(reader, i);
         }
      }
      final List<PersistenceMetadata> PUs = new ArrayList<PersistenceMetadata>();
      // until the ending PERSISTENCE tag
      while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
         final Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case PERSISTENCEUNIT:
               PersistenceMetadata pu = parsePU(reader, version);
               PUs.add(pu);
               break;

            default:
               throw unexpectedElement(reader);
         }
      }


      return PUs;
   }

   /**
    * Parse the persistence unit definitions based on persistence_2_0.xsd.
    * @param reader
    * @return
    * @throws XMLStreamException
    */
   private static PersistenceMetadata parsePU(XMLStreamReader reader, Version version) throws XMLStreamException {
      PersistenceMetadata pu = new PersistenceMetadata();
      List<String> classes = new ArrayList<String>(1);
      List<String> jarfiles = new ArrayList<String>(1);
      List<String> mappingFiles = new ArrayList<String>(1);
      Properties properties = new Properties();

      // set defaults
      pu.setTransactionType(PersistenceUnitTransactionType.JTA);
      pu.setValidationMode(ValidationMode.AUTO);
      pu.setSharedCacheMode(SharedCacheMode.UNSPECIFIED);

      final int count = reader.getAttributeCount();
      for (int i = 0; i < count; i++) {
         final String value = reader.getAttributeValue(i);
         if (reader.getAttributeNamespace(i) != null) {
            continue;
         }
         final Element attribute = Element.forName(reader.getAttributeLocalName(i));
         switch (attribute) {
            case NAME:
               pu.setPersistenceUnitName(value);
               break;
            case TRANSACTIONTYPE:
               if (value.equalsIgnoreCase("RESOURCE_LOCAL"))
                  pu.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
               break;
            default:
               throw unexpectedAttribute(reader, i);
         }
      }

      // until the ending PERSISTENCEUNIT tag
      while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
         final Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case CLASS:
               classes.add(reader.getElementText());
               break;

            case DESCRIPTION:
               final String description = reader.getElementText();
               break;

            case EXCLUDEUNLISTEDCLASSES:
               pu.setExcludeUnlistedClasses(Boolean.getBoolean(reader.getElementText()));
               break;

            case JARFILE:
               String file = reader.getElementText();
               jarfiles.add(file);
               break;

            case JTADATASOURCE:
               pu.setJtaDataSource(reader.getElementText());
               break;

            case NONJTADATASOURCE:
               pu.setNonJtaDataSource(reader.getElementText());
               break;

            case MAPPINGFILE:
               mappingFiles.add(reader.getElementText());
               break;

            case PROPERTIES:
               parseProperties(reader, properties);
               break;

            case PROVIDER:
               pu.setPersistenceProviderClassName(reader.getElementText());
               break;

            case SHAREDCACHEMODE:
               String cm = reader.getElementText();
               pu.setSharedCacheMode(SharedCacheMode.valueOf(cm));
               break;

            case VALIDATIONMODE:
               String validationMode = reader.getElementText();
               pu.setValidationMode(ValidationMode.valueOf(validationMode));
               break;

            default:
               throw unexpectedElement(reader);
         }
      }

      pu.setClasses(classes);
      pu.setJarFiles(jarfiles);
      pu.setMappingFiles(mappingFiles);
      pu.setProperties(properties);
      return pu;
   }

   private static void parseProperties(XMLStreamReader reader, Properties properties) throws XMLStreamException {

      while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
         final Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case PROPERTY:
               final int count = reader.getAttributeCount();
               String name=null, value=null;
               for (int i = 0; i < count; i++) {
                  final String attributeValue = reader.getAttributeValue(i);
                  if (reader.getAttributeNamespace(i) != null) {
                     continue;
                  }
                  final Element attribute = Element.forName(reader.getAttributeLocalName(i));
                  switch (attribute) {
                     case NAME:
                        name = attributeValue;
                        break;
                     case VALUE:
                        value = attributeValue;
                        if( name != null && value != null) {
                           properties.put(name, value);
                        }
                        name = value = null;
                        break;
                     default:
                        throw unexpectedAttribute(reader, i);
                  }
               }
               if( reader.hasNext() && (reader.nextTag() != END_ELEMENT))
                  throw unexpectedElement(reader);

               break;
            default:
               throw unexpectedElement(reader);
         }
      }
   }

   /**
    * Simple test driver for parsing the specified persistence.xml file
    * @param args
    */
   public static void main(String[] args) {
      try {
         String filename;
         if (args.length < 2) {
            filename = "persistence.xml";
         }
         else
            filename = args[1];
         System.out.println("will parse " + filename);
         XMLInputFactory xmlif = XMLInputFactory.newInstance();

         XMLStreamReader reader =
            xmlif.createXMLStreamReader(filename, new
               FileInputStream(filename));

         List<PersistenceMetadata> puList = parse(reader);
         System.out.println("result = " + puList);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
