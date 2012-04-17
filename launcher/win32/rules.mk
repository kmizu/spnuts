vpath %.c $(PNUTS_NATIVE_SRCPATH)

%.obj: %.c
	$(CC) $(CFLAGS) -c $(CC_OUT)$@ $<

default.target: $(PNUTS_TARGET)

clean:
	@$(RM) -rf $(PNUTS_CLEAN_TARGET)

pnuts.exe: $(PNUTS_OBJFILES)
	echo linking...
	$(LD) $(LDFLAGS) $(PNUTS_OBJFILES) $(LD_OUT)$@ $(PNUTS_LIBS)

pnutsw.exe: $(PNUTSW_OBJFILES)
	$(LD) $(LDFLAGS) $(PNUTSW_OBJFILES) $(LD_OUT)$@ $(PNUTS_LIBS)

pnuts.obj: $(PNUTS_CFILES)
	$(CC) $(CFLAGS) -DPNUTS_MAIN_CLASS=\"pnuts.tools.Main\" -c $(CC_OUT)$@ $<

pnutsw.obj: pnuts_md.c
	$(CC) $(CFLAGS) -DWIN_MAIN -DPNUTS_MAIN_CLASS=\"pnuts.tools.Main\" -c $(CC_OUT)$@ $<

pnutsc.exe: $(PNUTSC_OBJFILES)
	$(LD) $(LDFLAGS) $(LD_OUT)$@ $(PNUTSC_OBJFILES) $(PNUTSC_LIBS)

pnutsc.obj: pnuts.c
	$(CC) $(CFLAGS) -DPNUTS_MAIN_CLASS=\"pnuts.tools.PnutsCompiler\" -c $(CC_OUT)$@ $<

install.registry:
	$(REGEDIT) -s pnuts.REG
