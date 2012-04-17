vpath %.c $(PNUTS_NATIVE_SRCPATH)

%:: SCCS/s.%

%.o: %.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -c -o $@ $<

default.target: $(PNUTS_TARGET)

clean:
	@$(RM) -rf $(PNUTS_CLEAN_TARGET)

pnuts: $(PNUTS_OBJFILES)
	$(LD) $(LDFLAGS) $(PNUTS_OBJFILES) -o $@ $(PNUTS_LIBS)

pnutsc: $(PNUTSC_OBJFILES)
	$(LD) -o $@ $(PNUTSC_OBJFILES) $(PNUTSC_LIBS)

pnuts.o: pnuts.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -DPNUTS_MAIN_CLASS=\"pnuts.tools.Main\" -c -o $@ $<

pnutsc.o: pnuts.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -DPNUTS_MAIN_CLASS=\"pnuts.tools.PnutsCompiler\" -c -o $@ $<
