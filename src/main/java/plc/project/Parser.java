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
//        System.out.println("if is being recognised");
advance();
        Ast.Expr condition = parseExpression();
        System.out.println("this is the condition");

        System.out.println(condition);
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after IF condition.");
        List<Ast.Stmt> thenBranch = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "ELSE") && !check(Token.Type.IDENTIFIER, "END")) {
            System.out.println("WE ARE INSIDE ELSE");

            thenBranch.add(parseStatement());
            System.out.println(thenBranch);

        }
        List<Ast.Stmt> elseBranch = new ArrayList<>();
        if (check(Token.Type.IDENTIFIER, "ELSE")) {
            advance();
            while (!check(Token.Type.IDENTIFIER, "END")) {
                elseBranch.add(parseStatement());
            }
        }
        System.out.println("now we are going back");
//        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' after IF statement.");
        System.out.println("back again");
        return new Ast.Stmt.If(condition, thenBranch, elseBranch);
    }
    private Ast.Stmt parseReturnStatement() throws ParseException {
//        System.out.println("if is being recognised");
        advance();
        Ast.Expr condition = parseExpression();
        System.out.println("this is the condition");

        System.out.println(condition);
        consume(Token.Type.OPERATOR, ";", "Expected ;");

        return new Ast.Stmt.Return(condition);
    }
    private Ast.Stmt parseFORStatement() throws ParseException {
        // Advance to consume the 'FOR' keyword
        advance();
       String loopVariable=peek().getLiteral();
        advance();
        // Expect 'IN' keyword
        consume(Token.Type.IDENTIFIER, "IN", "Expected 'IN' after loop variable.");

        // Parse the iterable expression (e.g., list)
        Ast.Expr iterable = parseExpression();

        // Expect 'DO' keyword
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after iterable expression.");

        // Parse the statements inside the loop body
        List<Ast.Stmt> thenBranch = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) {
            thenBranch.add(parseStatement());
        }

        // Expect 'END' keyword to close the loop
        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the 'FOR' loop.");

        // Return the constructed For statement
        return new Ast.Stmt.For(loopVariable, iterable, thenBranch);
    }
    private Ast.Stmt parseLetStatement() throws ParseException {
        // Advance to consume the 'LET' keyword
        System.out.println("raeched here");

        advance();
//String variable;
        // Ensure we have an identifier after 'LET'
        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            String variable = peek().getLiteral();
            System.out.println("this is the variable"+variable);
            advance(); // Advance after capturing the identifier

            // Attempt to parse either a declaration or an assignment
            try {
                // Expect either '=' for an assignment or ';' for a declaration
                if (matchOperator( "=")) {
                    // Parse assignment expression
                    Ast.Expr value = parseExpression();

                    // Expect a semicolon at the end of the statement
                    consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");

                    // Return an assignment statement
                    return new Ast.Stmt.Declaration(variable,Optional.of(value) );
                } else if (matchOperator( ";")) {
                    // If we encounter a semicolon, it's just a declaration
                    return new Ast.Stmt.Declaration(variable, Optional.empty());
                } else {
                    // If neither '=' nor ';' is found, throw an error

                }
            } catch (ParseException e) {
                // Handle the parse error
                System.err.println("Error: " + e.getMessage());
                throw e; // Rethrow the exception after handling
            }
        } else {
        }
        return new Ast.Stmt.Declaration(null, Optional.empty());

    }
