/*
 * @(#)PatternMatchHelper.java 1.3 05/05/25
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.regex.jsr51;

import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import java.util.*;
import java.util.regex.*;
import java.io.*;

class MatchContext implements Serializable {
	Matcher matcher;
	int count;
}

/**
 * This is a helper class for pnuts.regex module, which utilizes
 * java.util.regex package in J2SDK1.4.0.
 */
class PatternMatchHelper implements Serializable {

	String lastExpr;
	String lastOption;
	Pattern pattern;
	final static Object[] NO_PARAM = new Object[0];
	final static String CONTEXT_SYMBOL = "pnuts.regex.jsr51".intern();

	static String escapePattern(String pattern){
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < pattern.length(); i++){
			char c = pattern.charAt(i);
			switch (c){
			case '(':  case ')':
			case '{':  case '}':
			case '[':  case ']':
			case '^':  case '$':
			case '.':  case '?':  case '*': case '+':
			case '\\': case '|':
				sbuf.append('\\');
			}
			sbuf.append(c);
		}
		return sbuf.toString();
	}

	synchronized Pattern getPattern(String expr, String option, Context context){
		Pattern p;
		if ((expr == lastExpr || expr.equals(lastExpr)) &&
			(option == lastOption || option != null && option.equals(lastOption)))
		{
			p = pattern;
		} else {
			int flag = 0;
			if (option != null){
				for (int i = 0; i < option.length(); i++){
					switch (option.charAt(i)){
					case 'i': flag |= Pattern.CASE_INSENSITIVE; break;
					case 's': flag |= Pattern.DOTALL; break;
					case 'm': flag |= Pattern.MULTILINE; break;
					case 'u': flag |= Pattern.UNICODE_CASE; break;
					case 'c': flag |= Pattern.CANON_EQ; break;
					case 'n': expr = escapePattern(expr); break;
					case 'g': break;
					default:
						throw new PnutsException("pnuts.regex.errors",
												 "illegal.regex.option",
												 new Object[]{option},
												 context);
					}
				}
			}

			try {
				p = pattern = Pattern.compile(expr, flag);
				lastExpr = expr;
				lastOption = option;
			} catch (PatternSyntaxException e){
				throw new PnutsException("pnuts.regex.errors",
										 "illegal.regex.pattern",
										 new Object[]{expr},
										 context);
			}
		}
		return p;
	}

	static Object split(Pattern pattern, CharSequence input){
		Matcher m = pattern.matcher(input);
		ArrayList list = new ArrayList();
		IntList start = new IntList();
		IntList end = new IntList();
		int off = 0;

		while(m.find()) {
			start.add(off);
			end.add(m.start());
			off = m.end();
		}
		start.add(off);
		end.add(input.length());
		int size = start.size();
		int s[] = new int[size];
		int e[] = new int[size];
		start.copyInto(s);
		end.copyInto(e);
		return new CharSequenceList(input, s, e);
	}

	static boolean match(Pattern pattern, CharSequence input, MatchContext mc){
		Matcher m = pattern.matcher(input);
		boolean result = m.find();
		mc.matcher = m;
		mc.count = (result ? 1 : 0);
		return result;
	}

	static String getMatch(int idx, MatchContext mc){
		Matcher matcher = mc.matcher;
		if (matcher == null){
			return null;
		} else {
			try {
				return matcher.group(idx);
			} catch (Exception e1){
				return null;
			}
		}
	}

	static int getMatchStart(int idx, MatchContext mc){
		Matcher matcher = mc.matcher;
		if (matcher == null){
			return -1;
		} else {
			try {
				return matcher.start(idx);
			} catch (Exception e1){
				return -1;
			}
		}
	}

	static int getMatchEnd(int idx, MatchContext mc){
		Matcher matcher = mc.matcher;
		if (matcher == null){
			return -1;
		} else {
			try {
				return matcher.end(idx);
			} catch (Exception e1){
				return -1;
			}
		}
	}

	static int getNumberOfGroups(MatchContext mc){
		Matcher matcher = mc.matcher;
		if (matcher != null){
			return matcher.groupCount();
		} else {
			return -1;
		}
	}

	static int getMatchCount(MatchContext mc){
		return mc.count;
	}

	static Object matchAll(Pattern pattern,
						   CharSequence input,
						   final Context context)
		{
			Matcher matcher = pattern.matcher(input);
			boolean result = matcher.find();
			int matchCount = 0;
			while (result){
				matchCount++;
				result = matcher.find();
			}
			return new Integer(matchCount);
		}

	static Object matchAll(Pattern pattern,
						   CharSequence input,
						   PnutsFunction func,
						   final Context context)
		{
	
			final Matcher matcher = pattern.matcher(input);

			if (func != null && !func.defined(1) && !func.defined(-1)){
				throw new IllegalArgumentException();
			}
			if (func != null){
				boolean result = matcher.find();
				if (result){
					int matchCount = 0;
					MatchContext mc = new MatchContext();
					mc.matcher = matcher;
					while (result){
						matchCount++;
						Context ctx = (Context)context.clone();
						ctx.set(CONTEXT_SYMBOL, mc);
						func.call(new Object[]{input.subSequence(matcher.start(), matcher.end())}, ctx);
						result = matcher.find();
					}
					return new Integer(matchCount);
				}
			} else {
				return new MatchIterator(matcher, input);
			}
			return null;
		}

	static class MatchIterator implements Iterator {
		boolean checkNeeded = true;
		Matcher matcher;
		CharSequence input;
		boolean result;

		MatchIterator(Matcher matcher, CharSequence input){
			this.matcher = matcher;
			this.input = input;
		}

		synchronized void check(){
			if (checkNeeded){
				result = matcher.find();
				checkNeeded = false;
			}
		}

		public boolean hasNext(){
			check();
			return result;
		}

		public Object next(){
			check();
			if (!result){
				throw new NoSuchElementException();
			}
			checkNeeded = true;
			return input.subSequence(matcher.start(), matcher.end());
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	}

	static Object substitute(Pattern pattern,
							 Object replacement,
							 CharSequence input,
							 boolean g_option,
							 boolean n_option,
							 MatchContext mc,
							 Context context)
		{
			Matcher m = pattern.matcher(input);
			mc.matcher = m;
			int len = input.length();
			Object ret;
			int matchCount = 0;

			if (replacement instanceof CharSequence){
				if (n_option){
					boolean result = m.find();
					if (result){
						int start;
						int idx = 0;
						StringBuffer sbuf = new StringBuffer(len);
						if (g_option){
							while (result) {
								start = m.start();
								sbuf.append(input.subSequence(idx, start));
								sbuf.append(replacement);
								idx = m.end();
								if (idx == len) break;
								result = m.find();
							}
							sbuf.append(input.subSequence(idx, input.length()));
						} else {
							start = m.start();
							sbuf.append(input.subSequence(0, start));
							sbuf.append(replacement);
							idx = m.end();
							sbuf.append(input.subSequence(idx, input.length()));
						}
						ret = sbuf.toString();
					} else {
						ret = input;
					}
				} else {
					StringBuffer sb = new StringBuffer(len);
					m.reset();
					String str = replacement.toString();
					boolean result = m.find();
					if (result){
						if (g_option){
							while (result) {
								matchCount++;
								m.appendReplacement(sb, str);
								result = m.find();
							}
						} else {
							matchCount++;
							m.appendReplacement(sb, str);
						}
						m.appendTail(sb);
						ret = sb.toString();
					} else {
						ret = input;
					}
				}
			} else if (replacement instanceof PnutsFunction){
				PnutsFunction func = (PnutsFunction)replacement;
				boolean hasArg = false;
				if (func.defined(1)){
					hasArg = true;
				} else if (!func.defined(0) && !func.defined(-1)){
					throw new IllegalArgumentException();
				}
				StringBuffer sbuf = new StringBuffer(len);
				boolean result = m.find();
				int idx = 0;
				if (result){
					int start;
					while (result){
						matchCount++;
						start = m.start();
						sbuf.append(input.subSequence(idx, start));
			
						Context ctx = (Context)context.clone();
						mc.matcher = m;
						ctx.set(CONTEXT_SYMBOL, mc);
						Object v;
						idx = m.end();
						if (hasArg){
							v = func.call(new Object[]{input.subSequence(start, idx)}, ctx);
						} else {
							v = func.call(NO_PARAM, ctx);
							
						}
						if (v instanceof String){
							sbuf.append((String)v);
						}
						if (idx == len) break;
						if (!g_option) break;
						result = m.find();
					}
				}
				sbuf.append(input.subSequence(idx, input.length()));
				ret = sbuf.toString();
			} else {
				throw new IllegalArgumentException();
			}
			mc.count = matchCount;
			return ret;
		}


	/**
	 * split(pattern, input {, options })
	 */
	class split extends PnutsFunction {
		public split(){
			super("split");
		}

		public boolean defined(int nargs){
			return (nargs == 2 || nargs == 3);
		}

		protected Object exec(Object[] args, Context context){
			String option;
			int nargs = args.length;
			if (nargs == 3){
				option = (String)args[2];
			} else if (nargs == 2){
				option = null;
			} else {
				undefined(args, context);
				return null;
			}
			Pattern p;
			Object arg0 = args[0];
			if (arg0 instanceof Pattern){
				p = (Pattern)arg0;
			} else if (arg0 instanceof String){
				p = getPattern((String)arg0, option, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			CharSequence input = (CharSequence)args[1];
			return split(p, input);
		}

		public String toString(){
			return "function split(pattern, CharSequence {, options })";
		}
	}
	
	/**
	 * match(pattern, input {, options })
	 */
	class match extends PnutsFunction {

		public match(){
			super("match");
		}

		public boolean defined(int narg){
			return (narg == 2 || narg == 3);
		}

		protected Object exec(Object[] args, Context context){
			int nargs = args.length;
			String option;
			if (nargs == 3){
				option = (String)args[2];
			} else if (nargs == 2){
				option = null;
			} else {
				undefined(args, context);
				return null;
			}
			Pattern p;
			Object arg0 = args[0];
			if (arg0 instanceof Pattern){
				p = (Pattern)arg0;
			} else if (arg0 instanceof String){
				p = getPattern((String)arg0, option, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			CharSequence input = (CharSequence)args[1];
		
			MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
			if (mc == null){
				context.set(CONTEXT_SYMBOL, mc = new MatchContext());
			}
			return new Boolean(match(p, input, mc));
		}

		public String toString(){
			return "function match(pattern, CharSequence input {, options } )";
		}
	}

	static MatchContext mc = new MatchContext();

	/**
	 * substitute(pattern, replacement, input {, options })
	 */
	class substitute extends PnutsFunction {
		public substitute(){
			super("substitute");
		}

		public boolean defined(int narg){
			return (narg == 3 || narg == 4);
		}

		protected Object exec(Object[] args, Context context){
			String option;
			int nargs = args.length;
			if (nargs == 4){
				option = (String)args[3];
			} else if (nargs == 3){
				option = null;
			} else {
				undefined(args, context);
				return null;
			}
			Pattern p;
			Object arg0 = args[0];
			if (arg0 instanceof Pattern){
				p = (Pattern)arg0;
			} else if (arg0 instanceof String){
				p = getPattern((String)arg0, option, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			boolean g_option = (option != null && (option.indexOf('g') >= 0));
			boolean n_option = (option != null && (option.indexOf('n') >= 0));
		
			Object replacement = args[1];
			CharSequence input = (CharSequence)args[2];

			MatchContext mc = new MatchContext();
			context.set(CONTEXT_SYMBOL, mc);
			return substitute(p, replacement, input, g_option, n_option, mc, context);
		}

		public String toString(){
			return "function substitute(pattern, (func()|CharSequence) replacement, CharSequence input {, options } )";
		}
	}

	/**
	 * matchAll(pattern, input {, func(word) {, option }} )
	 */
	class matchAll extends PnutsFunction {
		public matchAll(){
			super("matchAll");
		}

		public boolean defined(int nargs){
			return nargs == 2 || nargs == 3 || nargs == 4;
		}

		protected Object exec(Object[] args, Context context){
			String option;
			PnutsFunction func;
			boolean nullfunc = false;
			int nargs = args.length;
			if (nargs == 4){
				func = (PnutsFunction)args[2];
				if (func == null){
					nullfunc = true;
				}
				option = (String)args[3];
			} else if (nargs == 3){
				func = (PnutsFunction)args[2];
				if (func == null){
					nullfunc = true;
				}
				option = null;
			} else if (nargs == 2){
				func = null;
				option = null;
			} else {
				undefined(args, context);
				return null;
			}
			Object arg0 = args[0];
			Pattern p;
			if (arg0 instanceof Pattern){
				p = (Pattern)arg0;
			} else if (arg0 instanceof String){
				p = getPattern((String)arg0, option, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			if (nullfunc){
				return matchAll(p, (CharSequence)args[1], context);
			} else {
				return matchAll(p, (CharSequence)args[1], func, context);
			}
		}

		public String toString(){
			return "function matchAll(pattern, CharSequence {, func() {, options }} )";
		}
	}

	class formatMatch extends PnutsFunction {
		public formatMatch(){
			super("formatMatch");
		}

		public boolean defined(int nargs){
			return nargs == 1;
		}

		protected Object exec(Object[] args, Context context){
			if (args.length != 1){
				undefined(args, context);
				return null;
			}
			MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
			if (mc == null){
				return null;
			}
			Matcher matcher = mc.matcher;
			if (matcher == null){
				return null;
			}
			StringBuffer result = new StringBuffer();
			int cursor = 0;
			String replacement = args[0].toString();

			while (cursor < replacement.length()) {
				char nextChar = replacement.charAt(cursor);
				if (nextChar == '\\') {
					cursor++;
					nextChar = replacement.charAt(cursor);
					result.append(nextChar);
					cursor++;
				} else if (nextChar == '$') {
					// Skip past $
					cursor++;

					// The first number is always a group
					int refNum = (int)replacement.charAt(cursor) - '0';
					if ((refNum < 0)||(refNum > 9))
						throw new IllegalArgumentException("Illegal group reference");
					cursor++;
			
					// Capture the largest legal group string
					boolean done = false;
					while (!done) {
						if (cursor >= replacement.length()) {
							break;
						}
						int nextDigit = replacement.charAt(cursor) - '0';
						if ((nextDigit < 0)||(nextDigit > 9)) { // not a number
							break;
						}
						int newRefNum = (refNum * 10) + nextDigit;
						if (matcher.groupCount() < newRefNum) {
							done = true;
						} else {
							refNum = newRefNum;
							cursor++;
						}
					}
			
					// Append group
					if (matcher.group(refNum) != null)
						result.append(matcher.group(refNum));
				} else {
					result.append(nextChar);
					cursor++;
				}
			}
			return result.toString();
		}

		public String toString(){
			return "function formatMatch(String template)";
		}
	}

	/**
	 * regex(pattern{, option})
	 */
	class regex extends PnutsFunction {

		public regex(){
			super("regex");
		}

		public boolean defined(int nargs){
			return nargs == 1 || nargs == 2;
		}

		protected Object exec(Object[] args, Context context){
			String option = null;
			int nargs = args.length;
			if (nargs == 2){
				option = (String)args[1];
			} else if (nargs != 1){
				undefined(args, context);
				return null;
			}
			Object expr = args[0];
			if (expr instanceof Pattern){
				return expr;
			} else if (expr instanceof String){
				return getPattern((String)expr, option, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(expr));
			}
		}

		public String toString(){
			return "function regex(String pattern {, option })";
		}
	}

	/**
	 * getMatch(index)
	 */
	class getMatch extends PnutsFunction {

		public getMatch(){
			super("getMatch");
		}

		public boolean defined(int narg){
			return (narg == 1);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 1){
				MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
				if (mc == null){
					return null;
				}
				int idx = ((Integer)args[0]).intValue();
				return getMatch(idx, mc);
			} else {
				undefined(args, context);
				return null;
			}
		}

		public String toString(){
			return "function getMatch(int n)";
		}
	}

	/**
	 * getMatches()
	 */
	class getMatches extends PnutsFunction {

		public getMatches(){
			super("getMatches");
		}

		public boolean defined(int narg){
			return (narg == 0);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 0){
				MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
				if (mc == null){
					return null;
				}
				Matcher matcher = mc.matcher;
				if (matcher != null){
					int c = matcher.groupCount();
					Object[] array = new Object[c];
					for (int i = 0; i < c; i++){
						array[i] = getMatch(i + 1, mc);
					}
					return array;
				} else {
					return null;
				}
			} else {
				undefined(args, context);
				return null;
			}
		}

		public String toString(){
			return "function getMatches()";
		}
	}


	/**
	 * getMatchStart(index)
	 */
	class getMatchStart extends PnutsFunction {

		public getMatchStart(){
			super("getMatchStart");
		}

		public boolean defined(int narg){
			return (narg == 1);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 1){
				MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
				if (mc == null){
					return null;
				}
				int idx = ((Integer)args[0]).intValue();
				int start = getMatchStart(idx, mc);
				if (start >= 0){
					return new Integer(start);
				} else {
					return null;
				}
			} else {
				undefined(args, context);
				return null;
			}
		} 

		public String toString(){
			return "function getMatchStart(int n)";
		}
	}

	/**
	 * getMatchEnd(index)
	 */
	class getMatchEnd extends PnutsFunction {

		public getMatchEnd(){
			super("getMatchEnd");
		}

		public boolean defined(int narg){
			return (narg == 1);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 1){
				MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
				if (mc == null){
					return null;
				}
				int idx = ((Integer)args[0]).intValue();
				int end = getMatchEnd(idx, mc);
				if (end >= 0){
					return new Integer(end);
				} else {
					return null;
				}
			} else {
				undefined(args, context);
				return null;
			}
		} 

		public String toString(){
			return "function getMatchEnd(int n)";
		}
	}

	/**
	 * getNumberOfGroups()
	 */
	class getNumberOfGroups extends PnutsFunction {
		public getNumberOfGroups(){
			super("getNumberOfGroups");
		}

		public boolean defined(int narg){
			return (narg == 0);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 0){
				MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
				if (mc == null){
					return null;
				}
				int num = getNumberOfGroups(mc);
				if (num < 0){
					return null;
				} else {
					return new Integer(num);
				}
			} else {
				undefined(args, context);
				return null;
			}
		}

		public String toString(){
			return "function getNumberOfGroups()";
		}
	}

	/**
	 * getMatchCount()
	 */
	class getMatchCount extends PnutsFunction {

		public getMatchCount(){
			super("getMatchCount");
		}

		public boolean defined(int narg){
			return (narg == 0);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length != 0){
				undefined(args, context);
				return null;
			}
			MatchContext mc = (MatchContext)context.get(CONTEXT_SYMBOL);
			if (mc != null){
				return new Integer(getMatchCount(mc));
			} else {
				return new Integer(0);
			}
		}

		public String toString(){
			return "function getMatchCount()";
		}
	}

	/**
	 * whichRegexAPI()
	 */
	class whichRegexAPI extends PnutsFunction {

		public whichRegexAPI(){
			super("whichRegexAPI");
		}

		public boolean defined(int narg){
			return (narg == 0);
		}

		protected Object exec(Object[] args, Context context){
			if (args.length == 0){
				return CONTEXT_SYMBOL;
			} else {
				undefined(args, context);
				return null;
			}
		}

		public String toString(){
			return "function whichRegexAPI()";
		}
	}

	public void registerAll(Package pkg, Context context){
		pkg.set("match".intern(), new match(), context);
		pkg.set("matchAll".intern(), new matchAll(), context);
		pkg.set("split".intern(), new split(), context);
		pkg.set("substitute".intern(), new substitute(), context);
		pkg.set("formatMatch".intern(), new formatMatch(), context);
		pkg.set("regex".intern(), new regex(), context);
		pkg.set("getMatch".intern(), new getMatch(), context);
		pkg.set("getMatches".intern(), new getMatches(), context);
		pkg.set("getMatchStart".intern(), new getMatchStart(), context);
		pkg.set("getMatchEnd".intern(), new getMatchEnd(), context);
		pkg.set("getNumberOfGroups".intern(), new getNumberOfGroups(), context);
		pkg.set("getMatchCount".intern(), new getMatchCount(), context);
		pkg.set("whichRegexAPI".intern(), new whichRegexAPI(), context);
	}
}
