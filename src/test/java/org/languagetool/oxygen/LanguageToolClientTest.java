/* LanguageTool plugin for Oxygen XML editor 
 * Copyright (C) 2014 Daniel Naber (http://www.danielnaber.de)
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

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LanguageToolClientTest {
  
  @Test
  @Ignore("needs LanguageTool running locally in server mode")
  public void testCheckViaHttp() {
    LanguageToolClient client = new LanguageToolClient("http://localhost:8081/");
    TextWithMapping text = new TextWithMapping(null);
    text.addMapping(new TextRange(0, 16), new TextRange(10, 26));
    text.setText("This is an text.");
    List<RuleMatch> ruleMatches = client.checkText(text, "en");
    assertThat(ruleMatches.size(), is(1));
    assertThat(ruleMatches.get(0).getReplacements().size(), is(1));
    assertThat(ruleMatches.get(0).getReplacements().get(0), is("a"));
    
    assertThat(ruleMatches.get(0).getOffsetStart(), is(8));
    assertThat(ruleMatches.get(0).getOffsetEnd(), is(10));

    assertThat(ruleMatches.get(0).getOxygenOffsetStart(), is(19));
    assertThat(ruleMatches.get(0).getOxygenOffsetEnd(), is(20));
  }
}
