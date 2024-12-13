package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Ast.Stmt parseIfStatement() throws ParseException {
        advance();
        Ast.Expr condition = parseExpression();
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after IF condition.");
        List<Ast.Stmt> thenBranch = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "ELSE") && !check(Token.Type.IDENTIFIER, "END")) {
            thenBranch.add(parseStatement());
        }
        List<Ast.Stmt> elseBranch = new ArrayList<>();
        if (check(Token.Type.IDENTIFIER, "ELSE")) {
            advance();
            while (!check(Token.Type.IDENTIFIER, "END")) {
                elseBranch.add(parseStatement());
            }
        }
        return new Ast.Stmt.If(condition, thenBranch, elseBranch);
    }

    private Ast.Stmt parseReturnStatement() throws ParseException {
        advance();
        Ast.Expr condition = parseExpression();
        consume(Token.Type.OPERATOR, ";", "Expected ;");
        return new Ast.Stmt.Return(condition);
    }

//    private Ast.Stmt parseFORStatement() throws ParseException {
//        advance();
//        String loopVariable = peek().getLiteral();
//        advance();
//        consume(Token.Type.IDENTIFIER, "IN", "Expected 'IN' after loop variable.");
//        Ast.Expr iterable = parseExpression();
//        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after iterable expression.");
//        List<Ast.Stmt> thenBranch = new ArrayList<>();
//        while (!check(Token.Type.IDENTIFIER, "END")) {
//            thenBranch.add(parseStatement());
//        }
//        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the 'FOR' loop.");
//        return new Ast.Stmt.For(loopVariable, iterable, thenBranch);
//    }
private Ast.Stmt.For parseFORStatement() throws ParseException {
    advance();  // consume 'FOR'
    String loopVariable = peek().getLiteral();
    advance();  // consume variable name
    consume(Token.Type.IDENTIFIER, "IN", "Expected 'IN' after loop variable.");

    Ast.Expr iterable = parseExpression();
    consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after iterable expression.");

    List<Ast.Stmt> thenBranch = new ArrayList<>();
    while (!check(Token.Type.IDENTIFIER, "END")) {
        thenBranch.add(parseStatement());  // This will handle the statement and its semicolon
    }

    consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the 'FOR' loop.");
    return new Ast.Stmt.For(loopVariable, iterable, thenBranch);
}

    private Ast.Stmt parseLetStatement() throws ParseException {
        advance();
        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            String variable = peek().getLiteral();
            advance();
            try {
                if (matchOperator("=")) {
                    Ast.Expr value = parseExpression();
                    consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");
                    return new Ast.Stmt.Declaration(variable, Optional.of(value));
                } else if (matchOperator(";")) {
                    return new Ast.Stmt.Declaration(variable, Optional.empty());
                }
            } catch (ParseException e) {
                System.err.println("Error: " + e.getMessage());
                throw e;
            }
        }
        return new Ast.Stmt.Declaration(null, Optional.empty());
    }

    private Ast.Field parseFieldStatement() throws ParseException {
        advance();
        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            String variable = peek().getLiteral();
            advance();
            consume(Token.Type.OPERATOR, ":", "Expected ':' after identifier in field declaration.");
            String typeName = peek().getLiteral();
            consume(Token.Type.IDENTIFIER, typeName, "Expected type identifier after ':'.");
            Optional<Ast.Expr> value = Optional.empty();
            if (matchOperator("=")) {
                value = Optional.of(parseExpression());
            }
            consume(Token.Type.OPERATOR, ";", "Expected ';' after field declaration.");
            return new Ast.Field(variable, typeName, value);
        } else {
            throw error(peek(), "Expected identifier after 'LET'.");
        }
    }
