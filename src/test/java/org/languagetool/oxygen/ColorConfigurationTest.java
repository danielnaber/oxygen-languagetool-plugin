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

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ColorConfigurationTest {

  @Test
  public void testConfig() throws IOException {
    ColorConfiguration config = new ColorConfiguration(ColorConfigurationTest.class.getResourceAsStream("/org/languagetool/oxygen/config1.properties"));
    Map<String, Color> map = config.getTypeToColorMap();
    assertNotNull(map);
    assertThat(map.get("misspelling"), is(new Color(255, 100, 0)));
    assertThat(map.get("grammar"), is(new Color(55, 55, 55)));
    assertNull(map.get("something-else"));
  }

  @Test
  public void testEmptyColorConfig() throws IOException {
    ColorConfiguration config = new ColorConfiguration(ColorConfigurationTest.class.getResourceAsStream("/org/languagetool/oxygen/config2.properties"));
    Map<String, Color> map = config.getTypeToColorMap();
    assertThat(map.size(), is(0));
  }

  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  @Test(expected = FileNotFoundException.class)
  public void testNonExistingFile() throws IOException {
    new ColorConfiguration(new File("/does-not-exist"));
  }

}
