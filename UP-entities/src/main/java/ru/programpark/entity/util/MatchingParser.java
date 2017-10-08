package ru.programpark.entity.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class MatchingParser extends BaseParser {
    private Datum currentDatum = null;

    protected interface Pattern {
        Boolean fit(Term term);
    }

    protected class StringPattern extends StringTerm implements Pattern {
        StringPattern(StringTerm proto) {
            this.value = proto.value;
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ StringPattern " + this + ".fit(" + occur + ")");
            if (occur instanceof StringTerm) {
                return this.value.equals(((StringTerm) occur).value);
            } else {
                return false;
            }
        }
    }

    protected class NumberPattern extends NumberTerm implements Pattern {
        NumberPattern(NumberTerm proto) {
            this.value = proto.value;
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ NumberPattern " + this + ".fit(" + occur + ")");
            if (occur instanceof NumberTerm) {
                return this.value.equals(((NumberTerm) occur).value);
            } else {
                return false;
            }
        }
    }

    protected class ListPattern extends ListTerm implements Pattern {
        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ ListPattern " + this + ".fit(" + occur + ")");
            if (occur instanceof ListTerm) {
                ListTerm lOccur = (ListTerm) occur;
                int pLen = this.length();
                int oLen = lOccur.length();
                int i;
                if (pLen == 1) {
                    Pattern sub = (Pattern) this.nth(0);
                    for (i = 0; i < oLen; ++i) {
                        if (! sub.fit(lOccur.nth(i))) return false;
                    }
                } else {
                    if (oLen != pLen) return false;
                    for (i = 0; i < oLen; ++i) {
                        Pattern sub = (Pattern) this.nth(i);
                        if (! sub.fit(lOccur.nth(i))) return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    protected class CompoundPattern extends CompoundTerm implements Pattern {
        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ CompoundPattern " + this + ".fit(" + occur + ")");
            if (occur instanceof CompoundTerm) {
                CompoundTerm cOccur = (CompoundTerm) occur;
                if (cOccur.functor != this.functor) return false;
                int arity = cOccur.arity();
                if (arity != this.arity()) return false;
                for (int i = 0; i < arity; ++i) {
                    Pattern arg = (Pattern) this.argument(i);
                    if (! arg.fit(cOccur.argument(i))) return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    protected abstract class Wrapper extends CompoundPattern implements Pattern {
        public abstract void open();
        public abstract void close();

        @Override
        public String toString() {
            return "<" +
                this.getClass().getName().replaceFirst(".*[.$](?=.*[^0-9].*)", "") +
                " " + super.toString() + ">";
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ Wrapper " + this + ".fit(" + occur + ")");
            if (occur instanceof CompoundTerm) {
                this.open();
                Boolean ret = super.fit(occur);
                if (ret) this.close();
                return ret;
            } else {
                return false;
            }
        }
    }

    private Type getParameterType(Object o) {
        Class c = o.getClass();
        Type t;
        while (! ((t  = c.getGenericSuperclass()) instanceof ParameterizedType)) {
            c = c.getSuperclass();
        }
        return ((ParameterizedType) t).getActualTypeArguments()[0];
    }

    protected abstract class Setter<T> extends Wrapper {
        public T object;
        private Class<T> objectClass;

        public Setter() {
            this.objectClass = (Class<T>) getParameterType(this);
        }

        public void open() {
            // System.err.println("@@@ Setter " + this + ".open()");
            try {
                object = objectClass.newInstance();
            } catch (ReflectiveOperationException e) {
                // System.err.println("@@@ Setter.open caught " + e);
                LoggingAssistant.logException(e);
                // e.printStackTrace();
            }
        }

        public void add(T datum) {
            String msg =
                String.format("Datum %s can't be added",
                              currentDatum.term.toString());
            throw new ParseException(msg);
        }

        public void del(T datum) {
            String msg =
                String.format("Datum %s can't be deleted",
                              currentDatum.term.toString());
            throw new ParseException(msg);
        }

        public void close() {
            switch (currentDatum.op) {
            case add:
                this.add(object); break;
            case del:
                this.del(object); break;
            default:
                String msg =
                    String.format("Datum %s is being neither added nor deleted",
                                  currentDatum.toString());
                throw new ParseException(msg);
            }
        }
    }

    protected abstract class TrivialAdder<T> extends Setter<T> {
        public void open() {}
        public void add(T noObject) {}
    }

    protected class UnionPattern extends Term implements Pattern {
        public List<Pattern> options = new ArrayList<Pattern>();

        @Override
        public String toString() {
            String ret = "{";
            for (Pattern option : options) {
                ret += option.toString() + " ∪ ";
            }
            return ret.replaceFirst("( ∪ )?$", "}");
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ Union " + this + ".fit(" + occur + ")");
            for (Pattern option : this.options) {
                if (option.fit(occur)) return true;
            }
            return false;
        }
    }

    protected class Wildcard extends Term implements Pattern {
        @Override
        public String toString() {
            return "<_>";
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ Wildcard " + this + ".fit(" + occur + ")");
            return true;
        }
    }

    protected abstract class Matcher<T extends Term>
    extends Term implements Pattern {
        private Class<T> targetClass;

        public Matcher() {
            this.targetClass = (Class<T>) getParameterType(this);
        }

        @Override
        public String toString() {
            return "<" +
                this.getClass().getName().replaceFirst(".*[.$](?=.*[^0-9].*)", "") +
                ">";
        }

        @Override
        public Boolean fit(Term occur) {
            // System.err.println("@@@ Matcher " + this + ".fit(" + occur + ")");
            if (targetClass.isInstance(occur)) {
                Boolean ret = this.match(targetClass.cast(occur));
                return ret;
            } else {
                return false;
            }
        }

        public Boolean match(T term) {
            // System.err.println("@@@ Matcher " + this + ".match(" + term + ")");
            return false;
        }
    }


    private Map<String, UnionPattern> termPatterns =
        new HashMap<String, UnionPattern>();

    private Boolean disableFitting = false;

    Boolean fit(Term occur, Pattern pattern) {
        // System.err.println("@@@ fit(" + occur + ", " + pattern + ")");
        try {
            return pattern.fit(occur);
        } catch (Exception e) {
            // System.err.println("@@@ fit caught " + e);
            // e.printStackTrace();
            LoggingAssistant.logException(e);
            return false;
        }
    }

    protected void parse(CompoundTerm term) throws ParseException {
        UnionPattern pattern = termPatterns.get(term.functor);
        if (pattern == null) {
            String msg =
                String.format("Unknown message type: %s", term.functor);
            throw new ParseException(msg);
        } else if (! fit(term, pattern)) { //присвоение значения
            String msg =
                String.format("%s does not fit any known pattern of %s messages",
                              term, term.functor);
            throw new ParseException(msg);
        }
    }

    @Override
    public Datum parse(Reader input) throws ParseException, IOException {
        Datum datum = super.parse(input);
        currentDatum = datum;
        if (!disableFitting) parse(datum.term);
        return datum;
    }

    public Datum parse(Reader input, Boolean disableFitting)
    throws ParseException, IOException {
        Datum ret = null;
        Boolean disableFittingTmp = false;
        try {
            disableFittingTmp = this.disableFitting;
            this.disableFitting = disableFitting;
            ret = parse(input);
        } finally {
            this.disableFitting = disableFittingTmp;
        }
        return ret;
    }

    public Datum parse(String input, Boolean disableFitting)
    throws ParseException, IOException {
        return parse(new StringReader(input), disableFitting);
    }

    private Pattern patternify(Term term0,
                               Map<String, Matcher<Term>> matcherMap,
                               Map<String, Wrapper> wrapperMap) {
        if (term0 instanceof StringTerm) {
            return new StringPattern((StringTerm) term0);
        } else if (term0 instanceof NumberTerm) {
            return new NumberPattern((NumberTerm) term0);
        } else if (term0 instanceof CompoundTerm) {
            CompoundTerm term = (CompoundTerm) term0;
            if (term.arity() == 0 && term.functor.matches("^[A-Z].*")) {
                Matcher<Term> matcher = matcherMap.get(term.functor);
                assert matcher != null;
                return matcher;
            } else if (term.arity() == 0 && term.functor.equals("_")) {
                return new Wildcard();
            } else {
                CompoundPattern pattern = null;
                if (wrappable(term) && wrapperMap.containsKey(term.functor)) {
                    pattern = (CompoundPattern) wrapperMap.get(term.functor);
                } else {
                    pattern = new CompoundPattern();
                }
                return patternify(term, pattern, matcherMap, wrapperMap);
            }
        } else if (term0 instanceof ListTerm) {
            ListTerm term = (ListTerm) term0;
            int len = term.length();
            if (len == 1) {
                return patternify(term, new ListPattern(),
                        matcherMap, wrapperMap);
            } else {
                return patternify(term, new UnionPattern(),
                        matcherMap, wrapperMap);
            }
        } else {
            return new Pattern() {
                public Boolean fit(Term term) {
                    return false;
                }
            };
        }
    }

    private boolean wrappable(CompoundTerm term) {
        if (term.arity() == 1 && term.args.get(0) instanceof ListTerm) {
            ListTerm sub = (ListTerm) (term.args.get(0));
            for (int i = sub.length() - 1; i >= 0; --i) {
                if (sub.elements.get(i) instanceof CompoundTerm) {
                    CompoundTerm subsub = (CompoundTerm) (sub.elements.get(i));
                    if (subsub.functor.equals(term.functor)) return false;
                }
            }
        }
        return true;
    }

    private Pattern patternify(CompoundTerm term, CompoundPattern pattern,
                               Map<String, Matcher<Term>> matcherMap,
                               Map<String, Wrapper> wrapperMap) {
        pattern.functor = term.functor;
        pattern.args = term.args;
        for (int i = term.arity() - 1; i >=0; --i) {
            Pattern pArg =
                patternify(term.args.get(i), matcherMap, wrapperMap);
            pattern.args.set(i, (Term) pArg);
        }
        return pattern;
    }

    private Pattern patternify(ListTerm term, ListPattern pattern,
                               Map<String, Matcher<Term>> matcherMap,
                               Map<String, Wrapper> wrapperMap) {
        pattern.elements = term.elements;
        for (int i = term.length() - 1; i >=0; --i) {
            Pattern pElt =
                patternify(term.elements.get(i), matcherMap, wrapperMap);
            pattern.elements.set(i, (Term) pElt);
        }
        return pattern;
    }

    private Pattern patternify(ListTerm term, UnionPattern union,
                               Map<String, Matcher<Term>> matcherMap,
                               Map<String, Wrapper> wrapperMap) {
        for (int i = term.length() - 1; i >= 0; --i) {
            Pattern option =
                patternify(term.elements.get(i), matcherMap, wrapperMap);
            union.options.add(option);
        }
        ListPattern pattern = new ListPattern();
        pattern.elements = Collections.singletonList((Term) union);
        return pattern;
    }

    protected void addPattern(String form, Setter<?> setter,
                              Object... dynamics)
    throws IOException, ParseException {
        CompoundTerm term0 = parse(form, true).term;
        Map<String, Matcher<Term>> matcherMap =
            new HashMap<String, Matcher<Term>>();
        Map<String, Wrapper> wrapperMap =
            new HashMap<String, Wrapper>();
        int i = 0, nDynamics = dynamics.length;
        assert nDynamics % 2 == 0;
        while (i < nDynamics) {
            String key = (String) (dynamics[i++]);
            if (key.matches("^[A-Z].*")) {
                matcherMap.put(key, (Matcher<Term>) (dynamics[i++]));
            } else {
                wrapperMap.put(key, (Wrapper) (dynamics[i++]));
            }
        }
        addPattern(term0.functor,
                   patternify(term0, setter, matcherMap, wrapperMap));
    }

    protected void addPattern(String form)
    throws IOException, ParseException {
        CompoundTerm term0 = parse(form, true).term;
        Map<String, Matcher<Term>> matcherMap = Collections.emptyMap();
        Map<String, Wrapper> wrapperMap = Collections.emptyMap();
        addPattern(term0.functor, patternify(term0, matcherMap, wrapperMap));
    }

    protected void addPattern(String key, Pattern pattern) {
        UnionPattern union = termPatterns.get(key);
        if (union == null) {
            termPatterns.put(key, (union = new UnionPattern()));
        }
        union.options.add(pattern);
    }

    public Map<String, UnionPattern> getPatterns() {
        return termPatterns;
    }

}

