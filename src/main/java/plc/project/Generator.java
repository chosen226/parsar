
package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0; // Tracks the current indentation level

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indentLevel) {
        writer.println();
        for (int i = 0; i < indentLevel; i++) {
            writer.write("    "); // 4 spaces per indentation level
        }
    }

//    @Override
//    public Void visit(Ast.Source ast) {
//        // Start the class definition
//        print("public class Main {");
//        newline(0);
//
//        // Generate the entry point `main` method
//        newline(++indent);
//        print("public static void main(String[] args) {");
//        newline(++indent);
//        print("System.exit(new Main().main());");
//        newline(--indent);
//        print("}");
//        newline(--indent);
//
//        // Generate each user-defined method
//        for (Ast.Method method : ast.getMethods()) {
//            newline(++indent); // Add a newline before each method for separation
//            visit(method);
//            newline(--indent); // Reset to the previous indentation level
//        }
//
//        // End the class
//        newline(0);
//        print("}");
//        return null;
//    }

    @Override
    public Void visit(Ast.Source ast) {
        // Start the class definition
        print("public class Main {");
        newline(0);

        // Generate each field
        for (Ast.Field field : ast.getFields()) {
            newline(++indent); // Indent for field declarations
            visit(field);
            --indent; // Reset the indentation level after printing the field
        }

        // Generate the entry point `main` method
        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent);

        // Generate each user-defined method
        for (Ast.Method method : ast.getMethods()) {
            newline(++indent); // Add a newline before each method for separation
            visit(method);
            newline(--indent); // Reset to the previous indentation level
        }

        // End the class
        newline(0);
        print("}");
        return null;
    }



    @Override
    public Void visit(Ast.Method ast) {
        // Generate the method signature
        String returnType = ast.getReturnTypeName().map(this::getJavaType).orElse("void");
        print(returnType, " ", ast.getName(), "() {");
        newline(++indent); // Indent for the method body

        // Generate each statement in the method body
        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i));
            if (i < ast.getStatements().size() - 1) {
                newline(indent); // Add a newline between statements
            }
        }

        // Dedent and close the method
        newline(--indent);
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        String javaType = ast.getTypeName().isPresent()
                ? getJavaType(ast.getTypeName().get())
                : getJavaType(ast.getVariable().getType().getName());

        print(javaType, " ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    private String getJavaType(String typeName) {
        switch (typeName) {
            case "Integer": return "int";
            case "Decimal": return "double";
            case "Boolean": return "boolean";
            case "String": return "String";
            default: throw new IllegalArgumentException("Unknown type: " + typeName);
        }
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }
    @Override
    public Void visit(Ast.Stmt.For ast) {
        // Default type as "Object" if type information is missing
        String type = "Object";

        // Attempt to infer the type of the iterable
        if (ast.getValue() instanceof Ast.Expr.Access) {
            Ast.Expr.Access access = (Ast.Expr.Access) ast.getValue();
            if (access.getVariable() != null) {
                type = getJavaType(access.getVariable().getType().getName());
            }
        }

        // Print the `for` loop header
        print("for (", type, " ", ast.getName(), " : ");
        visit(ast.getValue()); // Visit the iterable expression
        print(") {");

        // Handle the body of the `for` loop
        if (!ast.getStatements().isEmpty()) {
            newline(++indent); // Add indentation for the loop body
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                if (i < ast.getStatements().size() - 1) {
                    newline(indent); // Add newline between statements
                }
            }
            newline(--indent); // Dedent after processing statements
        }else{
            newline(indent);
        }

        // Ensure the closing brace is on a new line

        print("}");
        return null;
    }




    @Override
    public Void visit(Ast.Stmt.If ast) {
        // Generate the `if` keyword and condition
        print("if (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);

        // Generate the statements in the `then` branch
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            visit(ast.getThenStatements().get(i));
            if (i < ast.getThenStatements().size() - 1) {
                newline(indent); // Add a newline between statements
            }
        }

        // Dedent and close the `then` block
        newline(--indent);
        print("}");

        // Generate the `else` branch if it exists
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                visit(ast.getElseStatements().get(i));
                if (i < ast.getElseStatements().size() - 1) {
                    newline(indent); // Add a newline between statements
                }
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        // Extract and process the type name
        String javaType = getJavaTypes(ast.getTypeName());
        print(javaType, " ", ast.getName());

        // Handle initialization, if present
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    private String getJavaTypes(String typeName) {
        switch (typeName) {
            case "Optional[Integer]":
            case "Integer": return "int";
            case "Optional[Decimal]":
            case "Decimal": return "double";
            case "Optional[Boolean]":
            case "Boolean": return "boolean";
            case "Optional[String]":
            case "String": return "String";
            default: throw new IllegalArgumentException("Unknown type: " + typeName);
        }
    }


    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        newline(indent);
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal instanceof String) {
            print("\"", literal.toString(), "\"");
        } else {
            print(literal.toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft()); // Visit the left operand

        // Handle string concatenation separately
        if (ast.getOperator().equals("+") &&
                (ast.getLeft().getType() == Environment.Type.STRING ||
                        ast.getRight().getType() == Environment.Type.STRING)) {
            print(" + "); // Use + for string concatenation
        } else {
            // Map other operators to their Java equivalents
            String operator;
            switch (ast.getOperator()) {
                case "AND":
                    operator = "&&";
                    break;
                case "OR":
                    operator = "||";
                    break;
                default:
                    operator = ast.getOperator();
                    break;
            }
            print(" ", operator, " "); // Print operator with spaces
        }

        visit(ast.getRight()); // Visit the right operand
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getFunction().getJvmName(), "(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }
}