//    public Ast.Field parseField() throws ParseException {
//        advance();
//        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
//            String variable = peek().getLiteral();
//            advance();
//            consume(Token.Type.OPERATOR, ":", "Expected ':' after identifier in field declaration.");
//            String typeName = peek().getLiteral();
//            consume(Token.Type.IDENTIFIER, typeName, "Expected type identifier after ':'.");
//            Optional<Ast.Expr> value = Optional.empty();
//            if (matchOperator("=")) {
//                value = Optional.of(parseExpression());
//            }
//            consume(Token.Type.OPERATOR, ";", "Expected ';' after field declaration.");
//            return new Ast.Field(variable, typeName, value);
//        } else {
//            throw error(peek(), "Expected identifier after 'LET'.");
//        }
//    }
public Ast.Field parseField() {
    advance(); // consume LET
    String name = consume(Token.Type.IDENTIFIER, "Expected field name").getLiteral();
    consume(Token.Type.OPERATOR, ":", "Expected ':' after field name");
    String type = consume(Token.Type.IDENTIFIER, "Expected type").getLiteral();
    Optional<Ast.Expr> value = Optional.empty();
    if (matchOperator("=")) {
        value = Optional.of(parseExpression());
    }
    consume(Token.Type.OPERATOR, ";", "Expected ';' after field declaration");
    return new Ast.Field(name, type, value);
}

    private Ast.Stmt parseWHILEStatement() throws ParseException {
        advance();
        Ast.Expr condition = parseExpression();
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after WHILE condition.");
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) {
            statements.add(parseStatement());
        }
        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the WHILE loop.");
        return new Ast.Stmt.While(condition, statements);
    }

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (!isAtEnd()) {
            if (matchLet("LET")) {
                fields.add(parseFieldStatement());
            } else if (matchDEF("DEF")) {
                methods.add(parseDefStatement());
            } else {
                throw error(peek(), "Unexpected statement.");
            }
        }
        return new Ast.Source(fields, methods);
    }

    private Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        advance();
        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            String variable = peek().getLiteral();
            advance();
            Optional<String> typeName = Optional.empty();
            if (matchOperator(":")) {
                if (!check(Token.Type.IDENTIFIER)) {
                    throw error(peek(), "Expected type identifier after ':'.");
                }
                typeName = Optional.of(peek().getLiteral());
                advance();
            }
            Optional<Ast.Expr> value = Optional.empty();
            if (matchOperator("=")) {
                value = Optional.of(parseExpression());
            }
            consume(Token.Type.OPERATOR, ";", "Expected ';' after declaration.");
            return new Ast.Stmt.Declaration(variable, typeName, value);
        } else {
            throw error(peek(), "Expected identifier after 'LET'.");
        }
    }

    private Ast.Method parseDefStatement() throws ParseException {
        advance();
        if (!check(Token.Type.IDENTIFIER)) {
            throw error(peek(), "Expected method name after 'DEF'.");
        }
        String methodName = peek().getLiteral();
        advance();
        consume(Token.Type.OPERATOR, "(", "Expected '(' after method name.");
        List<String> parameters = new ArrayList<>();
        if (!check(Token.Type.OPERATOR, ")")) {
            do {
                if (!check(Token.Type.IDENTIFIER)) {
                    throw error(peek(), "Expected parameter name.");
                }
                parameters.add(peek().getLiteral());
                advance();
            } while (matchOperator(","));
        }
        consume(Token.Type.OPERATOR, ")", "Expected ')' after parameters.");
        Optional<String> returnType = Optional.empty();
        if (matchOperator(":")) {
            if (!check(Token.Type.IDENTIFIER)) {
                throw error(peek(), "Expected return type identifier after ':'.");
            }
            returnType = Optional.of(peek().getLiteral());
            advance();
        }
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' to start the method body.");
        List<Ast.Stmt> bodyStatements = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) {
            bodyStatements.add(parseStatement());
        }
        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the method.");
        return new Ast.Method(methodName, parameters, new ArrayList<>(), returnType, bodyStatements);
    }

