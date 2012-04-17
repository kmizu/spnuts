package org.pnuts.lang;
import pnuts.lang.*;

public class CompositeGenerator extends Generator {
    private Generator g1, g2;
    
    public CompositeGenerator(Generator g1, Generator g2){
	this.g1 = g1;
	this.g2 = g2;
    }

    public Object apply(PnutsFunction closure, Context context) {
	g1.apply(closure, context);
	return g2.apply(closure, context);
    }
}

