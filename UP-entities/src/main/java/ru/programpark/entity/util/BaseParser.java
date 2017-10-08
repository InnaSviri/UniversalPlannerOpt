package ru.programpark.entity.util;


import ru.programpark.entity.data.InputData;

import java.io.*;
import java.util.*;

public class BaseParser {
    protected InputData DATA = null;

    public InputData getData() {
        return DATA;
    }

    protected static class Token {
        public enum Type {
            any, invalid, word, string, number,
            add, del, lpar, rpar, lbr, rbr, comma
        }
        public Type type = Type.invalid;
        public Object value = null;
    }

    protected static class TokenIterator
    implements Iterator<Token> {

        private StreamTokenizer tokenizer;
        private Token nextToken;

        public IOException nextTokenIOExc;
        public Token lookaheadToken;

        public TokenIterator(Reader input) {
            tokenizer = new StreamTokenizer(input);
            tokenizer.resetSyntax();
            tokenizer.parseNumbers();
            tokenizer.quoteChar('"');
            tokenizer.whitespaceChars('\t', '\r');
            tokenizer.whitespaceChars(' ', ' ');
            tokenizer.wordChars('0', '9');
            tokenizer.wordChars('A', 'Z');
            tokenizer.wordChars('_', '_');
            tokenizer.wordChars('a', 'z');
            getNextToken();
        }

        private void getNextToken() {
            if (tokenizer.ttype != StreamTokenizer.TT_EOF) {
                nextToken = new Token();
                try {
                    switch (tokenizer.nextToken()) {
                    case StreamTokenizer.TT_NUMBER:
                        nextToken.type = Token.Type.number;
                        nextToken.value = new Double(tokenizer.nval);
                        break;
                    case StreamTokenizer.TT_WORD:
                        nextToken.type = Token.Type.word;
                        nextToken.value = tokenizer.sval.intern();
                        break;
                    case '"':
                        nextToken.type = Token.Type.string;
                        nextToken.value = tokenizer.sval;
                        break;
                    case '+':
                        nextToken.type = Token.Type.add;
                        break;
                    case '-':
                        nextToken.type = Token.Type.del;
                        break;
                    case '(':
                        nextToken.type = Token.Type.lpar;
                        break;
                    case ')':
                        nextToken.type = Token.Type.rpar;
                        break;
                    case '[':
                        nextToken.type = Token.Type.lbr;
                        break;
                    case ']':
                        nextToken.type = Token.Type.rbr;
                        break;
                    case ',':
                        nextToken.type = Token.Type.comma;
                        break;
                    }
                } catch (IOException e) {
                    nextTokenIOExc = e;
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (nextTokenIOExc != null) return false;
            return (tokenizer.ttype != StreamTokenizer.TT_EOF);
        }

        @Override
        public Token next() {
            if (! hasNext()) throw new NoSuchElementException();
            Token tok = nextToken;
            getNextToken();
            return tok;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    protected List<Token> tokenize(String input) throws IOException  {
        return tokenize(new StringReader(input));
    }

    protected List<Token> tokenize(Reader input) throws IOException {
        TokenIterator iter = new TokenIterator(input);
        List<Token> tokens = new ArrayList<Token>();
        while (iter.hasNext()) tokens.add(iter.next());
        if (iter.nextTokenIOExc != null) throw iter.nextTokenIOExc;
        return tokens;
    }

    public static abstract class Term {
        public abstract String toString();
    }

    public static class NumberTerm extends Term implements Serializable {
        public Double value;
        public double doubleValue() { return value.doubleValue(); }
        public long longValue() { return value.longValue(); }
        public int intValue() { return value.intValue(); }
        public boolean booleanValue() { return (value.intValue() != 0); }
        public String toString() {
            if (value == null) {
                return "";
            } else {
                String fmt = (value.equals(Math.floor(value))) ? "%.0f" : "%f";
                return String.format(Locale.ROOT, fmt, value);
            }
        }
    }

    public static class StringTerm extends Term implements Serializable {
        public String value;
        public String toString() {
            if (value == null) {
                return "";
            } else {
                String esc = value.replace("\\", "\\\\").replace("\"", "\\\"");
                return "\"" + esc + "\"";
            }
        }
    }

    public static class ListTerm extends Term implements Serializable {
        public List<Term> elements;
        public int length() {
            return elements.size();
        }
        public Term nth(int i) throws IndexOutOfBoundsException {
            return elements.get(i);
        }
        public Iterator<Term> iterator() {
            return elements.listIterator();
        }
        public String toString() {
            String ret = "[";
            for (Term elt : elements) ret += elt.toString() + ",";
            return ret.replaceFirst(",?$", "]");
        }
    }

    public static class CompoundTerm extends Term implements Serializable {
        public String functor;
        List<Term> args;
        public int arity() {
            return args.size();
        }
        public Term argument(int i) throws IndexOutOfBoundsException {
            return args.get(i);
        }
        public String toString() {
            if (arity() == 0) {
                return functor;
            } else {
                String ret = functor + "(";
                for (Term arg : args) ret += arg.toString() + ",";
                return ret.replaceFirst(",?$", ")");
            }
        }
    }

    public static class Datum implements Serializable {
        public enum Op { none, add, del }
        public Op op = Op.none;
        public CompoundTerm term = null;
        public String toString() {
            return ((op == Op.add) ? "+" : (op == Op.del) ? "-" : "")
                + term.toString();
        }
    }

    public class ParseException extends RuntimeException {
        private String faultyInput;
        private Integer faultyInputIndex;

        public ParseException(String msg) {
            super(msg);
            // LoggingAssistant.logException(this);
        }

        public String getFaultyInput() { return faultyInput; }
        public void setFaultyInput(String str) { faultyInput = str; }

        public Integer getFaultyInputIndex() { return faultyInputIndex; }
        public void setFaultyInputIndex(Integer i) { faultyInputIndex = i; }

        @Override public String toString() {
            String ret = super.toString();
            if (faultyInput != null || faultyInputIndex != null) {
                ret += ". Bad input";
                if (faultyInputIndex != null) ret += " @" + faultyInputIndex;
                if (faultyInput != null) ret += " is: " + faultyInput;
            }
            return ret;
        }
    }

    protected Token nextToken(TokenIterator iter, Token.Type expected)
    throws IOException {
        Token tok;
        if (iter.lookaheadToken != null) {
            tok = iter.lookaheadToken;
            iter.lookaheadToken = null;
        } else if (iter.hasNext()) {
            tok = iter.next();
        } else if (iter.nextTokenIOExc != null) {
            throw iter.nextTokenIOExc;
        } else {
            String msg = String.format("EOT on input where %s expected",
                                       expected);
            throw new ParseException(msg);
        }
        if (expected == Token.Type.any || tok.type == expected) {
            return tok;
        } else {
            String msg = String.format("%s on input where %s expected",
                                       tok.type, expected);
            throw new ParseException(msg);
        }
    }

    protected Token peekToken(TokenIterator iter, Token.Type expected)
    throws ParseException, IOException {
        if (expected == Token.Type.any) {
            if (iter.lookaheadToken == null && iter.hasNext()) {
                iter.lookaheadToken = iter.next();
            }
            if (iter.nextTokenIOExc != null) throw iter.nextTokenIOExc;
            return iter.lookaheadToken;
        } else {
            if (iter.lookaheadToken != null || iter.hasNext()) {
                Token tok = nextToken(iter, Token.Type.any);
                if (tok.type == expected) {
                    return tok;
                } else {
                    iter.lookaheadToken = tok;
                }
            }
        }
        return null;
    }

    public void parseFile(File file, Boolean ignoreErrors)
    throws ParseException, IOException {
        parseFile(new FileReader(file), ignoreErrors);
    }

    public void parseFile(String file, Boolean ignoreErrors)
    throws ParseException, IOException {
        parseFile(new FileReader(file), ignoreErrors);
    }

    public void parseFile(Reader input, Boolean ignoreErrors)
    throws ParseException, IOException {
        BufferedReader buffered = new BufferedReader(input);
        String line;
        Integer lineIndex = 0;
        while ((line = buffered.readLine()) != null) {
            ++lineIndex;
            switch (line.charAt(0)) {
            case '+': case '-':
                try {
                    parse(line);
                } catch(ParseException e) {
                    e.setFaultyInput(line);
                    e.setFaultyInputIndex(lineIndex);
                    LoggingAssistant.logException(e);
                    if (!ignoreErrors) throw e;
                }
            }
        }
    }

    public void parseAll(List<String> inputs, Datum.Op defaultOp,
                         Boolean ignoreErrors)
    throws ParseException, IOException {
        int index = 0;
        for (String input : inputs) {
            try {
                ++index;
                parse(input, defaultOp);
            } catch(ParseException e) {
                e.setFaultyInput(input);
                e.setFaultyInputIndex(index);
                LoggingAssistant.logException(e);
                //e.printStackTrace();
                if (!ignoreErrors) throw e;
            }
        }
    }

    public Datum parse(String input) throws ParseException, IOException {
        Datum datum = parse(new StringReader(input));
        return datum;
    }

    private Datum.Op defaultOp = Datum.Op.none;

    public Datum parse(Reader input) throws ParseException, IOException {
        TokenIterator iter = new TokenIterator(input);
        Token tok = peekToken(iter, Token.Type.any);
        if (tok != null) {
            Datum datum = new Datum();
            datum.op = defaultOp;
            switch (tok.type) {
            case add:
                datum.op = Datum.Op.add;
                nextToken(iter, tok.type);
                break;
            case del:
                datum.op = Datum.Op.del;
                nextToken(iter, tok.type);
                break;
            }
            datum.term = parseCompoundTerm(iter);
            tok = peekToken(iter, Token.Type.any);
            if (tok == null) {
                return datum;
            } else {
                String msg = String.format("%s on input where EOT expected",
                                           tok.type);
                throw new ParseException(msg);
            }
        } else {
            throw new ParseException("Empty input");
        }
    }

    public Datum parse(String input, Datum.Op defaultOp)
    throws ParseException, IOException {
        assert (defaultOp == Datum.Op.add || defaultOp == Datum.Op.del);
        Datum.Op defaultOpTmp = this.defaultOp;
        this.defaultOp = defaultOp;
        Datum ret = null;
        try {
            ret = parse(input);
        } finally {
            this.defaultOp = defaultOpTmp;
        }
        return ret;
    }

    CompoundTerm parseCompoundTerm(TokenIterator iter)
    throws ParseException, IOException {
        CompoundTerm term = new CompoundTerm();
        term.functor = (String) (nextToken(iter, Token.Type.word).value);
        term.args = new ArrayList<Term>();
        if (peekToken(iter, Token.Type.lpar) != null) {
            while (true) {
                term.args.add(parseTerm(iter));
                if (peekToken(iter, Token.Type.comma) != null) {
                    continue;
                } else if (peekToken(iter, Token.Type.rpar) != null) {
                    break;
                } else {
                    Token tok = nextToken(iter, Token.Type.any);
                    String msg =
                        String.format("%s on input where %s or %s expected",
                                      tok.type, Token.Type.comma,
                                      Token.Type.rpar);
                    throw new ParseException(msg);
                }
            }
        }
        return term;
    }

    ListTerm parseListTerm(TokenIterator iter)
    throws ParseException, IOException {
        ListTerm term = new ListTerm();
        term.elements = new ArrayList<Term>();
        nextToken(iter, Token.Type.lbr);
        if (peekToken(iter, Token.Type.rbr) == null) {
            while (true) {
                term.elements.add(parseTerm(iter));
                if (peekToken(iter, Token.Type.comma) != null) {
                    continue;
                } else if (peekToken(iter, Token.Type.rbr) != null) {
                    break;
                } else {
                    Token tok = nextToken(iter, Token.Type.any);
                    String msg =
                        String.format("%s on input where %s or %s expected",
                                      tok.type,
                                      Token.Type.comma, Token.Type.rbr);
                    throw new ParseException(msg);
                }
            }
        }
        return term;
    }

    Term parseTerm(TokenIterator iter)
    throws ParseException, IOException {
        Token tok = peekToken(iter, Token.Type.any);
        Term term = null;
        if (tok == null) {
            nextToken(iter, Token.Type.any); // In order to get an error.
        } else {
            switch (tok.type) {
            case number:
                term = new NumberTerm();
                ((NumberTerm) term).value = (Double) (tok.value);
                nextToken(iter, Token.Type.number);
                break;
            case string:
                term = new StringTerm();
                ((StringTerm) term).value = (String) (tok.value);
                nextToken(iter, Token.Type.string);
                break;
            case word:
                term = parseCompoundTerm(iter);
                break;
            case lbr:
                term = parseListTerm(iter);
                break;
            default:
                String msg =
                    String.format("%s on input where %s, %s, %s or %s expected",
                                  tok.type, Token.Type.number,
                                  Token.Type.string, Token.Type.word,
                                  Token.Type.lbr);
                throw new ParseException(msg);
            }
        }
        return term;
    }
}
