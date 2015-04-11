/* LanguageTool plugin for Oxygen XML editor
 * Copyright (C) 2015 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.oxygen;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class LanguageAttributeDetectorTest {

  @Test
  public void test() {
    LanguageAttributeDetector detector = new LanguageAttributeDetector();
    assertNull(detector.getDocumentLanguage(""));
    assertNull(detector.getDocumentLanguage("<x></x>"));
    assertThat(detector.getDocumentLanguage("<x lang='en'></x>"), is("en"));
    assertThat(detector.getDocumentLanguage("<x lang='foobar'></x>"), is("foobar"));
    assertThat(detector.getDocumentLanguage("<x lang=\"de-DE\"></x>"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x lang='de-DE'></x>"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x xml:lang=\"de-DE\"></x>"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'></x>"), is("de-DE"));
    // first occurrence wins:
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'><p lang='xx'></p></x>"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'><p xml:lang='xx'></p></x>"), is("de-DE"));
    // invalid XML after the attribute we're looking for:
    assertNull(detector.getDocumentLanguage("<x"));
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'><p"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'>p>"), is("de-DE"));
    assertThat(detector.getDocumentLanguage("<x xml:lang='de-DE'></x><x></x>"), is("de-DE"));
    // invalid XML before the attribute we're looking for:
    assertNull(detector.getDocumentLanguage("<p<x xml:lang='de-DE'></x>)"));
  }
  
}