//
//

    private Ast.Field parseFieldStatement() throws ParseException {
        // Advance to consume the 'LET' keyword
        System.out.println("raeched here");

        advance();
//String variable;
        // Ensure we have an identifier after 'LET'
        if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            String variable = peek().getLiteral();

            System.out.println("this is the variable"+variable);
            advance(); // Advance after capturing the identifier

            // Attempt to parse either a declaration or an assignment
            try {
                // Expect either '=' for an assignment or ';' for a declaration
                if (matchOperator( "=")) {
                    // Parse assignment expression
                    Ast.Expr value = parseExpression();

                    // Expect a semicolon at the end of the statement
                    consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");

                    // Return an assignment statement
                    return new Ast.Field(variable,Optional.of(value) );
                } else if (matchOperator( ";")) {
                    // If we encounter a semicolon, it's just a declaration
                    return new Ast.Field(variable, Optional.empty());
                } else {
                    // If neither '=' nor ';' is found, throw an error

                }
            } catch (ParseException e) {
                // Handle the parse error
                System.err.println("Error: " + e.getMessage());
                throw e; // Rethrow the exception after handling
            }
        } else {
        }
        return new Ast.Field(null, Optional.empty());

    }




    private Ast.Stmt parseWHILEStatement() throws ParseException {
        // Advance to consume the 'FOR' keyword
        advance();
        Ast.Expr condition = parseExpression();
        System.out.println("this is the condition");

        System.out.println(condition);
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' after IF condition.");
        List<Ast.Stmt> thenBranch = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) {
            System.out.println("WE ARE INSIDE ELSE");

            thenBranch.add(parseStatement());
            System.out.println(thenBranch);

        }

        System.out.println("now we are going back");
