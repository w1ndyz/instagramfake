package com.instagramfake.util;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class JsonExecutor implements Iterable<JsonExecutor> {
    private static final String SPLIT_STR = "->";
    private final JsonElement root;

    public JsonExecutor(String json) throws JsonParseException {
        this(JsonParser.parseString(json));
    }

    private JsonExecutor(JsonElement root) {
        this.root = root;
    }

    public JsonExecutor execute(String expression) {
        List<Expression> expressionList = parseExpressions(expression);
        JsonElement executed = null;
        for (Expression exp : expressionList) {
            if (executed == null) {
                executed = exp.get(root);
            } else {
                executed = exp.get(executed);
            }
        }
        return new JsonExecutor(executed);
    }

    public String getAsString() {
        return this.root.getAsString();
    }

    public boolean getAsBoolean() {
        return this.root.getAsBoolean();
    }

    public boolean isPresent() {
        return !this.root.isJsonNull();
    }

    public int getAsInt() {
        return this.root.getAsInt();
    }

    public double getAsDouble() {
        return this.root.getAsDouble();
    }

    public JsonExecutor last() {
        if (this.root.isJsonArray()) {
            JsonArray array = this.root.getAsJsonArray();
            return new JsonExecutor(array.get(array.size() - 1));
        }
        return this;
    }

    private static List<Expression> parseExpressions(String exp) {
        String expression = exp.replaceAll("\\s+", "");
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("expression can not be empty!");
        }
        if (expression.contains(SPLIT_STR)) {
            // multi expressions
            List<Expression> expressionList = new ArrayList<>();
            for (String _expression : expression.split(SPLIT_STR)) {
                _expression = _expression.replaceAll("\\s+", "");
                if (_expression.isEmpty()) {
                    throw new IllegalArgumentException("expression can not be empty!");
                }
                Expression parsed = parseExpression(_expression);
                expressionList.add(parsed);
            }
            return expressionList;
        }
        return Collections.singletonList(parseExpression(expression));
    }

    private static Expression parseExpression(String expression) {
        String indexStr = substringBetween(expression);
        if (indexStr != null) {
            int index = Integer.parseInt(indexStr);
            String _expression = expression.substring(0, expression.indexOf('[')).trim();
            if (_expression.isEmpty()) {
                throw new IllegalArgumentException("can not parse expression :" + expression);
            }
            return new ArrayExpression(_expression, index);
        } else {
            return new Expression(expression);
        }
    }

    public int getSize() {
        return this.root.isJsonArray() ? this.root.getAsJsonArray().size() : 0;
    }

    @Override
    public Iterator<JsonExecutor> iterator() {
        Iterator<JsonElement> elementIterator = this.root.isJsonArray() ? this.root.getAsJsonArray().iterator() : Collections.emptyIterator();
        return new Iterator<>() {

            @Override
            public JsonExecutor next() {
                return new JsonExecutor(elementIterator.next());
            }

            @Override
            public boolean hasNext() {
                return elementIterator.hasNext();
            }
        };
    }

    @Override
    public void forEach(Consumer<? super JsonExecutor> action) {
        if (this.root.isJsonArray()) {
            this.root.getAsJsonArray().forEach(jsonElement -> action.accept(new JsonExecutor(jsonElement)));
        }
    }

    private static class Expression {
        protected final String expression;

        public Expression(String expression) {
            super();
            this.expression = expression;
        }

        JsonElement get(JsonElement ele) {
            if (ele.isJsonObject()) {
                JsonObject jo = ele.getAsJsonObject();
                if (jo.has(expression)) {
                    return jo.get(expression);
                }
            }
            return JsonNull.INSTANCE;
        }
    }

    private static class ArrayExpression extends Expression {
        private final int index;

        public ArrayExpression(String expression, int index) {
            super(expression);
            this.index = index;
        }

        @Override
        JsonElement get(JsonElement ele) {
            if (ele.isJsonObject()) {
                JsonObject jo = ele.getAsJsonObject();
                if (jo.has(expression)) {
                    JsonElement expressionEle = jo.get(expression);
                    if (expressionEle.isJsonArray()) {
                        JsonArray array = expressionEle.getAsJsonArray();
                        if (index >= 0 && index <= array.size() - 1) {
                            return array.get(index);
                        }
                    }
                }
            }
            return JsonNull.INSTANCE;
        }
    }

    @Override
    public String toString() {
        return this.root.toString();
    }

    private static String substringBetween(final String str) {
        if (str == null) {
            return null;
        }
        final int start = str.indexOf("[");
        if (start != -1) {
            final int end = str.indexOf("]", start + "[".length());
            if (end != -1) {
                return str.substring(start + "[".length(), end);
            }
        }
        return null;
    }
}