//    public Ast.Stmt parseStatement() throws ParseException {
//        if (matchLet("LET")) {
//            return parseDeclarationStatement();
//        }
//        if (matchReturn("RETURN")) {
//            return parseReturnStatement();
//        }
//        if (matchcondition("IF")) {
//            return parseIfStatement();
//        }
//        if (matchFor("FOR")) {
//            return parseFORStatement();
//        }
//        if (matchWhile("WHILE")) {
//            return parseWHILEStatement();
//        }
//        if (matchType(Token.Type.IDENTIFIER)) {
//            Token identifier = previous();
//            if (matchOperator("=")) {
//                Ast.Expr value = parseExpression();
//                consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment. this is equal to one");
//                return new Ast.Stmt.Assignment(
//                        new Ast.Expr.Access(Optional.empty(), identifier.getLiteral()), value
//                );
//            } else if (matchOperator("(")) {
//                List<Ast.Expr> arguments = parseArguments();
//                consume(Token.Type.OPERATOR, ";", "Expected ';' after function call.");
//                return new Ast.Stmt.Expression(
//                        new Ast.Expr.Function(Optional.empty(), identifier.getLiteral(), arguments)
//                );
//            } else {
//
//                consume(Token.Type.OPERATOR, ";", "Expected ';' after expression. the else one");
//                return new Ast.Stmt.Expression(
//                        new Ast.Expr.Access(Optional.empty(), identifier.getLiteral())
//                );
//            }
//        }
//        throw error(peek(), "Unexpected statement.");
//    }
public Ast.Stmt parseStatement() throws ParseException {
    if (matchLet("LET")) {
        return parseDeclarationStatement();
    }
    if (matchReturn("RETURN")) {
        return parseReturnStatement();
    }
    if (matchcondition("IF")) {
        return parseIfStatement();
    }
    if (matchFor("FOR")) {
        return parseFORStatement();
    }
    if (matchWhile("WHILE")) {
        return parseWHILEStatement();
    }
    if (matchType(Token.Type.IDENTIFIER)) {
        // First parse the expression (which could be a simple identifier or field access)
        Token start = previous();
        Ast.Expr expr = parseAccessExpression(start.getLiteral());
//else if (matchOperator("(")) {
//                List<Ast.Expr> arguments = parseArguments();
//                consume(Token.Type.OPERATOR, ";", "Expected ';' after function call.");
//                return new Ast.Stmt.Expression(
//                        new Ast.Expr.Function(Optional.empty(), identifier.getLiteral(), arguments)
//                );
//            }
        // Now handle assignments, function calls, or simple expressions
        if (matchOperator("=")) {
            Ast.Expr value = parseExpression();
            consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");
            return new Ast.Stmt.Assignment(expr, value);
        } else if (expr instanceof Ast.Expr.Function) {
            consume(Token.Type.OPERATOR, ";", "Expected ';' after function call.");
            return new Ast.Stmt.Expression(expr);
        }
        if (matchOperator("(")) {
                List<Ast.Expr> arguments = parseArguments();
                consume(Token.Type.OPERATOR, ";", "Expected ';' after function call.");
                return new Ast.Stmt.Expression(
                        new Ast.Expr.Function(Optional.empty(), start.getLiteral(), arguments)
                );
            }

        else {
            consume(Token.Type.OPERATOR, ";", "Expected ';' after expression.");
            return new Ast.Stmt.Expression(expr);
        }
    }
    throw error(peek(), "Unexpected statement.");
}
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    public Ast.Expr parseLogicalExpression() throws ParseException {
        return parseOrExpression();
    }

    private Ast.Expr parseOrExpression() throws ParseException {
        Ast.Expr expr = parseAndExpression();
        while (matchLogicalOperator("OR")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseAndExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseAndExpression() throws ParseException {
        Ast.Expr expr = parseComparisonExpression();
        while (matchLogicalOperator("AND")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseComparisonExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private boolean matchLogicalOperator(String... literals) {
        for (String literal : literals) {
            if ((check(Token.Type.OPERATOR, literal) || check(Token.Type.IDENTIFIER, literal))) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Ast.Expr parseComparisonExpression() throws ParseException {
        Ast.Expr expr = parseAdditiveExpression();
        while (matchOperator("<", ">", "<=", ">=", "==", "!=")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseAdditiveExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr expr = parseMultiplicativeExpression();
        while (matchOperator("+", "-")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }
    public Ast.Method parseMethod() {
        advance(); // consume DEF
        String name = consume(Token.Type.IDENTIFIER, "Expected method name").getLiteral();

        // Parse parameters
        consume(Token.Type.OPERATOR, "(", "Expected '(' after method name");
        List<String> parameters = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();  // Added to store parameter types

        if (!check(Token.Type.OPERATOR, ")")) {
            do {
                parameters.add(consume(Token.Type.IDENTIFIER, "Expected parameter name").getLiteral());
                // Handle parameter type
                consume(Token.Type.OPERATOR, ":", "Expected ':' after parameter name");
                parameterTypes.add(consume(Token.Type.IDENTIFIER, "Expected parameter type").getLiteral());
            } while (matchOperator(","));
        }
        consume(Token.Type.OPERATOR, ")", "Expected ')' after parameters");

        // Parse return type
        Optional<String> returnType = Optional.empty();
        if (matchOperator(":")) {
            returnType = Optional.of(consume(Token.Type.IDENTIFIER, "Expected return type").getLiteral());
        }

        // Parse method body
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after method header");

        List<Ast.Stmt> statements = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) {
            statements.add(parseStatement());
        }

        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close method");

        return new Ast.Method(name, parameters, parameterTypes, returnType, statements);
    }
    private Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expr = parseSecondaryExpression();
        while (matchOperator("*", "/")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr expr = parsePrimaryExpression();
        while (matchOperator(".")) {
            Token identifier = consume(Token.Type.IDENTIFIER, "Expected identifier after '.'.");
            if (matchOperator("(")) {
                expr = new Ast.Expr.Function(Optional.of(expr), identifier.getLiteral(), parseArguments());
            } else {
                expr = new Ast.Expr.Access(Optional.of(expr), identifier.getLiteral());
            }
        }
        return expr;
    }

    private Ast.Expr parseGroupExpression() throws ParseException {
        // Expect and consume opening parenthesis
        consume(Token.Type.OPERATOR, "(", "Expected opening parenthesis.");

        // Parse the expression inside the parentheses
        Ast.Expr expression = parseExpression();

        // Expect and consume closing parenthesis
        consume(Token.Type.OPERATOR, ")", "Expected closing parenthesis.");

        // Create and return the group expression
        return new Ast.Expr.Group(expression);
    }

    private Ast.Expr parseAccessExpression(String identifier) throws ParseException {
        Ast.Expr expr = new Ast.Expr.Access(Optional.empty(), identifier);

        while (matchOperator(".")) {
            // Get the field/method name
            Token field = consume(Token.Type.IDENTIFIER, "Expected field name after '.'");

            if (matchOperator("(")) {
                // Method call
                List<Ast.Expr> arguments = parseArguments();
                expr = new Ast.Expr.Function(Optional.of(expr), field.getLiteral(), arguments);
            } else {
                // Field access
                expr = new Ast.Expr.Access(Optional.of(expr), field.getLiteral());
            }
        }

        return expr;
    }
    private Ast.Expr parsePrimaryExpression() throws ParseException {
        if (matchType(Token.Type.IDENTIFIER)) {
            String identifier = previous().getLiteral();
            if (identifier.equals("NIL")) {
                return new Ast.Expr.Literal(null);
            }
            else if (identifier.equals("AND") || identifier.equals("OR")) {
                return parseLogicalExpression();
            }
           else  if (identifier.equals("TRUE") || identifier.equals("FALSE")) {
                return new Ast.Expr.Literal(Boolean.parseBoolean(identifier));
            }
          else  if (matchOperator("(")) {
                return new Ast.Expr.Function(Optional.empty(), identifier, parseArguments());
            }
//          else if (matchOperator(".")) {
//              return parseAccessExpression();
//            }
            return new Ast.Expr.Access(Optional.empty(), identifier);
        }
        if (matchType(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(previous().getLiteral()));
        }
        if (matchType(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(previous().getLiteral()));
        }
        if (matchType(Token.Type.CHARACTER)) {
            return new Ast.Expr.Literal(previous().getLiteral().charAt(1));
        }
        if (matchType(Token.Type.STRING)) {
            String literal = previous().getLiteral();
            literal = literal.substring(1, literal.length() - 1).replace("\\n", "\n");
            return new Ast.Expr.Literal(literal);
        }
        if (matchOperator("(")) { // Match opening parenthesis
            Ast.Expr expression = parseExpression(); // Parse the inner expression
            if (!matchOperator(")")) { // Check for and consume closing parenthesis
                throw error(peek(), "Expected expression.");
//                throw error("Expected ')' after expression.");
            }
            return new Ast.Expr.Group(expression); // Return the group expression
        }
        throw error(peek(), "Expected expression.");
    }

    private List<Ast.Expr> parseArguments() throws ParseException {
        List<Ast.Expr> arguments = new ArrayList<>();
        if (!check(Token.Type.OPERATOR, ")")) {
            do {
                arguments.add(parseExpression());
            } while (matchOperator(","));
        }
        consume(Token.Type.OPERATOR, ")", "Expected ')' after arguments.");
        return arguments;
    }

    private Token consume(Token.Type type, String literal, String message) throws ParseException {
        if (check(type, literal)) return advance();
        throw error(peek(), message);
    }

    private Token consume(Token.Type type, String message) throws ParseException {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean matchType(Token.Type... types) {
        int i = 0;
        while (i < types.length) {
            if (check(types[i])) {
                advance();
                return true;
            }
            i++;
        }
        return false;
    }

    private boolean matchOperator(String... literals) {
        for (String literal : literals) {
            if (check(Token.Type.OPERATOR, literal)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean matchloop() {
        if (check(Token.Type.IDENTIFIER, "FOR") || check(Token.Type.IDENTIFIER, "WHILE")) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchcondition(String... literals) {
        if (check(Token.Type.IDENTIFIER, "IF")) {
            return true;
        }
        return false;
    }

    private boolean matchReturn(String... literals) {
        if (check(Token.Type.IDENTIFIER, "RETURN")) {
            return true;
        }
        return false;
    }

    private boolean matchLet(String... literals) {
        if (check(Token.Type.IDENTIFIER, "LET")) {
            return true;
        }
        return false;
    }

    private boolean matchFor(String... literals) {
        if (check(Token.Type.IDENTIFIER, "FOR")) {
            return true;
        }
        return false;
    }

    private boolean matchDEF(String... literals) {
        if (check(Token.Type.IDENTIFIER, "DEF")) {
            return true;
        }
        return false;
    }

    private boolean matchWhile(String... literals) {
        if (check(Token.Type.IDENTIFIER, "WHILE")) {
            return true;
        }
        return false;
    }

    private boolean matchassignment(String... literals) {
        if (check(Token.Type.IDENTIFIER, "LET")) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchfunctionDef(String... literals) {
        if (check(Token.Type.IDENTIFIER, "DEF")) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(Token.Type type, String literal) {
        return !isAtEnd() && peek().getType() == type && peek().getLiteral().equals(literal);
    }

    private boolean check(Token.Type type) {
        return !isAtEnd() && peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size();
    }

    private Token peek() {
        if (isAtEnd()) {
            return null;
        }
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseException error(Token token, String message) {
        return new ParseException(message, token.getIndex());
    }
}
