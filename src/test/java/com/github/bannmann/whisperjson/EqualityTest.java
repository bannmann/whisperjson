package com.github.bannmann.whisperjson;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EqualityTest
{
    @Test
    public void compareNull()
    {
        assertEquals(Json.value((String) null), Json.value((String) null));
        assertEquals(Json.value((String) null), Json.parse("null"));
        assertEquals(Json.value((String) null), Literal.Null.INSTANCE);
    }

    @Test
    public void compareTrue()
    {
        assertEquals(Json.value(true), Json.value(true));
        assertEquals(Json.value(true), Json.parse("true"));
        assertEquals(Json.value(true), Literal.Bool.TRUE);
    }

    @Test
    public void compareFalse()
    {
        assertEquals(Json.value(false), Json.value(false));
        assertEquals(Json.value(false), Json.parse("false"));
        assertEquals(Json.value(false), Literal.Bool.FALSE);
    }

    @Test
    public void compareNumber()
    {
        assertEquals(Json.value(23), Json.value(23));
        assertEquals(Json.value(23), Json.parse("23"));
    }

    @Test
    public void compareString()
    {
        assertEquals(Json.value("\foo"), Json.value("\foo"));
        assertEquals(Json.value("\foo"), Json.parse("\"\\foo\""));
    }

    @Test
    public void compareWithWhitespace()
    {
        assertEquals(Json.parse("[1,2,3]"), Json.parse("[\n1,\n2,\n3\n]"));
    }
}
