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

public class TextWithMappingTest {

  @Test
  public void test1() {
    TextWithMapping mapping = new TextWithMapping();
    mapping.addMapping(new TextRange(0, 1), new TextRange(1, 5));
    mapping.setText("hallo");
    assertThat(mapping.getOxygenPositionFor(0), is(1));
  }

  @Test
  public void test2() {
    TextWithMapping mapping = new TextWithMapping();
    mapping.addMapping(new TextRange(0, 1), new TextRange(1, 5));
    mapping.addMapping(new TextRange(1, 5), new TextRange(10, 14));
    mapping.setText("hallo");
    assertThat(mapping.getOxygenPositionFor(0), is(1));
    assertThat(mapping.getOxygenPositionFor(1), is(10));
    assertThat(mapping.getOxygenPositionFor(2), is(11));
    assertThat(mapping.getOxygenPositionFor(4), is(13));
  }

  @Test(expected = RuntimeException.class)
  public void testInvalidOffset() {
    TextWithMapping mapping = new TextWithMapping();
    mapping.addMapping(new TextRange(0, 1), new TextRange(1, 5));
    mapping.setText("hallo");
    mapping.getOxygenPositionFor(1);  // end position is exclusive
  }
}