//      consume(Token.Type.IDENTIFIER, "END", "Expected 'END' after IF statement.");
        System.out.println("back again");
        return new Ast.Stmt.While(condition, thenBranch);
    }
    public Ast.Source parseSource() throws ParseException {
        // Declare and initialize the ArrayList to hold parsed statements
        List<Ast.Field> statements = new ArrayList<>(); // Create a list to hold the parsed statements
//        List<Ast.Field> source = new ArrayList<>(); // Create a list to hold the parsed statements
        List<Ast.Method> methods = new ArrayList<>();

        while (!isAtEnd()) { // Keep parsing until the end of the input is reached
            // Start parsing based on the current token
            if (matchLet("LET")) {
                System.out.println("Reached LET");

                advance(); // Advance past 'LET'

                // Ensure we have an identifier after 'LET'
                if (check(Token.Type.IDENTIFIER, peek().getLiteral())) {
                    String variable = peek().getLiteral();

                    System.out.println("This is the variable: " + variable);
                    advance(); // Advance after capturing the identifier

                    // Attempt to parse either a declaration or an assignment
                    try {
                        if (matchOperator("=")) {
                            // Parse assignment expression
                            Ast.Expr value = parseExpression();

                            // Expect a semicolon at the end of the statement
                            consume(Token.Type.OPERATOR, ";", "Expected ';' after assignment.");

                            // Add the assignment statement to the list
                            statements.add(new Ast.Field(variable, Optional.of(value)));
                        } else if (matchOperator(";")) {
                            // If we encounter a semicolon, it's just a declaration
                            statements.add(new Ast.Field(variable, Optional.empty()));
                        } else {
                            throw error(peek(), "Expected ';' or '=' after identifier.");
                        }
                    } catch (ParseException e) {
                        // Handle the parse error
                        System.err.println("Error: " + e.getMessage());
                        throw e; // Rethrow the exception after handling
                    }
                } else {
                    throw error(peek(), "Expected identifier after 'LET'.");
                }
            }
            else if (matchDEF("DEF")){
                methods.add(parseDefStatement());
            }
            else {

                System.out.println(peek().getLiteral());
                throw error(peek(), "Unexpected statement.");
            }

        }

        return  new Ast.Source(statements, methods); // Return the list of parsed statements
    }
    private Ast.Method parseDefStatement() throws ParseException {
        advance(); // Consume 'DEF'

        // Ensure the identifier for the method name
        if (!check(Token.Type.IDENTIFIER, peek().getLiteral())) {
            throw error(peek(), "Expected method name after 'DEF'.");
        }

        String methodName = peek().getLiteral();
        advance(); // Consume the method name

        // Expect the opening parenthesis for parameters
        consume(Token.Type.OPERATOR, "(", "Expected '(' after method name.");

        // Parse the parameters
        List<String> parameters = new ArrayList<>();
        if (!check(Token.Type.OPERATOR, ")")) { // Check if there are parameters
            do {
                if (!check(Token.Type.IDENTIFIER, peek().getLiteral())) {
                    throw error(peek(), "Expected parameter name.");
                }

                parameters.add(peek().getLiteral()); // Add parameter to the list
                advance(); // Consume the parameter

            } while (matchOperator(",")); // Continue parsing parameters if comma is present
        }

        // Expect closing parenthesis after parameters
        consume(Token.Type.OPERATOR, ")", "Expected ')' after parameters.");

        // Expect 'DO' keyword to indicate the start of the method body
        consume(Token.Type.IDENTIFIER, "DO", "Expected 'DO' to start the method body.");

        // Parse the statements in the method body
        List<Ast.Stmt> bodyStatements = new ArrayList<>();
        while (!check(Token.Type.IDENTIFIER, "END")) { // Continue until 'END' is encountered
            bodyStatements.add(parseStatement()); // Parse each statement and add it to the body
        }

        // Expect 'END' to close the method body
        consume(Token.Type.IDENTIFIER, "END", "Expected 'END' to close the method.");
        System.out.println("dome with method");
advance();
        // Return the parsed method as an Ast.Method object
        return new Ast.Method(methodName, parameters, bodyStatements);
    }
    //from here we start
    public Ast.Stmt parseStatement() throws ParseException
    {
        //if we are starting with an identifier there are a few possiblites where we can go
//        System.out.println("Your message here");
        if (matchLet("LET")) {
//            System.out.println("Recognises IF");

            // now we know it is an IF statement
            Ast.Stmt stmt = parseLetStatement();
            System.out.println(stmt);
            return stmt;
        }
        if (matchReturn("RETURN")) {
//            System.out.println("Recognises IF");

            // now we know it is an IF statement
            Ast.Stmt stmt = parseReturnStatement();
            System.out.println(stmt);
            return stmt;
        }
        if (matchcondition("IF")) {
//            System.out.println("Recognises IF");

            // now we know it is an IF statement
            Ast.Stmt stmt = parseIfStatement();
            System.out.println(stmt);
            return stmt;
        }
        if (matchFor("FOR")) {
//            System.out.println("Recognises IF");

            // now we know it is an IF statement
            Ast.Stmt stmt = parseFORStatement();
            System.out.println(stmt);
            return stmt;
        }

        if (matchWhile("WHILE")) {
//            System.out.println("Recognises IF");

            // now we know it is an IF statement
            Ast.Stmt stmt = parseWHILEStatement();
            System.out.println(stmt);
            return stmt;
        }
        if (matchType(Token.Type.IDENTIFIER))
        {
            //NOW we know it is an identifier aagay aagyaa hai
            Token identifier = previous();
            //peechay aao aur dekho kyaa scene hai
            // check karo agar FOR hai Yaa While hai

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
        System.out.println("we are in parse statement");
        System.out.println(peek().getLiteral());

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
            System.out.println("we are returning expression identifier wala IF wla ahi");
            System.out.println(identifier);

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
    private boolean matchloop() {
        if (check(Token.Type.IDENTIFIER, "FOR") || check(Token.Type.IDENTIFIER, "WHILE")) {
            advance();  // Move to the next token after matching
            return true;
        }
        return false;
    }

    private boolean matchcondition(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "IF") ) {
              // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchReturn(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "RETURN") ) {
            // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchLet(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "LET") ) {
            // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchFor(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "FOR") ) {
            // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchDEF(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "DEF") ) {
            // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchWhile(String... literals) {
//        System.out.println(check(Token.Type.IDENTIFIER, "IF") );

        if (check(Token.Type.IDENTIFIER, "WHILE") ) {
            // Move to the next token after matching
            return true;
        }
        return false;
    }
   private boolean matchassignment(String... literals) {
        if (check(Token.Type.IDENTIFIER, "LET") ) {
            advance();  // Move to the next token after matching
            return true;
        }
        return false;
    }
    private boolean matchfunctionDef(String... literals) {
        if (check(Token.Type.IDENTIFIER, "DEF") ) {
            advance();  // Move to the next token after matching
            return true;
        }
        return false;
    }
    // Check if the current token matches the given type and literal
    private boolean check(Token.Type type, String literal) {
//        System.out.println(peek().getLiteral());
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
