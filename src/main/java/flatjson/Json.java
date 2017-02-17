package flatjson;

import java.util.ArrayList;
import java.util.List;

public class Json {

    enum Token {
        NULL,
        TRUE,
        FALSE,
        NUMBER,
        STRING,
        ARRAY,
        OBJECT,
        OBJECT_VALUE
    }

    private static final char[] HEX_CHARS = "0123456789abcdefABCDEF".toCharArray();
    private static final char[] ESCAPED_CHARS = "\"\\/bfnrt".toCharArray();
    private static final char[] WHITESPACE_CHARS = " \t\r\n".toCharArray();

    private class State {
        State consume(int index, char c, char next) {
            throw new ParseException(c);
        }
    }

    private class SkipWhitespace extends State {
        @Override State consume(int index, char c, char next) {
            for (char w : WHITESPACE_CHARS) if (c == w) return this;
            return super.consume(index, c, next);
        }
    }

    private class Value extends SkipWhitespace {
        @Override State consume(int index, char c, char next) {
            if (c == 'n') return beginElement(index, Token.NULL);
            if (c == 't') return beginElement(index, Token.TRUE);
            if (c == 'f') return beginElement(index, Token.FALSE);
            if (c == '"') return beginElement(index, Token.STRING);
            if (c == '[') return beginElement(index, Token.ARRAY);
            if (c == '{') return beginElement(index, Token.OBJECT);
            if (c == '0') {
                if (next >= '0' && next <= '9') throw new ParseException(c);
                if (next == '.') return beginElement(index, Token.NUMBER);
                if (next == 'e' || next == 'E') {
                    beginElement(index, Token.NUMBER);
                    return EXPONENT;
                }
                beginElement(index, Token.NUMBER);
                return endElement(index, Token.NUMBER);
            }
            if (c >= '1' && c <= '9') {
                if (next >= '0' && next <= '9') return beginElement(index, Token.NUMBER);
                if (next == '.') return beginElement(index, Token.NUMBER);
                if (next == 'e' || next == 'E') {
                    beginElement(index, Token.NUMBER);
                    return EXPONENT;
                }
                beginElement(index, Token.NUMBER);
                return endElement(index, Token.NUMBER);
            }
            if (c == '-') {
                beginElement(index, Token.NUMBER);
                return NUMBER_NEGATIVE;
            }
            return super.consume(index, c, next);
        }
    }

    private final State VALUE = new Value();

    private final State END = new SkipWhitespace();

