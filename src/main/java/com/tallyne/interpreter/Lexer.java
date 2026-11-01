package com.tallyne.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tallyne Line-Aware Lexer.
 * 
 * Responsibilities:
 * - Tokenize Tallyne source respecting logical line boundaries
 * - Preserve string literal contents exactly (including spaces)
 * - Track line numbers for error reporting
 * - Distinguish significant newlines from insignificant whitespace
 * - Handle comments (// to end of line)
 * - Case-insensitive routine/entity name handling at parser level
 */
public class Lexer {

    // Token types
    public static final String TOK_RoutineDecl = "ROUTINE_DECL";
    public static final String TOK_ParameterDecl = "PARAM_DECL";
    public static final String TOK_ActionTask = "ACTION_TASK";
    public static final String TOK_ArithmeticTask = "ARITHMETIC_TASK";
    public static final String TOK_CheckTask = "CHECK_TASK";
    public static final String TOK_StringLiteral = "STRING_LITERAL";
    public static final String TOK_Number = "NUMBER";
    public static final String TOK_Ident = "IDENT";
    public static final String TOK_LeftParen = "LEFT_PAREN";
    public static final String TOK_RightParen = "RIGHT_PAREN";
    public static final String TOK_LeftBrace = "LEFT_BRACE";
    public static final String TOK_RightBrace = "RIGHT_BRACE";
    public static final String TOK_LeftBracket = "LEFT_BRACKET";
    public static final String TOK_RightBracket = "RIGHT_BRACKET";
    public static final String TOK_Comma = "COMMA";
    public static final String TOK_EOL = "EOL"; // End of logical line
    public static final String TOK_EOF = "EOF";
    public static final String TOK_COMMENT = "COMMENT";
    public static final String TOK_HashStar = "HASH_STAR"; // # ***

    // Pattern for string literals: content between double quotes, preserving spaces
    private static final Pattern STRING_LITERAL_PATTERN = 
        Pattern.compile("\"((?:[^\"]|\\\")*)\"");

    // Pattern for numbers (integers and decimals)
    private static final Pattern NUMBER_PATTERN = 
        Pattern.compile("-?\\d+(\\.\\d+)?");

    // Pattern for identifiers (routine names, variable names)
    private static final Pattern IDENT_PATTERN = 
        Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    // Pattern for operators and symbols
    private static final String SYMBOL_PATTERNS = 
        "#|\\$|\\?|\\{|\\}|\\}|\\(|\\)|\\[|\\]|,|//|#\\*\\*";

    private String source;
    private int lineNumber;
    private int position;
    private int sourceLength;
    private String currentLine;
    private int currentLineStart;

    public Lexer(String source) {
        this.source = source;
        this.lineNumber = 1;
        this.position = 0;
        this.sourceLength = source.length();
        this.currentLineStart = 0;
    }

    /**
     * Tokenize the entire source into a list of tokens.
     * Each token carries line number information.
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        
        while (position < sourceLength) {
            // Skip whitespace outside string literals
            skipWhitespace();
            
            if (position >= sourceLength) {
                break;
            }
            
            int lineStart = lineNumber;
            int tokenStartPos = position;
            
            // Check for string literal first (must be checked before other patterns)
            Matcher stringMatcher = STRING_LITERAL_PATTERN.matcher(source.substring(position));
            if (stringMatcher.find()) {
                String literalValue = stringMatcher.group(1);
                int literalEnd = position + stringMatcher.end();
                tokens.add(new Token(TOK_StringLiteral, literalValue, lineNumber, tokenStartPos));
                position = literalEnd;
                continue;
            }
            
            // Check for EOL (end of line) - newline or end of source
            if (source.charAt(position) == '\n' || source.charAt(position) == '\r') {
                // Handle \r\n Windows line ending
                if (source.charAt(position) == '\r' && position + 1 < sourceLength && source.charAt(position + 1) == '\n') {
                    position += 2;
                } else {
                    position++;
                }
                lineNumber++;
                tokens.add(new Token(TOK_EOL, null, lineStart, tokenStartPos));
                continue;
            }
            
            // Check for single-character symbols
            char ch = source.charAt(position);
            String singleSymbol = null;
            switch (ch) {
                case '{':
                    position++;
                    tokens.add(new Token(TOK_LeftBrace, "{", lineNumber, tokenStartPos));
                    continue;
                case '}':
                    position++;
                    tokens.add(new Token(TOK_RightBrace, "}", lineNumber, tokenStartPos));
                    continue;
                case '(':
                    position++;
                    tokens.add(new Token(TOK_LeftParen, "(", lineNumber, tokenStartPos));
                    continue;
                case ')':
                    position++;
                    tokens.add(new Token(TOK_RightParen, ")", lineNumber, tokenStartPos));
                    continue;
                case '[':
                    position++;
                    tokens.add(new Token(TOK_LeftBracket, "[", lineNumber, tokenStartPos));
                    continue;
                case ']':
                    position++;
                    tokens.add(new Token(TOK_RightBracket, "]", lineNumber, tokenStartPos));
                    continue;
                case ',':
                    position++;
                    tokens.add(new Token(TOK_Comma, ",", lineNumber, tokenStartPos));
                    continue;
            }
            
            // Check for // comment
            if (ch == '/' && position + 1 < sourceLength && source.charAt(position + 1) == '/') {
                // Consume until end of line
                int commentStart = position;
                position += 2;
                while (position < sourceLength && source.charAt(position) != '\n' && source.charAt(position) != '\r') {
                    position++;
                }
                // If file ends without trailing newline, still consume rest
                if (position >= sourceLength) {
                    // already at end
                }
                // Make sure line number is updated if we crossed a line
                if (position < sourceLength && (source.charAt(position) == '\n' || source.charAt(position) == '\r')) {
                    if (source.charAt(position) == '\r' && position + 1 < sourceLength && source.charAt(position + 1) == '\n') {
                        position += 2;
                    } else {
                        position++;
                    }
                    lineNumber++;
                }
                tokens.add(new Token(TOK_COMMENT, source.substring(commentStart, position), lineStart, tokenStartPos));
                continue;
            }
            
            // Check for # *** (hash star / routine terminator)
            if (ch == '#') {
                if (position + 2 < sourceLength && source.charAt(position + 1) == '*' && source.charAt(position + 2) == '*') {
                    position += 3;
                    tokens.add(new Token(TOK_HashStar, "# ***", lineNumber, tokenStartPos));
                    continue;
                }
                // Single # - this starts an action task
                position++;
                tokens.add(new Token(TOK_ActionTask, "#", lineNumber, tokenStartPos));
                continue;
            }
            
            // Check for $ (arithmetic task)
            if (ch == '$') {
                position++;
                tokens.add(new Token(TOK_ArithmeticTask, "$", lineNumber, tokenStartPos));
                continue;
            }
            
            // Check for ? (check task)
            if (ch == '?') {
                position++;
                tokens.add(new Token(TOK_CheckTask, "?", lineNumber, tokenStartPos));
                continue;
            }
            
            // Check for numbers
            if (Character.isDigit(ch) || (ch == '-' && position + 1 < sourceLength && Character.isDigit(source.charAt(position + 1)))) {
                String numStr;
                int numStart = position;
                
                // Handle negative numbers
                if (ch == '-') {
                    position++;
                    if (position < sourceLength && Character.isDigit(source.charAt(position))) {
                        // Check if this is a negative number or subtraction
                        // Look ahead - if next char is digit, it's a negative number
                        matcher = NUMBER_PATTERN.matcher(source.substring(position - 1));
                        if (matcher.find()) {
                            numStr = matcher.group();
                            position = numStart + matcher.end();
                            tokens.add(new Token(TOK_Number, numStr, lineNumber, tokenStartPos));
                            continue;
                        }
                    }
                    // If not a negative number, treat as separate symbols
                    // Fall through to handle the minus sign
                }
                
                matcher = NUMBER_PATTERN.matcher(source.substring(position));
                if (matcher.find()) {
                    numStr = matcher.group();
                    position = numStart + matcher.end();
                    tokens.add(new Token(TOK_Number, numStr, lineNumber, tokenStartPos));
                    continue;
                }
            }
            
            // Check for identifiers and routine names
            if (Character.isLetter(ch) || ch == '_') {
                matcher = IDENT_PATTERN.matcher(source.substring(position));
                if (matcher.find()) {
                    String ident = matcher.group();
                    position = numStart + matcher.end();
                    // Check if it's a reserved word / special identifier
                    if (ident.equalsIgnoreCase("TASKS") || ident.equalsIgnoreCase("TRUE") || ident.equalsIgnoreCase("FALSE")) {
                        // Could be a routine name; treat as ident for now
                        tokens.add(new Token(TOK_Ident, ident, lineNumber, tokenStartPos));
                        continue;
                    }
                    tokens.add(new Token(TOK_Ident, ident, lineNumber, tokenStartPos));
                    continue;
                }
            }
            
            // If we get here, it's an unknown character - report as error
            // For now, skip it and advance
            position++;
        }
        
        // Add EOF token
        tokens.add(new Token(TOK_EOF, null, lineNumber, position));
        
        return tokens;
    }

    private void skipWhitespace() {
        while (position < sourceLength) {
            char ch = source.charAt(position);
            if (ch == ' ' || ch == '\t') {
                position++;
            } else if (ch == '\n' || ch == '\r') {
                // Line break - stop skipping, let the main loop handle it
                break;
            } else {
                break;
            }
        }
    }

    /**
     * Token with line number and position information.
     */
    public static class Token {
        private final String type;
        private final String value;
        private final int lineNumber;
        private final int position;

        public Token(String type, String value, int lineNumber, int position) {
            this.type = type;
            this.value = value;
            this.lineNumber = lineNumber;
            this.position = position;
        }

        public String getType() { return type; }
        public String getValue() { return value; }
        public int getLineNumber() { return lineNumber; }
        public int getPosition() { return position; }

        @Override
        public String toString() {
            return String.format("Token[type=%s, value=%s, line=%d, pos=%d]", type, value, lineNumber, position);
        }
    }
}