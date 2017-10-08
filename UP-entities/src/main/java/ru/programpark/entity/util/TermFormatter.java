package ru.programpark.entity.util;


import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class TermFormatter {

    protected interface Getter<T> {
        T get();
    }

    protected interface Format {
        BaseParser.Term unfold();
    }

    protected class StringFormat
    extends BaseParser.StringTerm implements Format, Getter<String> {
        public StringFormat() {}

        public StringFormat(BaseParser.StringTerm proto) {
            this.value = proto.value;
        }

        public StringFormat(String value) {
            this.value = value;
        }

        BaseParser.Term unfold(String s) {
            BaseParser.StringTerm term = new BaseParser.StringTerm();
            term.value = s;
            return (BaseParser.Term) term;
        }

        @Override
        public BaseParser.Term unfold() {
            return unfold(get());
        }

        @Override
        public String get() { return null; }
    }

    protected class NumberFormat<T extends Number>
    extends BaseParser.NumberTerm implements Format, Getter<T> {
        public NumberFormat() {}

        public NumberFormat(BaseParser.NumberTerm proto) {
            this.value = proto.value;
        }

        public NumberFormat(Number value) {
            this.value = value.doubleValue();
        }

        BaseParser.Term unfold(T x) {
            BaseParser.NumberTerm term = new BaseParser.NumberTerm();
            term.value = new Double(x.doubleValue());
            return (BaseParser.Term) term;
        }

        @Override
        public BaseParser.Term unfold() {
            return unfold(get());
        }

        @Override
        public T get() { return null; }
    }

    protected class VanillaCompoundFormat
    extends BaseParser.CompoundTerm implements Format {
        @Override
        public BaseParser.Term unfold() {
            BaseParser.CompoundTerm term = new BaseParser.CompoundTerm();
            term.functor = this.functor;
            int arity = this.arity();
            term.args = new ArrayList<BaseParser.Term>(arity);
            for (int i = 0; i < arity; ++i) {
                Format arg = (Format) this.argument(i);
                term.args.add(arg.unfold());
            }
            return (BaseParser.Term) term;
        }
    }

    protected abstract class CompoundFormat<T>
    extends VanillaCompoundFormat implements Format, Getter<T> {
        public T object;

        @Override
        public BaseParser.Term unfold() {
            object = get();
            return (object == null) ? null : super.unfold();
        }

        @Override
        public T get() { return null; }
    }

    protected abstract class MultiFormat<T>
    extends CompoundFormat<T> implements Format, Getter<T> {
        Iterator<T> iterator;

        public abstract Iterator<T> getIterator();

        public void resetIterator() {
            iterator = null;
        }

        // Адская рефлективная хреновина, чтобы заставить вложенные
        // мультиформаты установить новые итераторы из следующего значения
        // итератора внешнего.
        private void resetNestedIterators() {
            Field[] fields = this.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                Field fld = fields[i];
                if (! fld.isSynthetic() &&
                        MultiFormat.class.isAssignableFrom(fld.getType())) {
                    try {
                        fld.setAccessible(true);
                    } catch (SecurityException e) {}
                    try {
                        MultiFormat<Object> fmt =
                            (MultiFormat<Object>) (fld.get(this));
                        fmt.resetIterator();
                    } catch (IllegalAccessException e) {}
                }
            }
        }

        @Override
        public T get() {
            if (iterator == null) iterator = getIterator();
            resetNestedIterators();
            return iterator.hasNext() ? iterator.next() : null;
        }
    }

    protected class ListFormat
    extends BaseParser.ListTerm implements Format {
        public ListFormat() {}
        public ListFormat(Collection elements) {
            initialize(elements);
        }
        public void initialize(Collection elements) {
            this.elements = new ArrayList<>();
            for (Object element : elements) {
                if (element instanceof Number) {
                    this.elements.add(new NumberFormat((Number) element));
                } else if (element instanceof String) {
                    this.elements.add(new StringFormat((String) element));
                } else if (element instanceof Collection) {
                    this.elements.add(new ListFormat((Collection) element));
                } else {
                    throw new RuntimeException("Invalid initializer for ListTerm:" +
                            elements);
                }
            }
        }
        private BaseParser.Term unfold_i(int i) {
            Format fmt = (Format) this.nth(i);
            try {
                return fmt.unfold();
            } catch (NullPointerException e) {
                //e.printStackTrace();
                return fmt.unfold();
            }
        }
        @Override
        public BaseParser.Term unfold() {
            BaseParser.ListTerm term = new BaseParser.ListTerm();
            int len = this.length();
            term.elements = new ArrayList<BaseParser.Term>(len);
            if (len > 0) {
                BaseParser.Term sub;
                for (int i = 0;
                     (sub = /*((Format) this.nth(i)).unfold()*/unfold_i(i)) != null;
                     i = (i + 1) % len) {
                    term.elements.add(sub);
                };

            }
            return (BaseParser.Term) term;
        }
    }

    private Map<String, Format> dataFormats = new HashMap<>();

    public String format(String key) {
        Format fmt = dataFormats.get(key);
        BaseParser.Term term = (fmt == null) ? null : fmt.unfold();
        return (term == null) ? null : term.toString();
    }

    private interface StringAppender {
        void append(String s) throws Exception;
    }

    private boolean formatOne(Format fmt, StringAppender appender) {
        try {
            BaseParser.Term term = fmt.unfold();
            if (term == null) return false;
            else appender.append(term.toString());
        } catch (Exception e) {
            LoggingAssistant.logException(e);
            //e.printStackTrace();
        }
        return true;
    }

    public void formatAll(String key, StringAppender appender) {
        Format fmt = dataFormats.get(key);
        if (fmt != null) {
            if (fmt instanceof MultiFormat) {
                while (formatOne(fmt, appender)) {}
            } else {
                formatOne(fmt, appender);
            }
        }
    }

    public void formatAll(String key, final List<String> outputs) {
        StringAppender lAppender = new StringAppender() {
            public void append(String s) {
                outputs.add(s);
            }
        };
        formatAll(key, lAppender);
    }

    public void formatAll(List<String> keys, List<String> outputs) {
        for (String key : keys) {
            formatAll(key, outputs);
        }
    }

    public void formatAll(String key, Writer output) {
        final BufferedWriter buffered = new BufferedWriter(output);
        StringAppender wAppender = new StringAppender() {
            public void append(String s) throws Exception {
                buffered.append(s);
                buffered.newLine();
                buffered.flush();
            }
        };
        formatAll(key, wAppender);
    }

    public void formatAll(List<String> keys, Writer output) {
        for (String key : keys) formatAll(key, output);
    }

    public void formatToFile(List<String> keys, File file)
    throws IOException {
        formatAll(keys, new FileWriter(file));
    }

    public void formatToFile(List<String> keys, String file)
    throws IOException {
        formatAll(keys, new FileWriter(file));
    }

    private Format formatify(BaseParser.Term term0,
                             Map<String, Format> formatMap) {
        if (term0 instanceof BaseParser.StringTerm) {
            return new StringFormat((BaseParser.StringTerm) term0);
        } else if (term0 instanceof BaseParser.NumberTerm) {
            return new NumberFormat((BaseParser.NumberTerm) term0);
        } else if (term0 instanceof BaseParser.CompoundTerm) {
            BaseParser.CompoundTerm term = (BaseParser.CompoundTerm) term0;
            int arity = term.arity();
            if (arity == 0 && term.functor.matches("^[A-Z].*")) {
                Format fmt = formatMap.get(term.functor);
                assert fmt != null;
                return fmt;
            } else {
                VanillaCompoundFormat fmt = null;
                if (! (arity == 1 &&
                           term.args.get(0) instanceof BaseParser.ListTerm) &&
                        formatMap.containsKey(term.functor)) {
                    fmt = (VanillaCompoundFormat) formatMap.get(term.functor);
                } else {
                    fmt = new VanillaCompoundFormat();
                }
                return formatify(term, fmt, formatMap);
            }
        } else if (term0 instanceof BaseParser.ListTerm) {
            BaseParser.ListTerm term = (BaseParser.ListTerm) term0;
            int len = term.length();
            return formatify(term, new ListFormat(), formatMap);
        } else {
            return null;
        }
    }

    private Format formatify(BaseParser.CompoundTerm term,
                             VanillaCompoundFormat fmt,
                             Map<String, Format> formatMap) {
        fmt.functor = term.functor;
        fmt.args = term.args;
        for (int i = term.arity() - 1; i >=0; --i) {
            Format fArg = formatify(term.args.get(i), formatMap);
            fmt.args.set(i, (BaseParser.Term) fArg);
        }
        return fmt;
    }

    private Format formatify(BaseParser.ListTerm term,
                             ListFormat fmt,
                             Map<String, Format> formatMap) {
        fmt.elements = term.elements;
        for (int i = term.length() - 1; i >=0; --i) {
            Format fElt = formatify(term.elements.get(i), formatMap);
            fmt.elements.set(i, (BaseParser.Term) fElt);
        }
        return fmt;
    }

    BaseParser formParser = new BaseParser();

    protected void addFormat(String form, CompoundFormat getter,
                              Object... dynamics)
    throws IOException, BaseParser.ParseException {
        BaseParser.CompoundTerm term0 = formParser.parse(form).term;
        Map<String, Format> formatMap = new HashMap<String, Format>();
        int i = 0, nDynamics = dynamics.length;
        assert nDynamics % 2 == 0;
        while (i < nDynamics) {
            String key = (String) (dynamics[i++]);
            formatMap.put(key, (Format) (dynamics[i++]));
        }
        Format fmt0;
        if (getter == null) {
            fmt0 = formatify(term0, formatMap);
        } else {
            fmt0 = formatify(term0, getter, formatMap);
        }
        String functor = term0.functor;
        if (functor.equals("tell")) {
            assert term0.arity() >= 1;
            BaseParser.Term term1 = term0.argument(0);
            assert term1 instanceof BaseParser.CompoundTerm;
            functor = ((BaseParser.CompoundTerm) term1).functor;
        }
        dataFormats.put(functor, fmt0);
    }

    public Map<String, Format> getFormats() {
        return dataFormats;
    }

}

