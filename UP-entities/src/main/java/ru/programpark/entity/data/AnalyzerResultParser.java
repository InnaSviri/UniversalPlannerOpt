package ru.programpark.entity.data;


import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.MatchingParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oracle on 16.10.2015.
 * я сделал этот класс, чтобы тягать данные из файла для проверки анализатора - Такмаз
 */

public class AnalyzerResultParser extends MatchingParser {

    public Map<Long, Double> analyzerValues = new HashMap<>();
    Long index;

    public AnalyzerResultParser(String[] addPercepts)
            throws IOException, ParseException {
        super();
        parseAll(Arrays.asList(addPercepts), Parser.Datum.Op.add, true);
        for (String s : addPercepts) {
            LoggingAssistant.getInputWriter().write(s + "\n");
        }
    }

    private class AnalyzerCheckValueSetter extends TrivialAdder<Long> {
        Matcher<NumberTerm> criterionNo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = term.longValue();
                analyzerValues.put(term.longValue(), 0.0);
                return true;
            }
        };

        Matcher<NumberTerm> criterionValue = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                analyzerValues.put(object,term.doubleValue());
                return true;
            }
        };
    }

    {
        final AnalyzerCheckValueSetter checkValueSetter = new AnalyzerCheckValueSetter();
        addPattern("analyzer_value(criterion_no(No),criterion_value(Value))", checkValueSetter,
                "No", checkValueSetter.criterionNo, "Value", checkValueSetter.criterionValue);

    }

}

