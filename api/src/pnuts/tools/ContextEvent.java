package pnuts.tools;

import java.util.EventObject;
import pnuts.lang.Context;

public class ContextEvent extends EventObject {

    public ContextEvent(Object source){
	super(source);
    }

    public Context getContext(){
	return (Context)getSource();
    }
}
