package at.searles.fractal;

import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.meelan.parser.MeelanParser;
import at.searles.parsing.parser.ParserStream;

public class ParserInstance {

    private static ParserInstance singleton = null;

    public static ParserInstance get() {
        if(singleton == null) {
            singleton = new ParserInstance();
        }

        return singleton;
    }

    private final MeelanEnv env;
    private final MeelanParser parser;

    private ParserInstance() {
        this.env = new MeelanEnv();
        this.parser = new MeelanParser();
    }

    public Tree parseExpr(String sourceCode) {
        ParserStream stream = ParserStream.fromString(sourceCode);

        Tree tree = parser.parseExpr(env, stream);

        if(!stream.isEmpty()) {
            // FIXME some kind of warning that it was not fully parsed?
        }

        return tree;
    }

    public Ast parseSource(String sourceCode) {
        ParserStream stream = ParserStream.fromString(sourceCode);

        return Ast.parse(env, stream);

//        if(!stream.isEmpty()) {
//            // TODO 2018-07-11: There should be some warning in this case
//            // TODO but no exception because of backwards compatibility.
//            // throw new MeelanException("not fully parsed!", null);
//        }
    }
}
