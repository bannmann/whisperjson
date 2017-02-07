package flatjson;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class Json {

    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(Token token) {
            super("illegal token: " + token);
        }

        public ParseException(char c) {
            super("illegal char: '" + c + "'");
        }
    }

    public enum Token {
        NULL,
        TRUE,
        FALSE,
        STRING,
        ARRAY,
        OBJECT,
        OBJECT_VALUE
    }

    private static final char[] HEX_CHARS = "0123456789abcdefABCDEF".toCharArray();
    private static final char[] ESCAPED_CHARS = "\"\\/bfnrt".toCharArray();
    private static final char[] WHITESPACE_CHARS = " \t\r\n".toCharArray();

    private static class State {
        State consume(Json json, int index, char c) {
            throw new ParseException(c);
        }
    }

    private static class SkipWhitespace extends State {
        @Override State consume(Json json, int index, char c) {
            for (char w : WHITESPACE_CHARS) if (c == w) return this;
            return super.consume(json, index, c);
        }
    }

    private static class Value extends SkipWhitespace {
        @Override State consume(Json json, int index, char c) {
            if (c == 'n') return json.begin(index, Token.NULL);
            if (c == 't') return json.begin(index, Token.TRUE);
            if (c == 'f') return json.begin(index, Token.FALSE);
            if (c == '"') return json.begin(index, Token.STRING);
            if (c == '[') return json.begin(index, Token.ARRAY);
            if (c == '{') return json.begin(index, Token.OBJECT);
            return super.consume(json, index, c);
        }
    }

    private static final State VALUE = new Value();

    private static final State END = new SkipWhitespace();

    private static final State NULL_1 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'u') return NULL_2;
            return super.consume(json, index, c);
        }
    };

    private static final State NULL_2 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'l') return NULL_3;
            return super.consume(json, index, c);
        }
    };

    private static final State NULL_3 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'l') return json.end(index, Token.NULL);
            return super.consume(json, index, c);
        }
    };

    private static final State TRUE_1 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'r') return TRUE_2;
            return super.consume(json, index, c);
        }
    };

    private static final State TRUE_2 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'u') return TRUE_3;
            return super.consume(json, index, c);
        }
    };

    private static final State TRUE_3 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'e') return json.end(index, Token.TRUE);
            return super.consume(json, index, c);
        }
    };

    private static final State FALSE_1 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'a') return FALSE_2;
            return super.consume(json, index, c);
        }
    };

    private static final State FALSE_2 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'l') return FALSE_3;
            return super.consume(json, index, c);
        }
    };

    private static final State FALSE_3 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 's') return FALSE_4;
            return super.consume(json, index, c);
        }
    };

    private static final State FALSE_4 = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == 'e') return json.end(index, Token.FALSE);
            return super.consume(json, index, c);
        }
    };

    private static final State ARRAY_START = new Value() {
        @Override State consume(Json json, int index, char c) {
            if (c == ']') return json.end(index, Token.ARRAY);
            return super.consume(json, index, c);
        }
    };

    private static final State ARRAY_NEXT = new SkipWhitespace() {
        @Override State consume(Json json, int index, char c) {
            if (c == ']') return json.end(index, Token.ARRAY);
            if (c == ',') return VALUE;
            return super.consume(json, index, c);
        }
    };

    private static final State OBJECT_START = new SkipWhitespace() {
        @Override State consume(Json json, int index, char c) {
            if (c == '}') return json.end(index, Token.OBJECT);
            if (c == '"') return json.begin(index, Token.STRING);
            return super.consume(json, index, c);
        }
    };

    private static final State OBJECT_KEY = new SkipWhitespace() {
        @Override State consume(Json json, int index, char c) {
            if (c == '"') return json.begin(index, Token.STRING);
            return super.consume(json, index, c);
        }
    };

    private static final State OBJECT_COLON = new SkipWhitespace() {
        @Override State consume(Json json, int index, char c) {
            if (c == ':') return json.begin(index, Token.OBJECT_VALUE);
            return super.consume(json, index, c);
        }
    };

    private static final State OBJECT_NEXT = new SkipWhitespace() {
        @Override State consume(Json json, int index, char c) {
            if (c == '}') return json.end(index, Token.OBJECT);
            if (c == ',') return OBJECT_KEY;
            return super.consume(json, index, c);
        }
    };

    private static final State STRING = new State() {
        @Override State consume(Json json, int index, char c) {
            if (c == '"') return json.end(index, Token.STRING);
            if (c == '\\') return STRING_ESCAPED;
            if (c <= 31) return super.consume(json, index, c);
            return this;
        }
    };

    private static final State STRING_ESCAPED = new State() {
        @Override State consume(Json json, int index, char c) {
            for (char e : ESCAPED_CHARS) if (c == e) return STRING;
            if (c == 'u') return UNICODE_1;
            return super.consume(json, index, c);
        }
    };

    private static final State UNICODE_1 = new State() {
        @Override State consume(Json json, int index, char c) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_2;
            return super.consume(json, index, c);
        }
    };

    private static final State UNICODE_2 = new State() {
        @Override State consume(Json json, int index, char c) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_3;
            return super.consume(json, index, c);
        }
    };

    private static final State UNICODE_3 = new State() {
        @Override State consume(Json json, int index, char c) {
            for (char h : HEX_CHARS) if (c == h) return UNICODE_4;
            return super.consume(json, index, c);
        }
    };

    private static final State UNICODE_4 = new State() {
        @Override State consume(Json json, int index, char c) {
            for (char h : HEX_CHARS) if (c == h) return STRING;
            return super.consume(json, index, c);
        }
    };

    public static Json parse(String input) {
        Json json = new Json(input);
        State state = VALUE;
        for (int index = 0; index < input.length(); index++) {
            state = state.consume(json, index, input.charAt(index));
        }
        if (state != END) throw new ParseException("unbalanced json");
        return json;
    }

    private class Element {
        final Token token;
        final int from;
        int to;
        int contains;

        Element(Token token, int from) {
            this.token = token;
            this.from = from;
        }

        @Override public String toString() {
            return "Element{" + token + " [" + from + ".." + to + "] " + " contains=" + contains + " " + _raw.substring(from, to + 1) + "\n";
        }
    }

    private class Frame {
        final Token token;
        final int element;

        Frame(Token token, int element) {
            this.token = token;
            this.element = element;
        }
    }

    private String _raw;
    private List<Element> _elements = new ArrayList<>();
    private Stack<Frame> _stack = new Stack<>();

    private Json(String raw) {
        _raw = raw;
    }

    State begin(int index, Token token) {
//        System.out.println("BEGIN " + token);
        if (token != Token.OBJECT_VALUE) {
            _elements.add(new Element(token, index));
        }
        _stack.add(new Frame(token, _elements.size() - 1));
        if (token == Token.NULL) return NULL_1;
        if (token == Token.TRUE) return TRUE_1;
        if (token == Token.FALSE) return FALSE_1;
        if (token == Token.ARRAY) return ARRAY_START;
        if (token == Token.OBJECT) return OBJECT_START;
        if (token == Token.OBJECT_VALUE) return VALUE;
        if (token == Token.STRING) return STRING;
        throw new ParseException("illegal state: " + token);
    }

    State end(int index, Token token) {
//        System.out.println("END " + token);
        Frame last = _stack.pop();
        if (last.token != token) throw new ParseException(token);
        Element element = _elements.get(last.element);
        element.to = index;
        element.contains = _elements.size() - last.element - 1;
        if (_stack.empty()) return END;
        if (Token.ARRAY == _stack.peek().token) return ARRAY_NEXT;
        if (Token.OBJECT == _stack.peek().token) return OBJECT_COLON;
        if (Token.OBJECT_VALUE == _stack.peek().token) {
            _stack.pop();
            return OBJECT_NEXT;
        }
        throw new ParseException("illegal state: " + token);
    }

    public List<Token> getTokens() {
        return _elements.stream().map(element -> element.token).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return Arrays.toString(_elements.toArray());
    }

    public static void main(String[] args) throws IOException {
        String input = "   [null, [ \"hello world\", [], true ], null]  ";
//        String input = "{ \"foo\": [true, false] }";
//        String input = new String(Files.readAllBytes(Paths.get("test/sample.json")));
        Json json = parse(input);
        System.out.println(json);
    }

}