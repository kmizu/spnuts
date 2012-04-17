package org.pnuts.lib;

import pnuts.lang.Context;
import java.io.*;
import java.util.*;

class MerlinCounter extends Counter {
	public Number size(Object a, Context context){
		if (a instanceof CharSequence){
			return new Integer(((CharSequence)a).length());
		} else {
			return super.size(a, context);
		}
	}
}
