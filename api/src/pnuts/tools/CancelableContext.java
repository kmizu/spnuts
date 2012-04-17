/*
 * CancelableContext.java
 *
 * Created on 2006/03/08, 0:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package pnuts.tools;

import java.util.Properties;
import pnuts.compiler.CompilerPnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Implementation;
import pnuts.lang.Jump;
import pnuts.lang.PnutsImpl;

/**
 *
 * @author tomatsu
 */
class CancelableContext extends Context {
    private boolean canceled;
    
    public void cancel(){
        this.canceled = true;
    }
    
    CancelableContext(){
        Implementation impl = getImplementation();
        if (impl instanceof CompilerPnutsImpl){
            ((PnutsImpl)impl).setProperty("pnuts.compiler.traceMode", "true");
        }
    }
    CancelableContext(Context context){
        super(context);
    }
    
    CancelableContext(Properties properties){
        super(properties);
        Implementation impl = getImplementation();
        if (impl instanceof CompilerPnutsImpl){
            ((PnutsImpl)impl).setProperty("pnuts.compiler.traceMode", "true");
        }
    }
    protected void updateLine(int line){
        if (canceled){
            throw new Jump(null);
        }
        super.updateLine(line);
    }
}