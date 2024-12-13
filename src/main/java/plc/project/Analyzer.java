package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Analyzer implements Ast.Visitor<Environment.Type> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.Type visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        try {
            Environment.Function mainFunction = scope.lookupFunction("main", 0);
            if (!mainFunction.getReturnType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("main/0 must have an integer return type.");
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Program does not contain a valid main/0 function.");
        }
        return null;
    }
    @Override
    public Environment.Type visit(Ast.Field ast) {
        Environment.Type type;
        try {
            type = Environment.getType(ast.getTypeName());
        } catch (RuntimeException e) {
            throw new RuntimeException("Unknown type specified for field: " + ast.getTypeName());
        }

        if (ast.getValue().isPresent()) {
            Environment.Type valueType = visit(ast.getValue().get());
            requireAssignable(type, valueType);
        }

        Environment.Variable variable = scope.defineVariable(
                ast.getName(), ast.getName(), type, Environment.NIL
        );
        ast.setVariable(variable);

        return null;
    }

    @Override
    public Environment.Type visit(Ast.Method ast) {
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (String typeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(typeName));
        }
        Environment.Type returnType = ast.getReturnTypeName()
                .map(Environment::getType)
                .orElse(Environment.Type.NIL);
        Environment.Function function = scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL);
        ast.setFunction(function);
        Scope originalScope = scope;
        scope = new Scope(scope);
        Ast.Method previousMethod = this.method;
        this.method = ast;
        for (int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), Environment.NIL);
        }
        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
        }
        this.method = previousMethod;
        scope = originalScope;
        return null;
    }

    @Override
    public Environment.Type visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Only function calls are allowed as expression statements.");
        }
        visit(ast.getExpression());
        return Environment.Type.NIL;
    }

    @Override
    public Environment.Type visit(Ast.Stmt.Declaration ast) {
        Environment.Type type;
        if (ast.getTypeName().isPresent()) {
            try {
                type = Environment.getType(ast.getTypeName().get());
            } catch (RuntimeException e) {
                throw new RuntimeException("Unknown type specified: " + ast.getTypeName().get());
            }
        } else if (ast.getValue().isPresent()) {
            type = visit(ast.getValue().get());
        } else {
            throw new RuntimeException("Declaration must have either a type or an initializer.");
        }
        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL);
        ast.setVariable(variable);
        if (ast.getValue().isPresent()) {
            Environment.Type valueType = visit(ast.getValue().get());
            requireAssignable(type, valueType);
        }
        return null;
    }

    @Override
    public Environment.Type visit(Ast.Stmt.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            throw new RuntimeException("Invalid assignment target.");
        }
        Environment.Type receiverType = visit(ast.getReceiver());
        Environment.Type valueType = visit(ast.getValue());
        requireAssignable(receiverType, valueType);
        return null;
    }

    @Override
    public Environment.Type visit(Ast.Stmt.If ast) {
        Environment.Type conditionType = visit(ast.getCondition());
        if (!conditionType.equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("If statement condition must be of type Boolean.");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("If statement must have at least one then statement.");
        }
        Scope originalScope = scope;
        scope = new Scope(scope);
        for (Ast.Stmt stmt : ast.getThenStatements()) {
            visit(stmt);
        }
        scope = originalScope;
        scope = new Scope(scope);
        for (Ast.Stmt stmt : ast.getElseStatements()) {
            visit(stmt);
        }
        scope = originalScope;
        return null;
    }

    @Override
    public Environment.Type visit(Ast.Stmt.For ast) {
        Environment.Type iterableType = visit(ast.getValue());
        if (!iterableType.equals(Environment.Type.INTEGER_ITERABLE)) {
            throw new RuntimeException("The value of a for loop must be an integer iterable.");
        }
        Scope originalScope = scope;
        scope = new Scope(scope);
        scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
        }
        scope = originalScope;
        return null;
    }
    @Override
    public Environment.Type visit(Ast.Stmt.While ast) {
        Environment.Type conditionType = visit(ast.getCondition());
        if (!conditionType.equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("The condition of a while loop must be a boolean.");
        }
        Scope originalScope = scope;
        scope = new Scope(scope);
        for (Ast.Stmt statement : ast.getStatements()) {
            visit(statement);
        }
        scope = originalScope;
        return null;
    }
    @Override
    public Environment.Type visit(Ast.Stmt.Return ast) {
        // Ensure that the method context is available
        if (method == null) {
            throw new RuntimeException("Return statement must be inside a method.");
        }

        // Get the expected return type of the method
        Environment.Type returnType = method.getFunction().getReturnType();

        // Validate the return value's type
        Environment.Type valueType = visit(ast.getValue());
        requireAssignable(returnType, valueType);

        return null;
    }

    @Override
    public Environment.Type visit(Ast.Expr.Literal ast) {
        Object value = ast.getLiteral();
        if (value instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
            return Environment.Type.BOOLEAN;
        } else if (value instanceof BigInteger) {
            BigInteger bigIntValue = (BigInteger) value;
            if (bigIntValue.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
                    bigIntValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("Integer literal out of range");
            }
            ast.setType(Environment.Type.INTEGER);
            return Environment.Type.INTEGER;
        } else if (value instanceof Double) {
            ast.setType(Environment.Type.DECIMAL);
            return Environment.Type.DECIMAL;
        } else if (value instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
            return Environment.Type.CHARACTER;
        } else if (value instanceof String) {
            ast.setType(Environment.Type.STRING);
            return Environment.Type.STRING;
        }
        else if (ast.getLiteral() instanceof BigDecimal) {  // Add this case
            ast.setType(Environment.Type.DECIMAL);
            return Environment.Type.DECIMAL;
        }
            throw new RuntimeException("Unsupported literal type.");
    }

    @Override
    public Environment.Type visit(Ast.Expr.Group ast) {
        Environment.Type innerType = visit(ast.getExpression());
        ast.setType(innerType);
        return innerType;
    }

    @Override
    public Environment.Type visit(Ast.Expr.Binary ast) {
        Environment.Type leftType = visit(ast.getLeft());
        Environment.Type rightType = visit(ast.getRight());
        String operator = ast.getOperator();
        switch (operator) {
            case "AND":
            case "OR":
                if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                    throw new RuntimeException("Both operands of " + operator + " must be Boolean.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                return Environment.Type.BOOLEAN;
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                if (!leftType.equals(rightType) || !leftType.equals(Environment.Type.COMPARABLE)) {
                    throw new RuntimeException("Both operands of " + operator + " must be comparable and of the same type.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                return Environment.Type.BOOLEAN;
            case "+":
                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                    return Environment.Type.STRING;
                } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                    return Environment.Type.INTEGER;
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                    return Environment.Type.DECIMAL;
                } else {
                    throw new RuntimeException("Invalid types for addition.");
                }
            case "-":
            case "*":
            case "/":
                if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                    return Environment.Type.INTEGER;
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                    return Environment.Type.DECIMAL;
                } else {
                    throw new RuntimeException("Invalid types for arithmetic operator " + operator);
                }
            default:
                throw new RuntimeException("Unknown operator: " + operator);
        }
    }

    public Environment.Type visit(Ast.Expr.Access ast) {
        Environment.Variable variable;
        if (ast.getReceiver().isPresent()) {
            Environment.Type receiverType = visit(ast.getReceiver().get());
            Optional<Environment.Variable> fieldOpt = Optional.ofNullable(receiverType.getField(ast.getName()));
            if (!fieldOpt.isPresent()) {
                throw new RuntimeException("Field '" + ast.getName() + "' does not exist in type " + receiverType);
            }
            variable = fieldOpt.get();
        } else {
            variable = scope.lookupVariable(ast.getName());
        }
        ast.setVariable(variable);
        return variable.getType();
    }

    @Override
    public Environment.Type visit(Ast.Expr.Function ast) {
        Environment.Function function;
        if (ast.getReceiver().isPresent()) {
            Environment.Type receiverType = visit(ast.getReceiver().get());
            Optional<Environment.Function> methodOpt = Optional.ofNullable(receiverType.getMethod(ast.getName(), ast.getArguments().size()));
            if (!methodOpt.isPresent()) {
                throw new RuntimeException("Method '" + ast.getName() + "' does not exist in type " + receiverType);
            }
            function = methodOpt.get();
            if (function.getParameterTypes().size() != ast.getArguments().size() + 1) {
                throw new RuntimeException("Incorrect number of arguments for method '" + ast.getName() + "'.");
            }
            for (int i = 0; i < ast.getArguments().size(); i++) {
                Environment.Type argumentType = visit(ast.getArguments().get(i));
                requireAssignable(function.getParameterTypes().get(i + 1), argumentType);
            }
        } else {
            function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            for (int i = 0; i < ast.getArguments().size(); i++) {
                Environment.Type argumentType = visit(ast.getArguments().get(i));
                requireAssignable(function.getParameterTypes().get(i), argumentType);
            }
        }
        ast.setFunction(function);
        return function.getReturnType();
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(Environment.Type.ANY)) {
            return;
        } else if (target.equals(type)) {
            return;
        } else if (target.equals(Environment.Type.COMPARABLE) &&
                (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) ||
                        type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING))) {
            return;
        }
        throw new RuntimeException("Type " + type + " is not assignable to " + target + ".");
    }
}
