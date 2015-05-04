/*
 * Copyright 2015 William Gadney <gadnex@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.binarypaper.barcodescanner.entity;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author William Gadney <gadnex@gmail.com>
 */
public class DocumentNGTest {

    private String marshalDocument(Document document) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Document.class}, null);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Create a StringWriter
            StringWriter stringWriter = new StringWriter();

            jaxbMarshaller.marshal(document, stringWriter);

            return stringWriter.toString();
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private Document unmarshalDocument(String xml) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Document.class}, null);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            StringReader stringReader = new StringReader(xml);
            
            return (Document) jaxbUnmarshaller.unmarshal(stringReader);
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testXmlFileGeneration() throws JAXBException {
        Document document = new Document();
        document.setFileName("MyDocument.pdf");
        List<Page> pages = new ArrayList<Page>();
        // Page 1
        Page page1 = new Page();
        page1.setPageNumber(1);
        List<Barcode> page1Barcodes = new ArrayList<Barcode>();
        // Page 1 Barcode 1
        Barcode page1Barcode1 = new Barcode();
        page1Barcode1.setType(BarcodeType.QR);
        page1Barcode1.setContent("Page 1 Barcode 1");
        page1Barcodes.add(page1Barcode1);
        // Page 1 Barcode 2
        Barcode page1Barcode2 = new Barcode();
        page1Barcode2.setType(BarcodeType.PDF417);
        page1Barcode2.setContent("Page 1 Barcode 2");
        page1Barcodes.add(page1Barcode2);
        page1.setBarcodes(page1Barcodes);
        pages.add(page1);
        // Page 2
        Page page2 = new Page();
        page2.setPageNumber(2);
        List<Barcode> page2Barcodes = new ArrayList<Barcode>();
        // Page 2 Barcode 1
        Barcode page2Barcode1 = new Barcode();
        page2Barcode1.setType(BarcodeType.QR);
        page2Barcode1.setContent("Page 2 Barcode 1");
        page2Barcodes.add(page2Barcode1);
        // Page 2 Barcode 2
        Barcode page2Barcode2 = new Barcode();
        page2Barcode2.setType(BarcodeType.PDF417);
        page2Barcode2.setContent("<xml>Page 2 Barcode 2 inside an xml tag</xml>");
        page2Barcodes.add(page2Barcode2);
        page2.setBarcodes(page2Barcodes);
        pages.add(page2);
        document.setPages(pages);
        String xml = marshalDocument(document);
        System.out.println(xml);
        Assert.assertTrue(xml.contains("<![CDATA["));
        Document unmarshalledDocument = unmarshalDocument(xml);
        Assert.assertEquals(unmarshalledDocument.getFileName(), "MyDocument.pdf");
        Assert.assertEquals(unmarshalledDocument.getPages().get(0).getBarcodes().get(0).getContent(), "Page 1 Barcode 1");
    }
}
