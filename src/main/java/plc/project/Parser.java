package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    //from here we start
    public Ast.Stmt parseStatement() throws ParseException
    {
        //if we are starting with an identifier there are a few possiblites where we can go
        if (matchType(Token.Type.IDENTIFIER))
        {
            Token identifier = previous();
            // is it an assignment statement
            if (matchOperator("="))
            {
                // on the other side we can have an expression
                Ast.Expr value = parseExpression();
                // after that do we have a semicolon
                consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");
                // makes the tree and returns it
                return new Ast.Stmt.Assignment(
                        new Ast.Expr.Access(Optional.empty(), identifier.getLiteral()),
                        value
                );
            }
            else if (matchOperator("("))
            {
                // if it is a function call then check for ; and then it is calling the function to parse arguments
                List<Ast.Expr> arguments = parseArguments();
                consume(Token.Type.OPERATOR, ";", "Expected ';' after function call.");
                //makes the tree and returns it
                return new Ast.Stmt.Expression(
                        new Ast.Expr.Function(Optional.empty(), identifier.getLiteral(), arguments)
                );
            } else {
                // this is handling simple identifiers
                consume(Token.Type.OPERATOR, ";", "Expected ';' after expression.");
                return new Ast.Stmt.Expression(
                        new Ast.Expr.Access(Optional.empty(), identifier.getLiteral())
                );
            }
        }
        throw error(peek(), "Unexpected statement.");
    }


    // Entry point for parsing expressions
    public Ast.Expr parseExpression() throws ParseException {
        return  parseLogicalExpression();

    }
    public Ast.Expr parseLogicalExpression() throws ParseException {
        //OR has lower precedence than AND, so we start with OR expression first

        return parseOrExpression();
    }


    private Ast.Expr parseOrExpression() throws ParseException {
        Ast.Expr expr = parseAndExpression();  // OR has lower precedence than AND

        // Match OR logical operators
        while (matchLogicalOperator("OR")) {
            // we will start from OR
            String operator = previous().getLiteral();
            Ast.Expr right = parseAndExpression();  // Recur to handle right-hand side AND expressions
            expr = new Ast.Expr.Binary(operator, expr, right);  // Build binary expression
        }

        return expr;
    }

    private Ast.Expr parseAndExpression() throws ParseException {
        Ast.Expr expr = parseComparisonExpression();
        while (matchLogicalOperator("AND")) {
            // then we will go to AND
            String operator = previous().getLiteral();
            Ast.Expr right = parseComparisonExpression();  // Recur to handle right-hand side comparison
            expr = new Ast.Expr.Binary(operator, expr, right);  // Build binary expression
        }

        return expr;
    }

    // Match based on operator literal (specific tokens like AND, OR)
// AND and OR are passed as identifiers but treated as operators
    private boolean matchLogicalOperator(String... literals) {
        for (String literal : literals)
        {
            // logical operators are labeled as identifiers so use them as that
            if ((check(Token.Type.OPERATOR, literal) || check(Token.Type.IDENTIFIER, literal))) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Comparison expressions (<, >, <=, >=, ==, !=)
    private Ast.Expr parseComparisonExpression() throws ParseException
    {
        // going to next precedence
        Ast.Expr expr = parseAdditiveExpression();

        while (matchOperator("<", ">", "<=", ">=", "==", "!=")) {
            String operator = previous().getLiteral();

            Ast.Expr right = parseAdditiveExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    // Additive expressions (+, -)
    private Ast.Expr parseAdditiveExpression() throws ParseException {
        //going to next precedence
        Ast.Expr expr = parseMultiplicativeExpression();
        //also working according precedence
        while (matchOperator("+", "-")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    // Multiplicative expressions (*, /)
    private Ast.Expr parseMultiplicativeExpression() throws ParseException
    {
        //because of precedence
        Ast.Expr expr = parseSecondaryExpression();
        //keep parsing
        while (matchOperator("*", "/")) {
            String operator = previous().getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    // Secondary expressions (for function calls, field accesses)
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
    // Primary expressions (numbers, booleans, parentheses, variables)
    private Ast.Expr parsePrimaryExpression() throws ParseException {
        if (matchType(Token.Type.IDENTIFIER)) {
            String identifier = previous().getLiteral();
            // Handle NIL as null
            if (identifier.equals("NIL")) {
                return new Ast.Expr.Literal(null);
            }
            // Treat "AND" and "OR" as operators, even if passed as identifiers

            if (identifier.equals("AND") || identifier.equals("OR")) {
                // Reinterpret the identifier as a logical operator and parse as logical expression
                return parseLogicalExpression();
            }

            if (identifier.equals("TRUE") || identifier.equals("FALSE")) {
                // Handle TRUE and FALSE as boolean literals
                return new Ast.Expr.Literal(Boolean.parseBoolean(identifier));
            }
            if (matchOperator("(")) {
                return new Ast.Expr.Function(Optional.empty(), identifier, parseArguments());
            }
            return new Ast.Expr.Access(Optional.empty(), identifier);
        }
        if (matchType(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(previous().getLiteral()));
        }
        if (matchType(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(previous().getLiteral()));
        }
        if (matchType(Token.Type.CHARACTER)) {
            return new Ast.Expr.Literal(previous().getLiteral().charAt(1)); // Remove surrounding quotes
        }
        if (matchType(Token.Type.STRING)) {
            // Remove surrounding quotes and handle escape sequences
            String literal = previous().getLiteral();
            literal = literal.substring(1, literal.length() - 1).replace("\\n", "\n");
            return new Ast.Expr.Literal(literal);
        }
        if (matchOperator("(")) {
            Ast.Expr expr = parseExpression();
            //checking for close bracket
            consume(Token.Type.OPERATOR, ")", "Expected ')' after expression.");
            return new Ast.Expr.Group(expr);
        }
        throw error(peek(), "Expected expression.");
    }
//METHODS THAT WILL HELP THE PARSING WORK
    private List<Ast.Expr> parseArguments() throws ParseException {
        List<Ast.Expr> arguments = new java.util.ArrayList<>();
        if (!check(Token.Type.OPERATOR, ")")) {
            do {
                arguments.add(parseExpression());
            } while (matchOperator(","));
        }
        consume(Token.Type.OPERATOR, ")", "Expected ')' after arguments.");
        return arguments;
    }

    // Helper method for consuming tokens
    private Token consume(Token.Type type, String literal, String message) throws ParseException {
        if (check(type, literal)) return advance();
        throw error(peek(), message);
    }

    private Token consume(Token.Type type, String message) throws ParseException {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Match based on token type
    private boolean matchType(Token.Type... types) {
        for (Token.Type type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Match based on operator literal (specific tokens like +, -, *, /, etc.)
    private boolean matchOperator(String... literals) {
        for (String literal : literals) {
            if (check(Token.Type.OPERATOR, literal)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Check if the current token matches the given type and literal
    private boolean check(Token.Type type, String literal) {
        return !isAtEnd() && peek().getType() == type && peek().getLiteral().equals(literal);
    }

    // Check if the current token matches the given type
    private boolean check(Token.Type type) {
        return !isAtEnd() && peek().getType() == type;
    }

    // Helper methods for traversing the token stream
    private Token advance() {
        if (!isAtEnd()) current++;  // Only advance if not at the end
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size();  // Just check if we've reached the end of the token list
    }

    // Modify the peek method
    private Token peek() {
        if (isAtEnd()) {
            return null;  // Return null if we're at the end of the token list
        }
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    // Error handling
    private ParseException error(Token token, String message) {
        return new ParseException(message, token.getIndex());
    }
}
