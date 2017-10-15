/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formattable;
import java.util.Formatter;
import java.util.UnknownFormatConversionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class FormatterTest {
    @Test(expected = UnknownFormatConversionException.class)
    public void unexpectedEndOfFormatString() {
        new Formatter().format("%1", "foo");
    }

    @Test(expected = DuplicateFormatFlagsException.class)
    public void duplicateFlag() {
        new Formatter().format("%--s", "q");
    }

    @Test(expected = UnknownFormatConversionException.class)
    public void noPrecisionAfterDot() {
        new Formatter().format("%1.s", "q");
    }

    @Test
    public void bothPreviousModifierAndArgumentIndexPresent() {
        String result = new Formatter().format("%s %2$<s", "q", "w").toString();
        assertEquals("q q", result);
    }

    @Test
    public void formatsBoolean() {
        assertEquals("true", new Formatter().format("%b", true).toString());
        assertEquals("false", new Formatter().format("%b", false).toString());

        assertEquals("true", new Formatter().format("%b", new Object()).toString());
        assertEquals("false", new Formatter().format("%b", null).toString());

        assertEquals("  true", new Formatter().format("%6b", true).toString());
        assertEquals("true  ", new Formatter().format("%-6b", true).toString());
        assertEquals("true", new Formatter().format("%2b", true).toString());
        assertEquals("tr", new Formatter().format("%2.2b", true).toString());
        assertEquals("  tr", new Formatter().format("%4.2b", true).toString());
        assertEquals("TRUE", new Formatter().format("%B", true).toString());

        try {
            new Formatter().format("%+b", true);
            fail("Should have thrown exception");
        } catch (FormatFlagsConversionMismatchException e) {
            assertEquals("+", e.getFlags());
            assertEquals('b', e.getConversion());
        }
    }

    @Test
    public void formatsString() {
        assertEquals("23 foo", new Formatter().format("%s %s", 23, "foo").toString());
        assertEquals("0:-1:-1", new Formatter().format("%s", new A()).toString());
    }

    static class A implements Formattable {
        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            formatter.format("%s", flags + ":" + width + ":" + precision);
        }
    }

    @Test
    public void respectsFormatArgumentOrder() {
        String result = new Formatter().format("%s %s %<s %1$s %<s %s %1$s %s %<s", "a", "b", "c", "d").toString();
        assertEquals("a b b a a c a d d", result);
    }
}
