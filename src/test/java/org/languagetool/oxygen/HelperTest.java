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

public class HelperTest {

  @Test
  public void testSplit() {
    assertThat(Helper.splitAtSpace("", 0).toString(), is("[]"));
    assertThat(Helper.splitAtSpace("", 1).toString(), is("[]"));
    assertThat(Helper.splitAtSpace("abc def", 2).toString(), is("[abc, def]"));
    assertThat(Helper.splitAtSpace("abc def", 5).toString(), is("[abc def]"));
    assertThat(Helper.splitAtSpace("abc def ghi", 5).toString(), is("[abc def, ghi]"));
  }

}