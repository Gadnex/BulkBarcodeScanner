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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

/**
 *
 * @author William Gadney <gadnex@gmail.com>
 */
public class Barcode {

    BarcodeType type;
    String content;

    @XmlAttribute(name = "barcodeType", required = true)
    public BarcodeType getType() {
        return type;
    }

    public void setType(BarcodeType type) {
        this.type = type;
    }

    @XmlElement(required = true)
    @XmlCDATA
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