    private final State NULL_1 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'u') return NULL_2;
            return super.consume(index, c, next);
        }
    };

    private final State NULL_2 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'l') return NULL_3;
            return super.consume(index, c, next);
        }
    };

    private final State NULL_3 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'l') return endElement(index, Token.NULL);
            return super.consume(index, c, next);
        }
    };

    private final State TRUE_1 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'r') return TRUE_2;
            return super.consume(index, c, next);
        }
    };

    private final State TRUE_2 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'u') return TRUE_3;
            return super.consume(index, c, next);
        }
    };

    private final State TRUE_3 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'e') return endElement(index, Token.TRUE);
            return super.consume(index, c, next);
        }
    };

    private final State FALSE_1 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'a') return FALSE_2;
            return super.consume(index, c, next);
        }
    };

    private final State FALSE_2 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'l') return FALSE_3;
            return super.consume(index, c, next);
        }
    };

    private final State FALSE_3 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 's') return FALSE_4;
            return super.consume(index, c, next);
        }
    };

    private final State FALSE_4 = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == 'e') return endElement(index, Token.FALSE);
            return super.consume(index, c, next);
        }
    };

    private final State NUMBER_NEGATIVE = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == '0') {
                if (next >= '0' && next <= '9') return super.consume(index, c, next);
                if (next == '.') return NUMBER_DOT;
                if (next == 'e' || next == 'E') return EXPONENT;
                return endElement(index, Token.NUMBER);
            }
            if (c >= '1' && c <= '9') {
                if (next >= '0' && next <= '9') return NUMBER_START;
                if (next == '.') return NUMBER_DOT;
                if (next == 'e' || next == 'E') return EXPONENT;
                return endElement(index, Token.NUMBER);

            }
            return super.consume(index, c, next);
        }
    };

    private final State NUMBER_START = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == '.') return NUMBER_FRACTION;
            if (c >= '0' && c <= '9') {
                if (next >= '0' && next <= '9') return NUMBER_START;
                if (next == '.') return NUMBER_DOT;
                if (next == 'e' || next == 'E') return EXPONENT;
                return endElement(index, Token.NUMBER);
            }
            return super.consume(index, c, next);
        }
    };

    private final State NUMBER_DOT = new State() {
        @Override State consume(int index, char c, char next) {
            return NUMBER_FRACTION;
        }
    };

    private final State NUMBER_FRACTION = new State() {
        @Override State consume(int index, char c, char next) {
            if (c >= '0' && c <= '9') {
                if (next >= '0' && next <= '9') return NUMBER_FRACTION;
                if (next == 'e' || next == 'E') return EXPONENT;
                return endElement(index, Token.NUMBER);
            }
            return super.consume(index, c, next);
        }
    };

    private final State EXPONENT = new State() {
        @Override State consume(int index, char c, char next) {
            if (next == '+' || next == '-') return EXPONENT_SIGN;
            return EXPONENT_DIGITS;
        }
    };

    private final State EXPONENT_SIGN = new State() {
        @Override State consume(int index, char c, char next) {
            return EXPONENT_DIGITS;
        }
    };

    private final State EXPONENT_DIGITS = new State() {
        @Override State consume(int index, char c, char next) {
            if (c >= '0' && c <= '9') {
                if (next >= '0' && next <= '9') return EXPONENT_DIGITS;
                return endElement(index, Token.NUMBER);
            }
            return super.consume(index, c, next);
        }
    };

    private final State ARRAY_START = new Value() {
        @Override State consume(int index, char c, char next) {
            if (c == ']') return endElement(index, Token.ARRAY);
            return super.consume(index, c, next);
        }
    };

    private final State ARRAY_NEXT = new SkipWhitespace() {
        @Override State consume(int index, char c, char next) {
            if (c == ']') return endElement(index, Token.ARRAY);
            if (c == ',') return VALUE;
            return super.consume(index, c, next);
        }
    };

    private final State OBJECT_START = new SkipWhitespace() {
        @Override State consume(int index, char c, char next) {
            if (c == '}') return endElement(index, Token.OBJECT);
            if (c == '"') return beginElement(index, Token.STRING);
            return super.consume(index, c, next);
        }
    };

    private final State OBJECT_KEY = new SkipWhitespace() {
        @Override State consume(int index, char c, char next) {
            if (c == '"') return beginElement(index, Token.STRING);
            return super.consume(index, c, next);
        }
    };

    private final State OBJECT_COLON = new SkipWhitespace() {
        @Override State consume(int index, char c, char next) {
            if (c == ':') return beginElement(index, Token.OBJECT_VALUE);
            return super.consume(index, c, next);
        }
    };

    private final State OBJECT_NEXT = new SkipWhitespace() {
        @Override State consume(int index, char c, char next) {
            if (c == '}') return endElement(index, Token.OBJECT);
            if (c == ',') return OBJECT_KEY;
            return super.consume(index, c, next);
        }
    };

    private final State STRING = new State() {
        @Override State consume(int index, char c, char next) {
            if (c == '"') return endElement(index, Token.STRING);
            if (c == '\\') return STRING_ESCAPED;
            if (c <= 31) return super.consume(index, c, next);
            return this;
        }
    };

    private final State STRING_ESCAPED = new State() {
        @Override State consume(int index, char c, char next) {
            for (char e : ESCAPED_CHARS) if (c == e) return STRING;
            if (c == 'u') return UNICODE_1;
            return super.consume(index, c, next);
        }
    };

    private final State UNICODE_1 = new State() {
        @Override State consume(int index, char c, char next) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_2;
            return super.consume(index, c, next);
        }
    };

    private final State UNICODE_2 = new State() {
        @Override State consume(int index, char c, char next) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_3;
            return super.consume(index, c, next);
        }
    };

    private final State UNICODE_3 = new State() {
        @Override State consume(int index, char c, char next) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_4;
            return super.consume(index, c, next);
        }
    };

    private final State UNICODE_4 = new State() {
        @Override State consume(int index, char c, char next) {
            for (char h : HEX_CHARS) if (c == h) return STRING;
            return super.consume(index, c, next);
        }
    };

    private static final int ELEMENTS_BLOCK_SIZE = 4 * 1024; // 16 KB
    private static final int FRAMES_BLOCK_SIZE = 2 * 256; // 2 KB

    private final String raw;
    private final List<int[]> elements;
    private int elementCount;
    private final List<int[]> frames;
    private int currentFrame;

    private Json(String raw) {
        this.raw = raw;
        this.elements = new ArrayList<>();
        this.elementCount = 0;
        this.frames = new ArrayList<>();
        this.currentFrame = -1;
    }

    private JsonValue parse() {
        State state = VALUE;
        int last = raw.length() - 1;
        for (int i = 0; i < last; i++) {
            state = state.consume(i, raw.charAt(i), raw.charAt(i + 1));
        }
        state = state.consume(last, raw.charAt(last), ' ');
        if (state != END) throw new ParseException("unbalanced json");
        return JsonValue.create(this, 0);
    }

    private State beginElement(int index, Token token) {
//        System.out.println("BEGIN " + elementCount + " " + token);
        if (token != Token.OBJECT_VALUE) {
            setToken(elementCount, token);
            setFrom(elementCount, index);
            elementCount++;
        }
        stackPush(token, elementCount - 1);
        if (token == Token.NULL) return NULL_1;
        if (token == Token.TRUE) return TRUE_1;
        if (token == Token.FALSE) return FALSE_1;
        if (token == Token.ARRAY) return ARRAY_START;
        if (token == Token.OBJECT) return OBJECT_START;
        if (token == Token.OBJECT_VALUE) return VALUE;
        if (token == Token.STRING) return STRING;
        if (token == Token.NUMBER) return NUMBER_START;
        throw new ParseException("illegal state: " + token);
    }

    private State endElement(int index, Token token) {
//        System.out.println("END " + token);
        if (token != stackPeekToken()) throw new ParseException(token);
        int element = stackPeekElement();
        setTo(element, index);
        setNested(element, elementCount - element - 1);
        stackPop();
        if (stackEmpty()) return END;
        if (Token.ARRAY == stackPeekToken()) return ARRAY_NEXT;
        if (Token.OBJECT == stackPeekToken()) return OBJECT_COLON;
        if (Token.OBJECT_VALUE == stackPeekToken()) {
            stackPop();
            return OBJECT_NEXT;
        }
        throw new ParseException("illegal state: " + token);
    }

    Token getToken(int element) {
        return Token.values()[getElement(element, 0)];
    }

    private void setToken(int element, Token token) {
        setElement(element, 0, token.ordinal());
    }

    int getFrom(int element) {
        return getElement(element, 1);
    }

    private void setFrom(int element, int from) {
        setElement(element, 1, from);
    }

    int getTo(int element) {
        return getElement(element, 2);
    }

    private void setTo(int element, int to) {
        setElement(element, 2, to);
    }

    int getNested(int element) {
        return getElement(element, 3);
    }

    private void setNested(int element, int nested) {
        setElement(element, 3, nested);
    }

    String getRaw(int element) {
        return raw.substring(getFrom(element), getTo(element) + 1);
    }

    String getRawString(int element) {
        return raw.substring(getFrom(element) + 1, getTo(element));
    }

    private int getElement(int element, int offset) {
        int major = (element * 4 + offset) / ELEMENTS_BLOCK_SIZE;
        int minor = (element * 4 + offset) % ELEMENTS_BLOCK_SIZE;
        return elements.get(major)[minor];
    }

    private void setElement(int element, int offset, int value) {
        int major = (element * 4 + offset) / ELEMENTS_BLOCK_SIZE;
        int minor = (element * 4 + offset) % ELEMENTS_BLOCK_SIZE;
        if (major == elements.size()) {
            elements.add(new int[ELEMENTS_BLOCK_SIZE]);
        }
        elements.get(major)[minor] = value;
    }

    private void stackPush(Token token, int element) {
        currentFrame++;
        setFrame(currentFrame, 0, token.ordinal());
        setFrame(currentFrame, 1, element);
    }

    private Token stackPeekToken() {
        return Token.values()[getFrame(currentFrame, 0)];
    }

    private int stackPeekElement() {
        return getFrame(currentFrame, 1);
    }

    private void stackPop() {
        currentFrame--;
    }

    private boolean stackEmpty() {
        return currentFrame < 0;
    }

    private int getFrame(int frame, int offset) {
        int major = (frame * 2 + offset) / FRAMES_BLOCK_SIZE;
        int minor = (frame * 2 + offset) % FRAMES_BLOCK_SIZE;
        return frames.get(major)[minor];
    }

    private void setFrame(int frame, int offset, int value) {
        int major = (frame * 2 + offset) / FRAMES_BLOCK_SIZE;
        int minor = (frame * 2 + offset) % FRAMES_BLOCK_SIZE;
        if (major == frames.size()) {
            frames.add(new int[FRAMES_BLOCK_SIZE]);
        }
        frames.get(major)[minor] = value;
    }

    public static JsonValue parse(String raw) {
        if (raw == null) throw new ParseException("cannot parse null");
        if (raw.isEmpty()) throw new ParseException("cannot parse empty string");
        return new Json(raw).parse();
    }
}
