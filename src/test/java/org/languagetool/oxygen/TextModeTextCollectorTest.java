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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TextModeTextCollectorTest {

  @Test
  public void testTextCollector() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    assertThat(textCollector.collectTexts("<t/>").getText(), is(""));
    assertThat(textCollector.collectTexts("<t></t>").getText(), is(""));
    assertThat(textCollector.collectTexts("<t>x</t>").getText(), is("x"));
    assertThat(textCollector.collectTexts("<t>öäü</t>").getText(), is("öäü"));
    assertThat(textCollector.collectTexts("<t>a<nested>bc</nested></t>").getText(), is("abc"));
    assertThat(textCollector.collectTexts("<t>a<nested>bc</nested><foo test='attrib'>ddd</foo></t>").getText(), is("abcddd"));
  }

  @Test
  public void testMapping1() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts("<t>x</t>");
    assertThat(mapping.getText(), is("x"));
    assertThat(mapping.getMapping().size(), is(1));
    assertThat(mapping.getMapping().toString(), is("{0-1=3-4}"));
  }

  @Test
  public void testMapping2() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts("<t>xyz\n<foo att='val'>abc</foo></t>");
    assertThat(mapping.getMapping().size(), is(2));
    String mapStr = mapping.getMapping().toString();
    assertThat(mapping.getText(), is("xyz\nabc"));
    assertTrue("Got: " + mapStr, mapStr.contains("0-4=3-7"));
    assertTrue("Got: " + mapStr, mapStr.contains("4-7=22-25"));
  }

  @Test
  public void testMappingWithWhitespace1() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts(
            "<r>\n" +
            "  <c></c>\n" +
            "</r>\n");
    assertThat(mapping.getText(), is("\n  \n\n"));
    assertThat(mapping.getMapping().size(), is(3));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-3=3-6"));
    assertTrue("Got: " + mapStr, mapStr.contains("3-4=13-14"));
    assertTrue("Got: " + mapStr, mapStr.contains("4-5=18-19"));
  }

  @Test
  public void testMappingWithWhitespace2() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts("<r>  <e>XX</e></r>");
    assertThat(mapping.getText(), is("  XX"));
    assertThat(mapping.getMapping().size(), is(2));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-2=3-5"));
    assertTrue("Got: " + mapStr, mapStr.contains("2-4=8-10"));
  }

  @Test
  public void testMappingWithWhitespace3() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts("<t>\n  <e>X</e></t>");
    assertThat(mapping.getText(), is("\n  X"));
    assertThat(mapping.getMapping().size(), is(2));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-3=3-6"));
    assertTrue("Got: " + mapStr, mapStr.contains("3-4=9-10"));
  }

  @Test
  public void testMappingWithEntities() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE rules [\n" +
            "    <!ENTITY myEntity \"den\">\n" +
            "]>\n" +
            "<rules>\n" +
            "  <example>Bla.</example>\n" +
            "</rules>\n");
    assertThat(mapping.getText(), is("\n\n\n  Bla.\n\n"));
    assertThat(mapping.getMapping().size(), is(6));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-1=38-61"));
    assertTrue("Got: " + mapStr, mapStr.contains("2-5=96-99"));
    assertTrue("Got: " + mapStr, mapStr.contains("5-9=108-112"));
    assertTrue("Got: " + mapStr, mapStr.contains("9-10=122-123"));
  }

  @Test
  public void testIgnoreComments1() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts(
            "<t>\n<!-- <b -->foo <b>bar</b></t>");
    assertThat(mapping.getText(), is("\nfoo bar"));
    assertThat(mapping.getMapping().size(), is(2));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-5=3-19"));
    assertTrue("Got: " + mapStr, mapStr.contains("5-8=22-25"));
  }

  @Test
  public void testIgnoreComments2() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    TextWithMapping mapping = textCollector.collectTexts(
            "<t>\n<!-- b> -->foo <b>bar</b></t>");
    assertThat(mapping.getText(), is("\nfoo bar"));
    assertThat(mapping.getMapping().size(), is(2));
    String mapStr = mapping.getMapping().toString();
    assertTrue("Got: " + mapStr, mapStr.contains("0-5=3-19"));
    assertTrue("Got: " + mapStr, mapStr.contains("5-8=22-25"));
  }

  @Test
  public void testCommentsCornerCases() {
    TextModeTextCollector textCollector = new TextModeTextCollector();
    // just make sure not to crash:
    textCollector.collectTexts("<!-- -->");
    textCollector.collectTexts("<!-- b> -->");
    textCollector.collectTexts("<!-- <b> -->");
    textCollector.collectTexts("<!-- <> -->");
    textCollector.collectTexts("<!--\n-->");
    textCollector.collectTexts("<!--");
    textCollector.collectTexts("<!-- ");
    textCollector.collectTexts("-->");
    textCollector.collectTexts(" -->");
  }

}